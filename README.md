# ESPHome Device Driver

An Ignition 8.3 module that provides a device driver for communicating with [ESPHome](https://esphome.io/) devices via the [Native API](https://esphome.io/components/api.html) binary protocol. Entity states are mapped to OPC UA tags for use in Ignition's Designer, Vision, and Perspective.

## Features

- Bidirectional communication — read state and send commands
- Automatic entity discovery with dynamic OPC UA tag creation
- Persistent TCP connection with keepalive monitoring
- Automatic reconnection with exponential backoff (5–60 seconds)
- Write support for controllable entities (switches, lights, covers, fans, climate, etc.)
- Optional Noise encryption (`Noise_NNpsk0_25519_ChaChaPoly_SHA256`)
- 12 supported entity types
- Hierarchical OPC UA tag organization
- Diagnostic tags for connection health monitoring
- Device metadata exposure

## Supported Entity Types

| Domain | Properties | Writable |
|---|---|---|
| `binary_sensor` | value (Boolean), state, icon | — |
| `sensor` | value (Double), state, uom, icon | — |
| `text_sensor` | value (String), state, icon | — |
| `number` | value (Double), state, uom, min_value, max_value, step, mode, icon | value |
| `switch` | value (Boolean), state, assumed_state, icon | value |
| `light` | state, effect, color_mode, brightness, color, effects, icon | state, brightness, color, effect |
| `button` | value (Boolean), icon | value |
| `cover` | position, tilt, current_operation, state, icon | position, tilt |
| `fan` | value, state, oscillating, speed_level, direction, preset_mode, icon | value, speed_level, oscillating, direction |
| `select` | state, options, icon | state |
| `climate` | mode, current_temperature, target_temperature, action, fan_mode, swing_mode, supported_modes, supported_fan_modes, supported_swing_modes, supported_presets, icon | mode, target_temperature, fan_mode, swing_mode |
| `lock` | state, icon | state |

## OPC UA Tag Structure

```
[Device Name]/
  state/
    binary_sensor/
      <Entity Name>/
        value       (Boolean)
        state       (String)
        icon        (String)
    sensor/
      <Entity Name>/
        value       (Double)
        state       (String)
        uom         (String)
        icon        (String)
    ...
  meta/
    title           (String)
    comment         (String)
    hw_version      (String)
    sw_version      (String)
    ...
  [Diagnostics]/
    Connected        (Boolean)
    State            (String)
    Status           (String)
    Host             (String)
    Connection Count (Integer)
    Received Messages (Long)
    Last Message Size (Integer)
    Sent Messages     (Long)
    Received Bytes    (Long)
    Sent Bytes        (Long)
    Reset Counters    (Boolean, writable)
```

## Configuration

1. Install the module on your Ignition 8.3 gateway
2. Navigate to **Connections > Devices > Connections**
3. Create a new **ESPHome Device (Native API)** device
4. Set the **Host** to your ESPHome device's IP address (e.g., `192.168.1.100`)
5. Set the **Port** (default: `6053`)
6. Optionally set the **Encryption Key** if the device has API encryption enabled

### Encryption

The driver supports the Noise encryption protocol used by ESPHome's Native API. To enable it:

1. Configure the `encryption` key in your ESPHome device's YAML:
   ```yaml
   api:
     encryption:
       key: "<base64-encoded key>"
   ```
2. Copy the same base64-encoded key into the **Encryption Key** field in the device configuration

The encryption uses `Noise_NNpsk0_25519_ChaChaPoly_SHA256` with JDK 17 built-in crypto (X25519, ChaCha20-Poly1305, SHA-256) — no external dependencies required.

When encryption is not configured, the driver communicates in plaintext.

## Requirements

- Ignition 8.3
- Java 17+
- ESPHome device with the [Native API](https://esphome.io/components/api.html) component enabled

## Building

```bash
mvn package
```

The built module `.modl` file will be located in `esphome-device-build/target/`.
