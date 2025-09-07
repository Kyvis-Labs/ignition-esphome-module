package com.kyvislabs.esphome.gateway;

import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.common.gson.GsonBuilder;
import com.inductiveautomation.ignition.gateway.opcua.server.api.Device;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceContext;

import java.util.List;
import java.util.HashMap;

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
import org.python.modules.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kyvislabs.esphome.gateway.types.*;
import com.kyvislabs.esphome.gateway.types.Number;

public class ESPHomeDevice extends ManagedAddressSpaceWithLifecycle implements Device {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SubscriptionModel subscriptionModel;

    private final DeviceContext context;
    private final ESPHomeDeviceConfig config;
    OkHttpClient client;
    EventSource.Factory factory;
    String connectionStatus = "Not Connected";
    EventSource eventSource;
    HashMap<String, UaVariableNode> nodeList = new HashMap<String, UaVariableNode>();
    Gson gson = new GsonBuilder().create();
    UaFolderNode stateFolder;
    UaFolderNode metaFolder;

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
            }

            @Override
            public void onClosed(EventSource eventSource) {
                logger.info("SSE connection closed");
                connectionStatus = "Closed";
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                logger.error("SSE connection failed", t);
                connectionStatus = "Connection Failure";
                eventSource.cancel();
                Time.sleep(5.0);
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

    private void handlePingEvent(String json) {

        var payload = gson.fromJson(json, HashMap.class);

        for (Object key: payload.keySet()){
            var name = String.valueOf(key);
            var value = payload.get(key);
            var id = String.format("%s.%s","meta",name);
            if (nodeList.containsKey(id)) {
                var node = nodeList.get(id);
                node.setValue(new DataValue(new Variant(value)));
                return;
            }

            var node = UaVariableNode.build(getNodeContext(), b ->
                    b.setNodeId(context.nodeId(id))
                            .setBrowseName(context.qualifiedName(name))
                            .setDisplayName(new LocalizedText(name))
                            .setDataType(OpcUaDataType.fromBackingClass(value.getClass()).getNodeId())
                            .setTypeDefinition(NodeIds.BaseDataVariableType)
                            .setAccessLevel(AccessLevel.READ_ONLY)
                            .setUserAccessLevel(AccessLevel.READ_ONLY)
                            .setValue(new DataValue(new Variant(value)))
                            .build()
            );

            getNodeManager().addNode(node);
            metaFolder.addOrganizes(node);
            nodeList.put(id, node);
        }

    }

    private void handleStateEvent(String json) {

        var payload = gson.fromJson(json, HashMap.class);

        if (!payload.containsKey("id")) {
            logger.debug("No id defined in payload");
            return;
        }

        var id = String.valueOf(payload.get("id"));

        if (nodeList.containsKey(id)) {
            var node = nodeList.get(id);
            if (payload.containsKey("value")) {
                node.setValue(new DataValue(new Variant(payload.get("value"))));
            }
            return;
        }

        var type = id.split("-")[0];

        if (!payload.containsKey("name")) {
            logger.warn("No name defined in payload");
            return;
        }

        switch (type) {
            case "binary_sensor" -> logger.info(new BinarySensor(payload).toString());
            case "text_sensor" -> logger.info(new TextSensor(payload).toString());
            case "sensor" -> logger.info(new Sensor(payload).toString());
            case "number" -> logger.info(new Number(payload).toString());
            case "Button" -> logger.info(new Button(payload).toString());
            case "switch" -> logger.info(new Switch(payload).toString());
        }

        var name = String.valueOf(payload.get("name"));

        var value = payload.getOrDefault("value",false);

        var node = UaVariableNode.build(getNodeContext(), b ->
                b.setNodeId(context.nodeId(id))
                        .setBrowseName(context.qualifiedName(name))
                        .setDisplayName(new LocalizedText(name))
                        .setDataType(OpcUaDataType.fromBackingClass(value.getClass()).getNodeId())
                        .setTypeDefinition(NodeIds.BaseDataVariableType)
                        .setAccessLevel(AccessLevel.READ_ONLY)
                        .setUserAccessLevel(AccessLevel.READ_ONLY)
                        .setValue(new DataValue(new Variant(value)))
                        .build()
        );

        getNodeManager().addNode(node);
        stateFolder.addOrganizes(node);
        nodeList.put(id, node);

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
