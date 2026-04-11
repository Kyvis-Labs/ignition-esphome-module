package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class TextSensorTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "text_sensor-version");
        payload.put("name", "Version");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("state", "2026.2.2");
        payload.put("value", "2026.2.2");
        var entity = new TextSensor(payload);
        var props = entity.getProperties();
        assertEquals("2026.2.2", props.get("value"));
        assertEquals("2026.2.2", props.get("state"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new TextSensor(basePayload());
        var props = entity.getProperties();
        assertEquals("", props.get("value"));
        assertEquals("", props.get("state"));
    }

    @Test
    void getValue_returnsValue() {
        var payload = basePayload();
        payload.put("value", "hello");
        assertEquals("hello", new TextSensor(payload).getValue());
    }

    @Test
    void getValue_nullReturnsEmpty() {
        assertEquals("", new TextSensor(basePayload()).getValue());
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("state", "test");
        var entity = new TextSensor(payload);
        assertTrue(entity.toString().contains("TextSensor"));
        assertTrue(entity.toString().contains("test"));
    }
}
