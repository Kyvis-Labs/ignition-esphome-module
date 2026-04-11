# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

```bash
mvn package                # Build everything; produces .modl at esphome-device-build/target/ESP-Home-device-driver-unsigned.modl
mvn test                   # Run all tests
mvn test -pl esphome-device-gateway                              # Tests in gateway module only
mvn test -pl esphome-device-gateway -Dtest=MessageFramerTest     # Single test class
mvn test -pl esphome-device-gateway -Dtest=MessageFramerTest#roundTrip_simplePayload  # Single test method
```

Java 17+, Ignition SDK 8.3.4, JUnit 5. Tests are in `esphome-device-gateway/src/test/`.

## Architecture

This is an Ignition module that provides an OPC UA device driver for ESPHome devices via the Native API protocol. The driver discovers entities from the device and exposes them as hierarchical OPC UA tags (`state/<domain>/<entity>/<property>`).

### Module entry point

`ModuleHook` extends `AbstractDeviceModuleHook` and registers the `NativeApiDeviceExtensionPoint`.

### Native API device (bidirectional, TCP)

`NativeApiDevice` → implements `NativeApiConnectionListener` to receive callbacks from `NativeApiConnection`. Supports writes to controllable entities via OPC UA `write()` override. Write dispatch is domain-based (switch, light, climate, etc.).

`NativeApiConnection` → manages the TCP socket lifecycle:
1. Optional Noise encryption handshake (if PSK configured) via `NoiseFrameHelper`
2. API handshake: sends `HelloRequest` + `DeviceInfoRequest`
3. Reader thread processes all incoming messages via `handleMessage()`
4. Message flow: DeviceInfoResponse → ListEntitiesRequest → entity responses → ListEntitiesDone → SubscribeStatesRequest → state updates
5. Keepalive pings every 20s, reconnect with exponential backoff (5s–60s)

### Noise encryption layer (`nativeapi/noise/`)

Optional Noise_NNpsk0_25519_ChaChaPoly_SHA256 encryption using a base64-encoded 32-byte pre-shared key from the device config. When enabled, all frames use `0x01` preamble with 2-byte big-endian length (vs plaintext `0x00` with varint length). Uses JDK 17 built-in crypto (X25519, ChaCha20-Poly1305, SHA-256, HMAC-SHA256) — no external dependencies.

- `NoiseState` — Noise NNpsk0 handshake state machine and transport encryption. I/O-free; operates on byte arrays.
- `NoiseFrameHelper` — encrypted frame wire protocol, handshake over socket, read/write encrypted `RawMessage`s.

### Native API protocol layer (`nativeapi/proto/`)

- `MessageFramer` — frame format: `[0x00] [varint payload_size] [varint msg_type] [protobuf payload]`. **The length varint is the protobuf payload size only** — it does NOT include the msg_type varint.
- `ProtobufReader` / `ProtobufWriter` — hand-rolled protobuf codec (no generated code). Reads/writes varint, fixed32, float, string, bool fields by field number.
- `EntityMessageParser` — parses raw protobuf bytes into `HashMap<String, Object>` for each entity/state message type. Field mappings are hardcoded per domain.
- `MessageTypes` — integer constants for all Native API message types with domain lookup helpers.

### Entity type system (`types/`)

`Base` is the parent class. Each domain subclass (Sensor, Light, Climate, etc.) takes a `HashMap<String, Object>` payload and exposes a `getProperties()` → `LinkedHashMap<String, Object>` used to create/update OPC UA tag values. Entity creation is a `switch` on the domain string in `NativeApiDevice.createEntity()`.

### Key patterns

- OPC UA nodes are tracked in a `ConcurrentHashMap<String, UaVariableNode> nodeList` keyed by a string like `"entityId.propName"` or `"diagnostics-connected"`.
- Domain folders are lazily created via `getOrCreateDomainFolder()`.
- Native API key ↔ entity ID mappings enable correlation between protocol-level integer keys and OPC UA node identifiers.
- Device configs are Java records with Ignition form annotations (`@FormField`, `@Label`, `@DefaultValue`).
- Resource bundles (`.properties` files alongside each Device class) provide display names and descriptions for the Ignition gateway UI.
