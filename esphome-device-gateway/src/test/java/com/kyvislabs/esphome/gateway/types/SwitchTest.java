package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class SwitchTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "switch-relay");
        payload.put("name", "Relay");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("state", "ON");
        payload.put("value", true);
        payload.put("assumed_state", "true");
        var entity = new Switch(payload);
        var props = entity.getProperties();
        assertEquals(true, props.get("value"));
        assertEquals("ON", props.get("state"));
        assertEquals("true", props.get("assumed_state"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new Switch(basePayload());
        var props = entity.getProperties();
        assertEquals(false, props.get("value"));
        assertEquals("", props.get("state"));
        assertEquals("", props.get("assumed_state"));
    }

    @Test
    void getValue_returnsValue() {
        var payload = basePayload();
        payload.put("value", true);
        assertEquals(true, new Switch(payload).getValue());
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("state", "OFF");
        var entity = new Switch(payload);
        assertTrue(entity.toString().contains("Switch"));
        assertTrue(entity.toString().contains("OFF"));
    }
}
