package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class BinarySensorTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "binary_sensor-motion");
        payload.put("name", "Motion Sensor");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("state", "ON");
        payload.put("value", true);
        var entity = new BinarySensor(payload);
        var props = entity.getProperties();
        assertEquals(true, props.get("value"));
        assertEquals("ON", props.get("state"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new BinarySensor(basePayload());
        var props = entity.getProperties();
        assertEquals(false, props.get("value"));
        assertEquals("", props.get("state"));
        assertEquals("", props.get("icon"));
    }

    @Test
    void getValue_returnsValue() {
        var payload = basePayload();
        payload.put("value", true);
        var entity = new BinarySensor(payload);
        assertEquals(true, entity.getValue());
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("state", "OFF");
        payload.put("value", false);
        var entity = new BinarySensor(payload);
        var str = entity.toString();
        assertTrue(str.contains("BinarySensor"));
        assertTrue(str.contains("OFF"));
    }
}
