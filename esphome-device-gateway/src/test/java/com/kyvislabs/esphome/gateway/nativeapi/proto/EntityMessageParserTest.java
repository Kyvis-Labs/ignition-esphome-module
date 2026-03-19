package com.kyvislabs.esphome.gateway.nativeapi.proto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class EntityMessageParserTest {

    // ---- parseLightState ----

    @Test
    void parseLightState_allFields() {
        var writer = new ProtobufWriter();
        // field 1: key (fixed32)
        writer.writeFixed32Field(1, 12345);
        // field 2: state (bool) — ON
        writer.writeBoolField(2, true);
        // field 3: brightness (float)
        writer.writeFloatField(3, 0.75f);
        // field 4: color_r (float)
        writer.writeFloatField(4, 1.0f);
        // field 5: color_g (float)
        writer.writeFloatField(5, 0.5f);
        // field 6: color_b (float)
        writer.writeFloatField(6, 0.25f);
        // field 9: effect (string)
        writer.writeStringField(9, "rainbow");

        HashMap<String, Object> result = EntityMessageParser.parseStateResponse(
                MessageTypes.LIGHT_STATE_RESPONSE, writer.toByteArray());

        assertNotNull(result);
        assertEquals("light", result.get("domain"));
        assertEquals(12345, result.get("key"));
        assertEquals("ON", result.get("state"));
        assertEquals(0.75, (double) result.get("brightness"), 0.001);
        assertEquals(1.0, (double) result.get("color_r"), 0.001);
        assertEquals(0.5, (double) result.get("color_g"), 0.001);
        assertEquals(0.25, (double) result.get("color_b"), 0.001);
        assertEquals("rainbow", result.get("effect"));
    }

    @Test
    void parseLightState_offState() {
        var writer = new ProtobufWriter();
        writer.writeFixed32Field(1, 99);
        writer.writeBoolField(2, false);

        HashMap<String, Object> result = EntityMessageParser.parseStateResponse(
                MessageTypes.LIGHT_STATE_RESPONSE, writer.toByteArray());

        assertNotNull(result);
        assertEquals("OFF", result.get("state"));
    }

    // ---- parseDeviceInfo ----

    @Test
    void parseDeviceInfo_basicFields() {
        var writer = new ProtobufWriter();
        // field 2: name
        writer.writeStringField(2, "my-device");
        // field 3: mac_address
        writer.writeStringField(3, "AA:BB:CC:DD:EE:FF");
        // field 4: esphome_version
        writer.writeStringField(4, "2024.1.0");

        HashMap<String, Object> result = EntityMessageParser.parseDeviceInfo(writer.toByteArray());

        assertNotNull(result);
        assertEquals("my-device", result.get("name"));
        assertEquals("AA:BB:CC:DD:EE:FF", result.get("mac_address"));
        assertEquals("2024.1.0", result.get("esphome_version"));
    }

    @Test
    void parseDeviceInfo_allFields() {
        var writer = new ProtobufWriter();
        writer.writeBoolField(1, false);           // uses_password
        writer.writeStringField(2, "test-node");   // name
        writer.writeStringField(3, "11:22:33:44:55:66"); // mac_address
        writer.writeStringField(4, "2024.2.0");    // esphome_version
        writer.writeStringField(5, "Jan 1 2024");  // compilation_time
        writer.writeStringField(6, "ESP32");        // model
        writer.writeBoolField(7, true);             // has_deep_sleep
        writer.writeStringField(8, "my_project");   // project_name
        writer.writeStringField(9, "1.0.0");        // project_version

        HashMap<String, Object> result = EntityMessageParser.parseDeviceInfo(writer.toByteArray());

        assertEquals(false, result.get("uses_password"));
        assertEquals("test-node", result.get("name"));
        assertEquals("11:22:33:44:55:66", result.get("mac_address"));
        assertEquals("2024.2.0", result.get("esphome_version"));
        assertEquals("Jan 1 2024", result.get("compilation_time"));
        assertEquals("ESP32", result.get("model"));
        assertEquals(true, result.get("has_deep_sleep"));
        assertEquals("my_project", result.get("project_name"));
        assertEquals("1.0.0", result.get("project_version"));
    }

    @Test
    void parseDeviceInfo_emptyPayload() {
        HashMap<String, Object> result = EntityMessageParser.parseDeviceInfo(new byte[0]);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---- parseStateResponse returns null for unknown message types ----

    @Test
    void parseStateResponse_unknownType_returnsNull() {
        HashMap<String, Object> result = EntityMessageParser.parseStateResponse(
                9999, new byte[0]);
        assertNull(result);
    }

    // ---- parseBinarySensorState ----

    @Test
    void parseBinarySensorState_on() {
        var writer = new ProtobufWriter();
        writer.writeFixed32Field(1, 42);
        writer.writeBoolField(2, true);

        HashMap<String, Object> result = EntityMessageParser.parseStateResponse(
                MessageTypes.BINARY_SENSOR_STATE_RESPONSE, writer.toByteArray());

        assertNotNull(result);
        assertEquals("binary_sensor", result.get("domain"));
        assertEquals(42, result.get("key"));
        assertEquals(true, result.get("value"));
        assertEquals("ON", result.get("state"));
    }

    // ---- parseSensorState ----

    @Test
    void parseSensorState_floatValue() {
        var writer = new ProtobufWriter();
        writer.writeFixed32Field(1, 100);
        writer.writeFloatField(2, 23.5f);

        HashMap<String, Object> result = EntityMessageParser.parseStateResponse(
                MessageTypes.SENSOR_STATE_RESPONSE, writer.toByteArray());

        assertNotNull(result);
        assertEquals("sensor", result.get("domain"));
        assertEquals(23.5, (double) result.get("value"), 0.001);
    }
}
