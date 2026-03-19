package com.kyvislabs.esphome.gateway.nativeapi;

import com.inductiveautomation.ignition.gateway.opcua.server.api.Device;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceContext;
import com.kyvislabs.esphome.gateway.types.*;
import com.kyvislabs.esphome.gateway.types.Number;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NativeApiDevice extends ManagedAddressSpaceWithLifecycle implements Device, NativeApiConnectionListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SubscriptionModel subscriptionModel;
    private final DeviceContext context;
    private final NativeApiDeviceConfig config;

    private NativeApiConnection connection;
    String connectionStatus = "Not Connected";
    int connectionCount = 0;

    ConcurrentHashMap<String, UaVariableNode> nodeList = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, UaFolderNode> domainFolders = new ConcurrentHashMap<>();
    Set<String> entityFolders = ConcurrentHashMap.newKeySet();

    // Maps native API key -> entity ID for state updates
    ConcurrentHashMap<Integer, String> keyToEntityId = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, String> keyToDomain = new ConcurrentHashMap<>();

    UaFolderNode stateFolder;
    UaFolderNode metaFolder;
    UaFolderNode diagnosticsFolder;

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

        connection = new NativeApiConnection(config.general().host(), config.general().port(), this);
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

    private void addDiagnosticTags() {
        var node = UaVariableNode.build(getNodeContext(), b ->
                b.setNodeId(context.nodeId("diagnostics-connected"))
                        .setBrowseName(context.qualifiedName("Connected"))
                        .setDisplayName(new LocalizedText("Connected"))
                        .setDataType(OpcUaDataType.fromBackingClass(Boolean.class).getNodeId())
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .setAccessLevel(AccessLevel.READ_ONLY)
                        .setUserAccessLevel(AccessLevel.READ_ONLY)
                        .setValue(new DataValue(new Variant("Connected".equals(connectionStatus))))
                        .build()
        );
        getNodeManager().addNode(node);
        diagnosticsFolder.addOrganizes(node);
        nodeList.put("diagnostics-connected", node);

        node = UaVariableNode.build(getNodeContext(), b ->
                b.setNodeId(context.nodeId("diagnostics-state"))
                        .setBrowseName(context.qualifiedName("State"))
                        .setDisplayName(new LocalizedText("State"))
                        .setDataType(OpcUaDataType.fromBackingClass(String.class).getNodeId())
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .setAccessLevel(AccessLevel.READ_ONLY)
                        .setUserAccessLevel(AccessLevel.READ_ONLY)
                        .setValue(new DataValue(new Variant(connectionStatus)))
                        .build()
        );
        getNodeManager().addNode(node);
        diagnosticsFolder.addOrganizes(node);
        nodeList.put("diagnostics-state", node);

        node = UaVariableNode.build(getNodeContext(), b ->
                b.setNodeId(context.nodeId("diagnostics-status"))
                        .setBrowseName(context.qualifiedName("Status"))
                        .setDisplayName(new LocalizedText("Status"))
                        .setDataType(OpcUaDataType.fromBackingClass(String.class).getNodeId())
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .setAccessLevel(AccessLevel.READ_ONLY)
                        .setUserAccessLevel(AccessLevel.READ_ONLY)
                        .setValue(new DataValue(new Variant(connectionStatus)))
                        .build()
        );
        getNodeManager().addNode(node);
        diagnosticsFolder.addOrganizes(node);
        nodeList.put("diagnostics-status", node);

        node = UaVariableNode.build(getNodeContext(), b ->
                b.setNodeId(context.nodeId("diagnostics-host"))
                        .setBrowseName(context.qualifiedName("Host"))
                        .setDisplayName(new LocalizedText("Host"))
                        .setDataType(OpcUaDataType.fromBackingClass(String.class).getNodeId())
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .setAccessLevel(AccessLevel.READ_ONLY)
                        .setUserAccessLevel(AccessLevel.READ_ONLY)
                        .setValue(new DataValue(new Variant(config.general().host() + ":" + config.general().port())))
                        .build()
        );
        getNodeManager().addNode(node);
        diagnosticsFolder.addOrganizes(node);
        nodeList.put("diagnostics-host", node);

        node = UaVariableNode.build(getNodeContext(), b ->
                b.setNodeId(context.nodeId("diagnostics-connection-count"))
                        .setBrowseName(context.qualifiedName("Connection Count"))
                        .setDisplayName(new LocalizedText("Connection Count"))
                        .setDataType(OpcUaDataType.fromBackingClass(Integer.class).getNodeId())
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .setAccessLevel(AccessLevel.READ_ONLY)
                        .setUserAccessLevel(AccessLevel.READ_ONLY)
                        .setValue(new DataValue(new Variant(connectionCount)))
                        .build()
        );
        getNodeManager().addNode(node);
        diagnosticsFolder.addOrganizes(node);
        nodeList.put("diagnostics-connection-count", node);
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
        nodeList.get("diagnostics-connected").setValue(new DataValue(new Variant(false)));
        nodeList.get("diagnostics-status").setValue(new DataValue(new Variant("Disconnected")));
        nodeList.get("diagnostics-state").setValue(new DataValue(new Variant("Disconnected")));
    }

    @Override
    public void onEntityDiscovered(EntityInfo entity) {
        logger.debug("Discovered entity: {} (domain={}, key={})", entity.name(), entity.domain(), entity.key());
        keyToEntityId.put(entity.key(), entity.entityId());
        keyToDomain.put(entity.key(), entity.domain());

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
    public void onError(String message, Throwable cause) {
        logger.error("Native API error: {}", message, cause);
        connectionStatus = "Error: " + message;
        nodeList.get("diagnostics-status").setValue(new DataValue(new Variant(connectionStatus)));
        nodeList.get("diagnostics-state").setValue(new DataValue(new Variant(connectionStatus)));
    }

    // ---- Node creation helpers (same pattern as ESPHomeDevice) ----

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
        var node = UaVariableNode.build(getNodeContext(), b ->
                b.setNodeId(context.nodeId(nodeKey))
                        .setBrowseName(context.qualifiedName(propName))
                        .setDisplayName(new LocalizedText(propName))
                        .setDataType(OpcUaDataType.fromBackingClass(propValue.getClass()).getNodeId())
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .setAccessLevel(AccessLevel.READ_ONLY)
                        .setUserAccessLevel(AccessLevel.READ_ONLY)
                        .setValue(new DataValue(new Variant(propValue)))
                        .build()
        );
        getNodeManager().addNode(node);
        parentFolder.addOrganizes(node);
        nodeList.put(nodeKey, node);
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
