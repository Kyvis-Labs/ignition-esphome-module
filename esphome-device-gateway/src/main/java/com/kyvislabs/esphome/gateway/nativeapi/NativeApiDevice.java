package com.kyvislabs.esphome.gateway.nativeapi;

import com.inductiveautomation.ignition.gateway.opcua.server.api.Device;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceContext;
import com.inductiveautomation.ignition.gateway.secrets.Plaintext;
import com.inductiveautomation.ignition.gateway.secrets.Secret;
import com.inductiveautomation.ignition.gateway.secrets.SecretConfig;
import com.inductiveautomation.ignition.gateway.secrets.SecretException;
import com.kyvislabs.esphome.gateway.types.*;
import com.kyvislabs.esphome.gateway.types.Number;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.AddressSpace;
import org.eclipse.milo.opcua.sdk.server.ManagedAddressSpaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NativeApiDevice extends ManagedAddressSpaceWithLifecycle implements Device, NativeApiConnectionListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // Protocol enum ordinal maps (values match ESPHome Native API protobuf definitions)
    private static final Map<String, Integer> CLIMATE_MODES = Map.of(
            "OFF", 0, "HEAT_COOL", 1, "COOL", 2, "HEAT", 3,
            "FAN_ONLY", 4, "DRY", 5, "AUTO", 6
    );
    private static final Map<String, Integer> CLIMATE_FAN_MODES = Map.of(
            "ON", 0, "OFF", 1, "AUTO", 2, "LOW", 3, "MEDIUM", 4,
            "HIGH", 5, "MIDDLE", 6, "FOCUS", 7, "DIFFUSE", 8, "QUIET", 9
    );
    private static final Map<String, Integer> CLIMATE_SWING_MODES = Map.of(
            "OFF", 0, "BOTH", 1, "VERTICAL", 2, "HORIZONTAL", 3
    );
    private static final Map<String, Integer> FAN_DIRECTIONS = Map.of(
            "FORWARD", 0, "REVERSE", 1
    );
    private static final Map<String, Integer> LOCK_COMMANDS = Map.ofEntries(
            Map.entry("UNLOCK", 0), Map.entry("UNLOCKED", 0),
            Map.entry("LOCK", 1), Map.entry("LOCKED", 1),
            Map.entry("OPEN", 2)
    );

    private final SubscriptionModel subscriptionModel;
    private final DeviceContext context;
    private final NativeApiDeviceConfig config;

    private NativeApiConnection connection;
    private volatile String connectionStatus = "Not Connected";
    private volatile int connectionCount = 0;
    private volatile int disconnectCount = 0;
    private volatile int errorCount = 0;
    private volatile String lastError = "";

    private final ConcurrentHashMap<String, UaVariableNode> nodeList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UaFolderNode> domainFolders = new ConcurrentHashMap<>();
    private final Set<String> entityFolders = ConcurrentHashMap.newKeySet();

    // Maps native API key -> entity ID for state updates
    private final ConcurrentHashMap<Integer, String> keyToEntityId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> keyToDomain = new ConcurrentHashMap<>();

    // Maps entity ID -> native API key (reverse lookup for write commands)
    private final ConcurrentHashMap<String, Integer> entityIdToKey = new ConcurrentHashMap<>();

    private UaFolderNode stateFolder;
    private UaFolderNode metaFolder;
    private UaFolderNode diagnosticsFolder;

    public NativeApiDevice(DeviceContext context, NativeApiDeviceConfig config) {
        super(context.getServer());

        this.context = context;
        this.config = config;

        subscriptionModel = new SubscriptionModel(context.getServer(), this);

        getLifecycleManager().addLifecycle(subscriptionModel);
        getLifecycleManager().addStartupTask(this::onStartup);
        getLifecycleManager().addShutdownTask(this::onShutdown);
    }

    @Override
    public String getStatus() {
        return connectionStatus;
    }

    private void onStartup() {
        var rootNode = new UaFolderNode(
                getNodeContext(),
                context.nodeId(context.getName()),
                context.qualifiedName(String.format("[%s]", context.getName())),
                new LocalizedText(String.format("[%s]", context.getName()))
        );

        getNodeManager().addNode(rootNode);

        rootNode.addReference(new Reference(
                rootNode.getNodeId(),
                NodeIds.Organizes,
                context.getRootNodeId().expanded(),
                Reference.Direction.INVERSE
        ));

        stateFolder = new UaFolderNode(
                getNodeContext(),
                context.nodeId("state"),
                context.qualifiedName("state"),
                new LocalizedText("state")
        );
        getNodeManager().addNode(stateFolder);
        rootNode.addOrganizes(stateFolder);

        metaFolder = new UaFolderNode(
                getNodeContext(),
                context.nodeId("meta"),
                context.qualifiedName("meta"),
                new LocalizedText("meta")
        );
        getNodeManager().addNode(metaFolder);
        rootNode.addOrganizes(metaFolder);

        diagnosticsFolder = new UaFolderNode(
                getNodeContext(),
                context.nodeId("[Diagnostics]"),
                context.qualifiedName("[Diagnostics]"),
                new LocalizedText("[Diagnostics]")
        );
        getNodeManager().addNode(diagnosticsFolder);
        rootNode.addOrganizes(diagnosticsFolder);

        addDiagnosticTags();

        onDataItemsCreated(
                context.getSubscriptionModel()
                        .getDataItems(context.getName())
        );

        byte[] psk = resolveEncryptionKey();
        connection = new NativeApiConnection(config.general().host(), config.general().port(), psk, this);
        connection.connect();
    }

    private void onShutdown() {
        context.getSubscriptionModel()
                .getDataItems(context.getName())
                .forEach(item -> item.setQuality(new StatusCode(StatusCodes.Uncertain_LastUsableValue)));

        context.getGatewayContext()
                .getExecutionManager()
                .unRegister(NativeApiDeviceExtensionPoint.TYPE_ID, context.getName());

        if (connection != null) {
            connection.shutdown();
        }
    }

    private byte[] resolveEncryptionKey() {
        SecretConfig secretConfig = config.general().encryptionKey();
        if (secretConfig == null) {
            return null;
        }

        try {
            Secret<?> secret = Secret.create(context.getGatewayContext(), secretConfig);
            try (Plaintext plaintext = secret.getPlaintext()) {
                String base64Key = plaintext.getAsString();
                if (base64Key == null || base64Key.isEmpty()) {
                    return null;
                }
                byte[] decoded = Base64.getDecoder().decode(base64Key);
                if (decoded.length != 32) {
                    throw new IllegalArgumentException(
                        "Encryption key must decode to exactly 32 bytes (got " + decoded.length + ")");
                }
                return decoded;
            }
        } catch (SecretException e) {
            throw new RuntimeException("Failed to resolve encryption key secret", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid encryption key: " + e.getMessage(), e);
        }
    }

    private void addDiagnosticTags() {
        createDiagnosticNode("connected", "Connected", Boolean.class, AccessLevel.READ_ONLY, "Connected".equals(connectionStatus));
        createDiagnosticNode("state", "State", String.class, AccessLevel.READ_ONLY, connectionStatus);
        createDiagnosticNode("status", "Status", String.class, AccessLevel.READ_ONLY, connectionStatus);
        createDiagnosticNode("host", "Host", String.class, AccessLevel.READ_ONLY, config.general().host() + ":" + config.general().port());
        createDiagnosticNode("connection-count", "Connection Count", Integer.class, AccessLevel.READ_ONLY, connectionCount);
        createDiagnosticNode("received-messages", "Received Messages", Long.class, AccessLevel.READ_ONLY, 0L);
        createDiagnosticNode("last-message-size", "Last Message Size", Integer.class, AccessLevel.READ_ONLY, 0);
        createDiagnosticNode("sent-messages", "Sent Messages", Long.class, AccessLevel.READ_ONLY, 0L);
        createDiagnosticNode("received-bytes", "Received Bytes", Long.class, AccessLevel.READ_ONLY, 0L);
        createDiagnosticNode("sent-bytes", "Sent Bytes", Long.class, AccessLevel.READ_ONLY, 0L);
        createDiagnosticNode("reset-counters", "Reset Counters", Boolean.class, AccessLevel.READ_WRITE, false);
        createDiagnosticNode("entity-count", "Entity Count", Integer.class, AccessLevel.READ_ONLY, 0);
        createDiagnosticNode("last-error", "Last Error", String.class, AccessLevel.READ_ONLY, "");
        createDiagnosticNode("error-count", "Error Count", Integer.class, AccessLevel.READ_ONLY, 0);
        createDiagnosticNode("disconnect-count", "Disconnect Count", Integer.class, AccessLevel.READ_ONLY, 0);
    }

    private void createDiagnosticNode(String idSuffix, String displayName,
            Class<?> dataType, Set<AccessLevel> accessLevel, Object initialValue) {
        var node = UaVariableNode.build(getNodeContext(), b ->
                b.setNodeId(context.nodeId("diagnostics-" + idSuffix))
                        .setBrowseName(context.qualifiedName(displayName))
                        .setDisplayName(new LocalizedText(displayName))
                        .setDataType(OpcUaDataType.fromBackingClass(dataType).getNodeId())
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .setAccessLevel(accessLevel)
                        .setUserAccessLevel(accessLevel)
                        .setValue(new DataValue(new Variant(initialValue)))
                        .build()
        );
        getNodeManager().addNode(node);
        diagnosticsFolder.addOrganizes(node);
        nodeList.put("diagnostics-" + idSuffix, node);
    }

    // ---- NativeApiConnectionListener implementation ----

    @Override
    public void onConnected(HashMap<String, Object> deviceInfo) {
        logger.info("Native API connected to device");
        connectionStatus = "Connected";
        connectionCount++;
        nodeList.get("diagnostics-connected").setValue(new DataValue(new Variant(true)));
        nodeList.get("diagnostics-status").setValue(new DataValue(new Variant("Connected")));
        nodeList.get("diagnostics-state").setValue(new DataValue(new Variant("Connected")));
        nodeList.get("diagnostics-connection-count").setValue(new DataValue(new Variant(connectionCount)));

        // Populate meta folder with device info
        for (Map.Entry<String, Object> entry : deviceInfo.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                value = "";
            }
            var id = String.format("%s.%s", "meta", name);
            if (nodeList.containsKey(id)) {
                var metaNode = nodeList.get(id);
                metaNode.setValue(new DataValue(new Variant(value)));
                continue;
            }

            final Object finalValue = value;
            var metaNode = UaVariableNode.build(getNodeContext(), b ->
                    b.setNodeId(context.nodeId(id))
                            .setBrowseName(context.qualifiedName(name))
                            .setDisplayName(new LocalizedText(name))
                            .setDataType(OpcUaDataType.fromBackingClass(finalValue.getClass()).getNodeId())
                            .setTypeDefinition(NodeIds.BaseDataVariableType)
                            .setAccessLevel(AccessLevel.READ_ONLY)
                            .setUserAccessLevel(AccessLevel.READ_ONLY)
                            .setValue(new DataValue(new Variant(finalValue)))
                            .build()
            );

            getNodeManager().addNode(metaNode);
            metaFolder.addOrganizes(metaNode);
            nodeList.put(id, metaNode);
        }
    }

    @Override
    public void onDisconnected(String reason) {
        logger.info("Native API disconnected: {}", reason);
        connectionStatus = "Disconnected";
        disconnectCount++;
        nodeList.get("diagnostics-connected").setValue(new DataValue(new Variant(false)));
        nodeList.get("diagnostics-status").setValue(new DataValue(new Variant("Disconnected")));
        nodeList.get("diagnostics-state").setValue(new DataValue(new Variant("Disconnected")));
        nodeList.get("diagnostics-disconnect-count").setValue(new DataValue(new Variant(disconnectCount)));
    }

    @Override
    public void onEntityDiscovered(EntityInfo entity) {
        logger.debug("Discovered entity: {} (domain={}, key={})", entity.name(), entity.domain(), entity.key());
        keyToEntityId.put(entity.key(), entity.entityId());
        keyToDomain.put(entity.key(), entity.domain());
        entityIdToKey.put(entity.entityId(), entity.key());
        nodeList.get("diagnostics-entity-count").setValue(new DataValue(new Variant(keyToEntityId.size())));

        // Create the entity folder structure with initial values from metadata
        try {
            var metadata = entity.metadata();
            var entityObj = createEntity(entity.domain(), metadata);
            var properties = entityObj.getProperties();

            var domainFolder = getOrCreateDomainFolder(entity.domain());

            var entityFolder = new UaFolderNode(
                    getNodeContext(),
                    context.nodeId(entity.entityId()),
                    context.qualifiedName(entity.name()),
                    new LocalizedText(entity.name())
            );
            getNodeManager().addNode(entityFolder);
            domainFolder.addOrganizes(entityFolder);
            entityFolders.add(entity.entityId());

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                createPropertyNode(entity.entityId(), entry.getKey(), entry.getValue(), entityFolder);
            }
        } catch (Exception e) {
            logger.error("Failed to create entity nodes for: {}", entity.entityId(), e);
        }
    }

    @Override
    public void onEntitiesDiscoveryDone() {
        logger.info("Entity discovery complete");
    }

    @Override
    public void onStateUpdate(int key, String domain, HashMap<String, Object> stateData) {
        String entityId = keyToEntityId.get(key);
        if (entityId == null) {
            logger.debug("State update for unknown key: {}", key);
            return;
        }

        try {
            // Add the id to stateData for entity creation
            stateData.put("id", entityId);

            var entity = createEntity(domain, stateData);
            for (Map.Entry<String, Object> entry : entity.getProperties().entrySet()) {
                var nodeKey = entityId + "." + entry.getKey();
                var node = nodeList.get(nodeKey);
                if (node != null) {
                    node.setValue(new DataValue(new Variant(entry.getValue())));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process state update for entity: {}", entityId, e);
        }
    }

    @Override
    public void onMessageStats(long receivedCount, int lastReceivedSize, long sentCount, long receivedBytes, long sentBytes) {
        nodeList.get("diagnostics-received-messages").setValue(new DataValue(new Variant(receivedCount)));
        nodeList.get("diagnostics-last-message-size").setValue(new DataValue(new Variant(lastReceivedSize)));
        nodeList.get("diagnostics-sent-messages").setValue(new DataValue(new Variant(sentCount)));
        nodeList.get("diagnostics-received-bytes").setValue(new DataValue(new Variant(receivedBytes)));
        nodeList.get("diagnostics-sent-bytes").setValue(new DataValue(new Variant(sentBytes)));
    }

    @Override
    public void onError(String message, Throwable cause) {
        logger.error("Native API error: {}", message, cause);
        connectionStatus = "Error: " + message;
        errorCount++;
        lastError = message;
        nodeList.get("diagnostics-status").setValue(new DataValue(new Variant(connectionStatus)));
        nodeList.get("diagnostics-state").setValue(new DataValue(new Variant(connectionStatus)));
        nodeList.get("diagnostics-error-count").setValue(new DataValue(new Variant(errorCount)));
        nodeList.get("diagnostics-last-error").setValue(new DataValue(new Variant(message)));
    }

    // ---- Node creation helpers ----

    private UaFolderNode getOrCreateDomainFolder(String domain) {
        return domainFolders.computeIfAbsent(domain, d -> {
            var folder = new UaFolderNode(
                    getNodeContext(),
                    context.nodeId("state-" + d),
                    context.qualifiedName(d),
                    new LocalizedText(d)
            );
            getNodeManager().addNode(folder);
            stateFolder.addOrganizes(folder);
            return folder;
        });
    }

    private Base createEntity(String domain, HashMap<String, Object> payload) {
        return switch (domain) {
            case "binary_sensor" -> new BinarySensor(payload);
            case "text_sensor" -> new TextSensor(payload);
            case "sensor" -> new Sensor(payload);
            case "number" -> new Number(payload);
            case "switch" -> new Switch(payload);
            case "light" -> new Light(payload);
            case "button" -> new Button(payload);
            case "cover" -> new Cover(payload);
            case "fan" -> new Fan(payload);
            case "select" -> new Select(payload);
            case "climate" -> new Climate(payload);
            case "lock" -> new Lock(payload);
            default -> {
                logger.warn("Unknown domain: {}", domain);
                yield new Base(payload);
            }
        };
    }

    private void createPropertyNode(String entityId, String propName, Object propValue, UaFolderNode parentFolder) {
        var nodeKey = entityId + "." + propName;
        boolean writable = isWritableProperty(entityId, propName);
        var accessLevel = writable ? AccessLevel.READ_WRITE : AccessLevel.READ_ONLY;
        var node = UaVariableNode.build(getNodeContext(), b ->
                b.setNodeId(context.nodeId(nodeKey))
                        .setBrowseName(context.qualifiedName(propName))
                        .setDisplayName(new LocalizedText(propName))
                        .setDataType(OpcUaDataType.fromBackingClass(propValue.getClass()).getNodeId())
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .setAccessLevel(accessLevel)
                        .setUserAccessLevel(accessLevel)
                        .setValue(new DataValue(new Variant(propValue)))
                        .build()
        );
        getNodeManager().addNode(node);
        parentFolder.addOrganizes(node);
        nodeList.put(nodeKey, node);
    }

    private boolean isWritableProperty(String entityId, String propName) {
        Integer key = entityIdToKey.get(entityId);
        if (key == null) {
            return false;
        }
        String domain = keyToDomain.get(key);
        return switch (domain) {
            case "switch", "button", "number" -> "value".equals(propName);
            case "select", "lock" -> "state".equals(propName);
            case "cover" -> "position".equals(propName) || "tilt".equals(propName);
            case "fan" -> Set.of("value", "speed_level", "oscillating", "direction").contains(propName);
            case "light" -> Set.of("state", "brightness", "color", "effect").contains(propName);
            case "climate" -> Set.of("mode", "target_temperature", "fan_mode", "swing_mode").contains(propName);
            default -> false;
        };
    }

    // ---- Write handling ----

    @Override
    public List<StatusCode> write(AddressSpace.WriteContext context, List<WriteValue> writeValues) {
        List<StatusCode> results = new ArrayList<>(writeValues.size());

        for (WriteValue wv : writeValues) {
            String fullId = wv.getNodeId().getIdentifier().toString();
            String devicePrefix = "[" + this.context.getName() + "]";
            String nodeKeyStr = fullId.startsWith(devicePrefix)
                ? fullId.substring(devicePrefix.length())
                : fullId;
            var node = nodeList.get(nodeKeyStr);
            if (node == null) {
                results.add(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
                continue;
            }

            // Handle diagnostic reset counters tag
            if ("diagnostics-reset-counters".equals(nodeKeyStr)) {
                Object rawVal = wv.getValue().getValue().getValue();
                if (toBoolean(rawVal)) {
                    connection.resetMessageStats();
                    nodeList.get("diagnostics-received-messages").setValue(new DataValue(new Variant(0L)));
                    nodeList.get("diagnostics-last-message-size").setValue(new DataValue(new Variant(0)));
                    nodeList.get("diagnostics-sent-messages").setValue(new DataValue(new Variant(0L)));
                    nodeList.get("diagnostics-received-bytes").setValue(new DataValue(new Variant(0L)));
                    nodeList.get("diagnostics-sent-bytes").setValue(new DataValue(new Variant(0L)));
                    disconnectCount = 0;
                    errorCount = 0;
                    lastError = "";
                    nodeList.get("diagnostics-disconnect-count").setValue(new DataValue(new Variant(0)));
                    nodeList.get("diagnostics-error-count").setValue(new DataValue(new Variant(0)));
                    nodeList.get("diagnostics-last-error").setValue(new DataValue(new Variant("")));
                    node.setValue(new DataValue(new Variant(false)));
                }
                results.add(StatusCode.GOOD);
                continue;
            }

            // Parse entityId from nodeKey (format: "domain-objectId.propName")
            int lastDot = nodeKeyStr.lastIndexOf('.');
            if (lastDot < 0) {
                results.add(new StatusCode(StatusCodes.Bad_NotWritable));
                continue;
            }
            String entityId = nodeKeyStr.substring(0, lastDot);
            String propName = nodeKeyStr.substring(lastDot + 1);
            Integer nativeKey = entityIdToKey.get(entityId);
            String domain = nativeKey != null ? keyToDomain.get(nativeKey) : null;

            if (nativeKey == null || domain == null) {
                results.add(new StatusCode(StatusCodes.Bad_NotWritable));
                continue;
            }

            Object rawValue = wv.getValue().getValue().getValue();
            try {
                boolean handled = switch (domain) {
                    case "switch" -> writeSwitch(nativeKey, node, rawValue);
                    case "button" -> writeButton(nativeKey);
                    case "number" -> writeNumber(nativeKey, node, rawValue);
                    case "cover" -> writeCover(nativeKey, node, rawValue, propName);
                    case "fan" -> writeFan(nativeKey, node, rawValue, propName);
                    case "light" -> writeLight(nativeKey, node, rawValue, propName);
                    case "select" -> writeSelect(nativeKey, node, rawValue);
                    case "climate" -> writeClimate(nativeKey, node, rawValue, propName);
                    case "lock" -> writeLock(nativeKey, node, rawValue);
                    default -> false;
                };
                results.add(handled ? StatusCode.GOOD : new StatusCode(StatusCodes.Bad_NotWritable));
            } catch (Exception e) {
                logger.error("Failed to write to entity {}: {}", entityId, e.getMessage(), e);
                results.add(new StatusCode(StatusCodes.Bad_InternalError));
            }
        }

        return results;
    }

    private boolean writeSwitch(int key, UaVariableNode node, Object rawValue) throws IOException {
        boolean state = toBoolean(rawValue);
        connection.sendSwitchCommand(key, state);
        node.setValue(new DataValue(new Variant(state)));
        return true;
    }

    private boolean writeButton(int key) throws IOException {
        connection.sendButtonCommand(key);
        return true;
    }

    private boolean writeNumber(int key, UaVariableNode node, Object rawValue) throws IOException {
        float state = toFloat(rawValue);
        connection.sendNumberCommand(key, state);
        node.setValue(new DataValue(new Variant((double) state)));
        return true;
    }

    private boolean writeCover(int key, UaVariableNode node, Object rawValue, String propName) throws IOException {
        float val = toFloat(rawValue);
        switch (propName) {
            case "position" -> connection.sendCoverCommand(key, val, null, false);
            case "tilt" -> connection.sendCoverCommand(key, null, val, false);
            default -> { return false; }
        }
        node.setValue(new DataValue(new Variant((double) val)));
        return true;
    }

    private boolean writeFan(int key, UaVariableNode node, Object rawValue, String propName) throws IOException {
        switch (propName) {
            case "value" -> {
                boolean state = toBoolean(rawValue);
                connection.sendFanCommand(key, state, null, null, null);
                node.setValue(new DataValue(new Variant(state)));
            }
            case "speed_level" -> {
                int val = (rawValue instanceof java.lang.Number)
                        ? ((java.lang.Number) rawValue).intValue()
                        : Integer.parseInt(rawValue.toString());
                connection.sendFanCommand(key, null, val, null, null);
                node.setValue(new DataValue(new Variant(val)));
            }
            case "oscillating" -> {
                boolean val = toBoolean(rawValue);
                connection.sendFanCommand(key, null, null, val, null);
                node.setValue(new DataValue(new Variant(val)));
            }
            case "direction" -> {
                String dirStr = rawValue.toString();
                int dir = lookupEnum(FAN_DIRECTIONS, dirStr, "fan direction");
                connection.sendFanCommand(key, null, null, null, dir);
                node.setValue(new DataValue(new Variant(dirStr)));
            }
            default -> { return false; }
        }
        return true;
    }

    private boolean writeLight(int key, UaVariableNode node, Object rawValue, String propName) throws IOException {
        switch (propName) {
            case "state" -> {
                boolean val = toBoolean(rawValue);
                connection.sendLightCommand(key, val, null, null, null, null, null);
                node.setValue(new DataValue(new Variant(val)));
            }
            case "brightness" -> {
                float val = toFloat(rawValue);
                connection.sendLightCommand(key, null, val, null, null, null, null);
                node.setValue(new DataValue(new Variant((double) val)));
            }
            case "color" -> {
                String hexStr = rawValue.toString().trim();
                if (hexStr.startsWith("#")) {
                    hexStr = hexStr.substring(1);
                }
                float r = Integer.parseInt(hexStr.substring(0, 2), 16) / 255f;
                float g = Integer.parseInt(hexStr.substring(2, 4), 16) / 255f;
                float b = Integer.parseInt(hexStr.substring(4, 6), 16) / 255f;
                connection.sendLightCommand(key, null, null, r, g, b, null);
                node.setValue(new DataValue(new Variant("#" + hexStr.toUpperCase())));
            }
            case "effect" -> {
                String val = rawValue.toString();
                connection.sendLightCommand(key, null, null, null, null, null, val);
                node.setValue(new DataValue(new Variant(val)));
            }
            default -> { return false; }
        }
        return true;
    }

    private boolean writeSelect(int key, UaVariableNode node, Object rawValue) throws IOException {
        String val = rawValue.toString();
        connection.sendSelectCommand(key, val);
        node.setValue(new DataValue(new Variant(val)));
        return true;
    }

    private boolean writeClimate(int key, UaVariableNode node, Object rawValue, String propName) throws IOException {
        switch (propName) {
            case "mode" -> {
                String modeStr = rawValue.toString();
                int mode = lookupEnum(CLIMATE_MODES, modeStr, "climate mode");
                connection.sendClimateCommand(key, mode, null, null, null);
                node.setValue(new DataValue(new Variant(modeStr)));
            }
            case "target_temperature" -> {
                float val = toFloat(rawValue);
                connection.sendClimateCommand(key, null, val, null, null);
                node.setValue(new DataValue(new Variant((double) val)));
            }
            case "fan_mode" -> {
                String fmStr = rawValue.toString();
                int fm = lookupEnum(CLIMATE_FAN_MODES, fmStr, "climate fan mode");
                connection.sendClimateCommand(key, null, null, fm, null);
                node.setValue(new DataValue(new Variant(fmStr)));
            }
            case "swing_mode" -> {
                String smStr = rawValue.toString();
                int sm = lookupEnum(CLIMATE_SWING_MODES, smStr, "climate swing mode");
                connection.sendClimateCommand(key, null, null, null, sm);
                node.setValue(new DataValue(new Variant(smStr)));
            }
            default -> { return false; }
        }
        return true;
    }

    private boolean writeLock(int key, UaVariableNode node, Object rawValue) throws IOException {
        String lockStr = rawValue.toString();
        int cmd = lookupEnum(LOCK_COMMANDS, lockStr, "lock command");
        connection.sendLockCommand(key, cmd);
        node.setValue(new DataValue(new Variant(lockStr)));
        return true;
    }

    private static boolean toBoolean(Object raw) {
        return (raw instanceof Boolean) ? (Boolean) raw : Boolean.parseBoolean(raw.toString());
    }

    private static float toFloat(Object raw) {
        return (raw instanceof java.lang.Number) ? ((java.lang.Number) raw).floatValue()
                : Float.parseFloat(raw.toString());
    }

    private static int lookupEnum(Map<String, Integer> map, String value, String label) {
        Integer ordinal = map.get(value);
        if (ordinal == null) {
            throw new IllegalArgumentException("Unknown " + label + ": " + value);
        }
        return ordinal;
    }

    // ---- Subscription model delegation ----

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }
}
