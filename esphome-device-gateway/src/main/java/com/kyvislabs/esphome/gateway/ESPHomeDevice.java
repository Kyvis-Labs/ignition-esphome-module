package com.kyvislabs.esphome.gateway;

import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.GsonBuilder;
import com.inductiveautomation.ignition.gateway.opcua.server.api.Device;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
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
import com.kyvislabs.esphome.gateway.types.*;
import com.kyvislabs.esphome.gateway.types.Number;

import java.util.HashMap;

public class ESPHomeDevice extends ManagedAddressSpaceWithLifecycle implements Device {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SubscriptionModel subscriptionModel;

    private final DeviceContext context;
    private final ESPHomeDeviceConfig config;
    OkHttpClient client;
    EventSource.Factory factory;
    String connectionStatus = "Not Connected";
    int connectionCount = 0;
    EventSource eventSource;
    ConcurrentHashMap<String, UaVariableNode> nodeList = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, UaFolderNode> domainFolders = new ConcurrentHashMap<>();
    Set<String> entityFolders = ConcurrentHashMap.newKeySet();
    Gson gson = new GsonBuilder().create();
    UaFolderNode stateFolder;
    UaFolderNode metaFolder;
    UaFolderNode diagnosticsFolder;

    public ESPHomeDevice(DeviceContext context, ESPHomeDeviceConfig config) {
        super(context.getServer());

        this.context = context;
        this.config = config;

        subscriptionModel = new SubscriptionModel(context.getServer(), this);

        getLifecycleManager().addLifecycle(subscriptionModel);
        getLifecycleManager().addStartupTask(this::onStartup);
        getLifecycleManager().addShutdownTask(this::onShutdown);

        client = new OkHttpClient();
        factory = EventSources.createFactory(client);

    }

    @Override
    public String getStatus() {
        return connectionStatus;
    }

    private void onStartup() {
        // create a folder node for our configured device
        var rootNode = new UaFolderNode(
                getNodeContext(),
                context.nodeId(context.getName()),
                context.qualifiedName(String.format("[%s]", context.getName())),
                new LocalizedText(String.format("[%s]", context.getName()))
        );

        // add the folder node to the server
        getNodeManager().addNode(rootNode);

        // add a reference to the root "Devices" folder node
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
        // fire initial subscription creation
        onDataItemsCreated(
                context.getSubscriptionModel()
                        .getDataItems(context.getName())
        );

        Request request = new Request.Builder()
                .url(config.general().deviceUrl())
                .build();

        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                logger.info("SSE connection opened");
                connectionStatus = "Connected";
                connectionCount++;
                nodeList.get("diagnostics-connected").setValue(new DataValue(new Variant(true)));
                nodeList.get("diagnostics-status").setValue(new DataValue(new Variant("Connected")));
                nodeList.get("diagnostics-state").setValue(new DataValue(new Variant("Connected")));
                nodeList.get("diagnostics-connection-count").setValue(new DataValue(new Variant(connectionCount)));

            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                logger.debug(String.format("Received event: id=%s, type=%s, data=%s", id, type, data));

                if (type.equals("state")) {
                    handleStateEvent(data);
                    return;
                }

                if (type.equals("ping")) {
                    handlePingEvent(data);
                    return;
                }

                if (type.equals("log")) {
                    handleLogEvent(data);
                    return;
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
                logger.info("SSE connection closed");
                connectionStatus = "Closed";
                nodeList.get("diagnostics-connected").setValue(new DataValue(new Variant(false)));
                nodeList.get("diagnostics-status").setValue(new DataValue(new Variant("Disconnected")));
                nodeList.get("diagnostics-state").setValue(new DataValue(new Variant("Disconnected")));

            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                logger.error("SSE connection failed", t);
                connectionStatus = "Connection Failure";
                nodeList.get("diagnostics-connected").setValue(new DataValue(new Variant(false)));
                nodeList.get("diagnostics-status").setValue(new DataValue(new Variant("Connection Failure")));
                nodeList.get("diagnostics-state").setValue(new DataValue(new Variant("Connection Failure")));

                eventSource.cancel();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                factory.newEventSource(request, this);
            }
        };

        eventSource = factory.newEventSource(request, listener);
    }

    private void onShutdown() {
        context.getSubscriptionModel()
                .getDataItems(context.getName())
                .forEach(item -> item.setQuality(new StatusCode(StatusCodes.Uncertain_LastUsableValue)));

        context.getGatewayContext()
                .getExecutionManager()
                .unRegister(ESPHomeDeviceExtensionPoint.TYPE_ID, context.getName());
        eventSource.cancel();
        client.dispatcher().executorService().shutdown();
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
            b.setNodeId(context.nodeId("diagnostics-url"))
                    .setBrowseName(context.qualifiedName("URL"))
                    .setDisplayName(new LocalizedText("URL"))
                    .setDataType(OpcUaDataType.fromBackingClass(String.class).getNodeId())
                    .setTypeDefinition(NodeIds.BaseDataVariableType)
                    .setAccessLevel(AccessLevel.READ_ONLY)
                    .setUserAccessLevel(AccessLevel.READ_ONLY)
                    .setValue(new DataValue(new Variant(config.general().deviceUrl())))
                    .build()
         );
        getNodeManager().addNode(node);
        diagnosticsFolder.addOrganizes(node);
        nodeList.put("diagnostics-url", node);

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

    private void handleLogEvent(String json) {
        logger.info(json);
    }

    private void handlePingEvent(String json) {

        var payload = gson.fromJson(json, HashMap.class);

        if (payload == null) {
            logger.debug("Ping event had no parseable data");
            return;
        }

        for (Object key: payload.keySet()){
            var name = String.valueOf(key);
            var value = payload.get(key);
            if (value == null) {
                value = "";
            }
            var id = String.format("%s.%s","meta",name);
            if (nodeList.containsKey(id)) {
                var node = nodeList.get(id);
                node.setValue(new DataValue(new Variant(value)));
                continue;
            }

            final Object finalValue = value;
            var node = UaVariableNode.build(getNodeContext(), b ->
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

            getNodeManager().addNode(node);
            metaFolder.addOrganizes(node);
            nodeList.put(id, node);
        }

    }

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
        var entity = switch (domain) {
            case "binary_sensor" -> new BinarySensor(payload);
            case "text_sensor" -> new TextSensor(payload);
            case "sensor" -> new Sensor(payload);
            case "number" -> new Number(payload);
            case "switch" -> new Switch(payload);
            case "light" -> new Light(payload);
            case "button" -> new Button(payload);
            default -> {
                logger.warn("Unknown domain: {}", domain);
                yield new Base(payload);
            }
        };

        return entity;
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

    private void handleStateEvent(String json) {

        var payload = gson.fromJson(json, HashMap.class);

        if (payload == null) {
            logger.debug("State event had no parseable data");
            return;
        }

        if (!payload.containsKey("id")) {
            logger.debug("No id defined in payload");
            return;
        }

        var id = String.valueOf(payload.get("id"));
        var domain = payload.containsKey("domain") ? String.valueOf(payload.get("domain")) : id.split("-")[0];

        try {
            // Update existing entity properties
            if (entityFolders.contains(id)) {
                var entity = createEntity(domain, payload);
                for (Map.Entry<String, Object> entry : entity.getProperties().entrySet()) {
                    var nodeKey = id + "." + entry.getKey();
                    var node = nodeList.get(nodeKey);
                    if (node != null) {
                        node.setValue(new DataValue(new Variant(entry.getValue())));
                    }
                }
                return;
            }

            if (!payload.containsKey("name")) {
                logger.warn("No name defined in payload");
                return;
            }

            var name = String.valueOf(payload.get("name"));
            var entity = createEntity(domain, payload);
            var properties = entity.getProperties();

            // Create domain folder if needed
            var domainFolder = getOrCreateDomainFolder(domain);

            // Create entity folder
            var entityFolder = new UaFolderNode(
                    getNodeContext(),
                    context.nodeId(id),
                    context.qualifiedName(name),
                    new LocalizedText(name)
            );
            getNodeManager().addNode(entityFolder);
            domainFolder.addOrganizes(entityFolder);
            entityFolders.add(id);

            // Create a child node for each property
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                createPropertyNode(id, entry.getKey(), entry.getValue(), entityFolder);
            }
        } catch (Exception e) {
            logger.error("Failed to process state event for entity: {}", id, e);
        }
    }

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
