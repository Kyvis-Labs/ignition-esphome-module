# ESPHome Device Driver

An Ignition 8.3 module that provides a device driver for communicating with [ESPHome](https://esphome.io/) devices via their HTTP Server-Sent Events (SSE) stream. Entity states are mapped to OPC UA tags for use in Ignition's Designer, Vision, and Perspective.

## Features

- Real-time state updates via SSE
- Automatic reconnection on connection failure
- OPC UA tags organized by domain type with per-entity property folders
- Diagnostics tags for monitoring connection health

## Supported Entity Types

| Domain | Properties |
|---|---|
| `binary_sensor` | value, state, icon |
| `sensor` | value, state, uom, icon |
| `text_sensor` | value, state, icon |
| `number` | value, state, uom, min_value, max_value, step, mode, icon |
| `switch` | value, state, assumed_state, icon |
| `light` | state, effect, color_mode, brightness, color_r, color_g, color_b, effects, icon |
| `button` | value, icon |

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
    ...
  [Diagnostics]/
    Connected        (Boolean)
    State            (String)
    Status           (String)
    URL              (String)
    Connection Count (Integer)
```

## Configuration

1. Install the module on your Ignition 8.3 gateway
2. Navigate to **Config > OPC UA > Device Connections**
3. Create a new **ESPHome** device
4. Set the **URL** to your ESPHome device's event stream endpoint (e.g., `http://192.168.1.100/events`)

## Requirements

- Ignition 8.3
- ESPHome device with the [Web Server](https://esphome.io/components/web_server.html) component enabled

## Building

```bash
mvn package
```

The built module `.modl` file will be located in `esphome-device-build/target/`.
