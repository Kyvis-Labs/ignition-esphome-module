# ESPHome Device Driver

An Ignition 8.3 module that provides device drivers for communicating with [ESPHome](https://esphome.io/) devices. Entity states are mapped to OPC UA tags for use in Ignition's Designer, Vision, and Perspective.

Two device driver types are included:

- **SSE (HTTP)** — Real-time, read-only state monitoring via Server-Sent Events
- **Native API** — Full bidirectional control via the ESPHome Native API binary protocol

## Features

### SSE Device

- Real-time state updates via HTTP Server-Sent Events
- Automatic reconnection on connection failure
- Read-only OPC UA tags organized by domain type

### Native API Device

- Bidirectional communication — read state and send commands
- Automatic entity discovery with dynamic OPC UA tag creation
- Persistent TCP connection with keepalive monitoring
- Automatic reconnection with exponential backoff (5–60 seconds)
- Write support for controllable entities (switches, lights, covers, fans, climate, etc.)

### Common

- 12 supported entity types
- Hierarchical OPC UA tag organization
- Diagnostic tags for connection health monitoring
- Device metadata exposure

## Supported Entity Types

| Domain | Properties | Writable (Native API) |
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
| `climate` | mode, current_temperature, target_temperature, action, fan_mode, swing_mode, icon | mode, target_temperature, fan_mode, swing_mode |
| `lock` | state, icon | state |

All entity types are read-only when using the SSE device driver. The Native API device driver supports writes for the properties listed in the **Writable** column.

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
    URL              (String)     — SSE only
    Host             (String)     — Native API only
    Connection Count (Integer)
```

## Configuration

### SSE Device

1. Install the module on your Ignition 8.3 gateway
2. Navigate to **Connections > Devices > Connections**
3. Create a new **ESPHome Device** device
4. Set the **URL** to your ESPHome device's event stream endpoint (e.g., `http://192.168.1.100/events`)

**Requires** the [Web Server](https://esphome.io/components/web_server.html) component enabled on the ESPHome device.

### Native API Device

1. Install the module on your Ignition 8.3 gateway
2. Navigate to **Connections > Devices > Connections**
3. Create a new **ESPHome Device (Native API)** device
4. Set the **Host** to your ESPHome device's IP address (e.g., `192.168.1.100`)
5. Set the **Port** (default: `6053`)

**Requires** the [Native API](https://esphome.io/components/api.html) component enabled on the ESPHome device. API encryption and authentication are not currently supported.

## Requirements

- Ignition 8.3
- Java 17+
- ESPHome device with the Web Server component (for SSE) or Native API component (for Native API) enabled

## Building

```bash
mvn package
```

The built module `.modl` file will be located in `esphome-device-build/target/`.
