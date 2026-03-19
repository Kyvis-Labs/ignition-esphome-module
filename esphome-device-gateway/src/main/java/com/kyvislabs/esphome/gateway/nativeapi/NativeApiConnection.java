package com.kyvislabs.esphome.gateway.nativeapi;

import com.kyvislabs.esphome.gateway.nativeapi.proto.EntityMessageParser;
import com.kyvislabs.esphome.gateway.nativeapi.proto.MessageFramer;
import com.kyvislabs.esphome.gateway.nativeapi.proto.MessageFramer.RawMessage;
import com.kyvislabs.esphome.gateway.nativeapi.proto.MessageTypes;
import com.kyvislabs.esphome.gateway.nativeapi.proto.ProtobufReader;
import com.kyvislabs.esphome.gateway.nativeapi.proto.ProtobufWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NativeApiConnection {

    private static final String CLIENT_INFO = "Ignition ESPHome Module";
    private static final int API_VERSION_MAJOR = 1;
    private static final int API_VERSION_MINOR = 10;
    private static final int HANDSHAKE_TIMEOUT_MS = 15_000;
    private static final long KEEPALIVE_INTERVAL_MS = 20_000;
    private static final long INITIAL_RECONNECT_DELAY_MS = 5_000;
    private static final long MAX_RECONNECT_DELAY_MS = 60_000;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String host;
    private final int port;
    private final NativeApiConnectionListener listener;
    private final ScheduledExecutorService scheduler;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread readerThread;
    private ScheduledFuture<?> keepaliveFuture;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private long currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;

    // Maps native API key -> entity info for state correlation
    private final HashMap<Integer, EntityInfo> entityByKey = new HashMap<>();

    public NativeApiConnection(String host, int port, NativeApiConnectionListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NativeApi-scheduler-" + host);
            t.setDaemon(true);
            return t;
        });
    }

    public void connect() {
        shutdownRequested.set(false);
        scheduler.execute(this::doConnect);
    }

    public void disconnect() {
        shutdownRequested.set(true);
        scheduler.execute(this::doDisconnect);
    }

    public void shutdown() {
        shutdownRequested.set(true);
        doDisconnect();
        scheduler.shutdownNow();
    }

    private void doConnect() {
        if (shutdownRequested.get()) {
            return;
        }

        try {
            logger.info("Connecting to {}:{}", host, port);

            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            // Perform hello handshake (also sends DeviceInfoRequest in same batch)
            performHandshake();

            // Handshake complete — switch to blocking reads for the reader thread
            socket.setSoTimeout(0);

            connected.set(true);
            currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;

            // Start reader thread (will process DeviceInfoResponse, then request entities)
            readerThread = new Thread(this::readerLoop, "NativeApi-reader-" + host);
            readerThread.setDaemon(true);
            readerThread.start();

            // Start keepalive
            keepaliveFuture = scheduler.scheduleAtFixedRate(
                this::sendPing,
                KEEPALIVE_INTERVAL_MS,
                KEEPALIVE_INTERVAL_MS,
                TimeUnit.MILLISECONDS
            );

        } catch (SocketTimeoutException e) {
            logger.error("Handshake timed out connecting to {}:{} (no response within {}ms)", host, port, HANDSHAKE_TIMEOUT_MS);
            listener.onError("Handshake timed out — device may not have the api: component enabled, " +
                "or may require encryption", e);
            scheduleReconnect();
        } catch (Exception e) {
            logger.error("Failed to connect to {}:{}", host, port, e);
            listener.onError("Connection failed: " + e.getMessage(), e);
            scheduleReconnect();
        }
    }

    private RawMessage readExpectedMessage(int expectedType) throws IOException {
        while (true) {
            RawMessage msg = MessageFramer.readFrame(inputStream);
            int type = msg.messageType();

            if (type == expectedType) {
                return msg;
            }

            // Handle protocol messages that can arrive at any time
            switch (type) {
                case MessageTypes.PING_REQUEST -> {
                    logger.debug("Received PingRequest during handshake, responding");
                    sendMessage(MessageTypes.PING_RESPONSE, new byte[0]);
                }
                case MessageTypes.PING_RESPONSE -> logger.debug("Received PingResponse during handshake, ignoring");
                case MessageTypes.DISCONNECT_REQUEST -> {
                    logger.debug("Received DisconnectRequest during handshake, responding");
                    sendMessage(MessageTypes.DISCONNECT_RESPONSE, new byte[0]);
                }
                case MessageTypes.DISCONNECT_RESPONSE ->
                    throw new IOException("Device disconnected during handshake");
                default ->
                    logger.debug("Ignoring unexpected message type {} while waiting for type {}", type, expectedType);
            }
        }
    }

    private void performHandshake() throws IOException {
        // Send HelloRequest + DeviceInfoRequest as a single TCP write.
        // ESPHome 2026.1.0+ removed password auth, so ConnectRequest is skipped.
        // Both frames must arrive in the same TCP segment for the device to process them.
        var writer = new ProtobufWriter();
        writer.writeStringField(1, CLIENT_INFO);
        writer.writeVarIntField(2, API_VERSION_MAJOR);
        writer.writeVarIntField(3, API_VERSION_MINOR);
        byte[] helloPayload = writer.toByteArray();
        logger.info("Sending HelloRequest + DeviceInfoRequest to {}:{}", host, port);

        byte[] helloFrame = MessageFramer.buildFrame(MessageTypes.HELLO_REQUEST, helloPayload);
        byte[] deviceInfoFrame = MessageFramer.buildFrame(MessageTypes.DEVICE_INFO_REQUEST, new byte[0]);
        byte[] batch = new byte[helloFrame.length + deviceInfoFrame.length];
        System.arraycopy(helloFrame, 0, batch, 0, helloFrame.length);
        System.arraycopy(deviceInfoFrame, 0, batch, helloFrame.length, deviceInfoFrame.length);
        synchronized (this) {
            outputStream.write(batch);
            outputStream.flush();
        }

        // Read HelloResponse
        RawMessage helloResponse = readExpectedMessage(MessageTypes.HELLO_RESPONSE);
        var reader = new ProtobufReader(helloResponse.payload());
        String serverInfo = "";
        String serverName = "";
        while (reader.hasRemaining()) {
            int tag = reader.readTag();
            int field = reader.getFieldNumber(tag);
            int wire = reader.getWireType(tag);
            switch (field) {
                case 1 -> { /* api_version_major */ reader.readVarInt(); }
                case 2 -> { /* api_version_minor */ reader.readVarInt(); }
                case 3 -> serverInfo = reader.readString();
                case 4 -> serverName = reader.readString();
                default -> reader.skipField(wire);
            }
        }
        logger.info("Connected to {} ({})", serverName, serverInfo);
    }

    private void readerLoop() {
        try {
            while (connected.get() && !shutdownRequested.get()) {
                RawMessage msg = MessageFramer.readFrame(inputStream);
                handleMessage(msg);
            }
        } catch (IOException e) {
            if (!shutdownRequested.get()) {
                logger.warn("Connection lost to {}:{}: {}", host, port, e.getMessage());
                connected.set(false);
                listener.onDisconnected("Connection lost: " + e.getMessage());
                scheduleReconnect();
            }
        } catch (Exception e) {
            if (!shutdownRequested.get()) {
                logger.error("Unexpected error in reader loop", e);
                connected.set(false);
                listener.onDisconnected("Error: " + e.getMessage());
                scheduleReconnect();
            }
        }
    }

    private void handleMessage(RawMessage msg) throws IOException {
        int type = msg.messageType();

        switch (type) {
            case MessageTypes.PING_REQUEST -> sendMessage(MessageTypes.PING_RESPONSE, new byte[0]);
            case MessageTypes.PING_RESPONSE -> { /* keepalive ack */ }
            case MessageTypes.DISCONNECT_REQUEST -> {
                sendMessage(MessageTypes.DISCONNECT_RESPONSE, new byte[0]);
                connected.set(false);
                listener.onDisconnected("Device requested disconnect");
                if (!shutdownRequested.get()) {
                    scheduleReconnect();
                }
            }
            case MessageTypes.DISCONNECT_RESPONSE -> {
                connected.set(false);
                listener.onDisconnected("Disconnected");
            }
            case MessageTypes.DEVICE_INFO_RESPONSE -> {
                var deviceInfo = EntityMessageParser.parseDeviceInfo(msg.payload());
                listener.onConnected(deviceInfo);
                // Now request entity list
                sendMessage(MessageTypes.LIST_ENTITIES_REQUEST, new byte[0]);
            }
            case MessageTypes.LIST_ENTITIES_DONE_RESPONSE -> {
                listener.onEntitiesDiscoveryDone();
                // Subscribe to state updates
                sendMessage(MessageTypes.SUBSCRIBE_STATES_REQUEST, new byte[0]);
            }
            default -> {
                if (MessageTypes.isListEntitiesResponse(type)) {
                    handleListEntitiesResponse(type, msg.payload());
                } else if (MessageTypes.isStateResponse(type)) {
                    handleStateResponse(type, msg.payload());
                } else {
                    logger.trace("Ignoring message type {}", type);
                }
            }
        }
    }

    private void handleListEntitiesResponse(int messageType, byte[] payload) {
        var parsed = EntityMessageParser.parseListEntities(messageType, payload);
        if (parsed == null) {
            return;
        }

        int key = (int) parsed.getOrDefault("key", 0);
        String entityId = (String) parsed.getOrDefault("id", "");
        String domain = (String) parsed.getOrDefault("domain", "");
        String name = (String) parsed.getOrDefault("name", "");

        var info = new EntityInfo(key, entityId, domain, name, parsed);
        entityByKey.put(key, info);

        listener.onEntityDiscovered(info);
    }

    private void handleStateResponse(int messageType, byte[] payload) {
        var parsed = EntityMessageParser.parseStateResponse(messageType, payload);
        if (parsed == null) {
            return;
        }

        int key = parsed.containsKey("key") ? ((Number) parsed.get("key")).intValue() : 0;
        String domain = (String) parsed.get("domain");

        // Merge metadata from entity discovery (icon, uom, etc.)
        EntityInfo info = entityByKey.get(key);
        if (info != null) {
            // Carry over static metadata fields to the state update
            var metadata = info.metadata();
            if (metadata.containsKey("icon")) {
                parsed.putIfAbsent("icon", metadata.get("icon"));
            }
            if (metadata.containsKey("uom")) {
                parsed.putIfAbsent("uom", metadata.get("uom"));
            }
            if (metadata.containsKey("min_value")) {
                parsed.putIfAbsent("min_value", metadata.get("min_value"));
            }
            if (metadata.containsKey("max_value")) {
                parsed.putIfAbsent("max_value", metadata.get("max_value"));
            }
            if (metadata.containsKey("step")) {
                parsed.putIfAbsent("step", metadata.get("step"));
            }
            if (metadata.containsKey("mode")) {
                parsed.putIfAbsent("mode", metadata.get("mode"));
            }
            if (metadata.containsKey("options")) {
                parsed.putIfAbsent("options", metadata.get("options"));
            }
            if (metadata.containsKey("assumed_state")) {
                parsed.putIfAbsent("assumed_state", metadata.get("assumed_state"));
            }
            // Carry over id and name from discovery
            parsed.put("id", info.entityId());
            parsed.put("name", info.name());
        }

        listener.onStateUpdate(key, domain, parsed);
    }

    private void sendMessage(int messageType, byte[] payload) throws IOException {
        synchronized (this) {
            if (outputStream != null) {
                MessageFramer.writeFrame(outputStream, messageType, payload);
            }
        }
    }

    private void sendPing() {
        try {
            sendMessage(MessageTypes.PING_REQUEST, new byte[0]);
        } catch (IOException e) {
            logger.warn("Failed to send ping to {}:{}", host, port, e);
        }
    }

    private void doDisconnect() {
        connected.set(false);

        if (keepaliveFuture != null) {
            keepaliveFuture.cancel(false);
            keepaliveFuture = null;
        }

        try {
            if (outputStream != null && socket != null && !socket.isClosed()) {
                sendMessage(MessageTypes.DISCONNECT_REQUEST, new byte[0]);
            }
        } catch (IOException ignored) {
        }

        closeSocket();

        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                // Send TCP RST instead of FIN so the device immediately frees
                // the connection slot. Without this, the device keeps the
                // half-open connection until its own keepalive timer expires,
                // which can fill all available connection slots after repeated retries.
                socket.setSoLinger(true, 0);
                socket.close();
            }
        } catch (IOException ignored) {
        }
        socket = null;
        inputStream = null;
        outputStream = null;
    }

    private void scheduleReconnect() {
        if (shutdownRequested.get()) {
            return;
        }

        // Clean up current connection
        if (keepaliveFuture != null) {
            keepaliveFuture.cancel(false);
            keepaliveFuture = null;
        }
        closeSocket();
        entityByKey.clear();

        logger.info("Scheduling reconnect to {}:{} in {}ms", host, port, currentReconnectDelay);

        scheduler.schedule(this::doConnect, currentReconnectDelay, TimeUnit.MILLISECONDS);

        // Exponential backoff
        currentReconnectDelay = Math.min(currentReconnectDelay * 2, MAX_RECONNECT_DELAY_MS);
    }
}
