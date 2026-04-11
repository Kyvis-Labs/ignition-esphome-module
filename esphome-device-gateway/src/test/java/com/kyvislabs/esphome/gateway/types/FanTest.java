package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class FanTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "fan-ceiling");
        payload.put("name", "Ceiling Fan");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("value", true);
        payload.put("state", "ON");
        payload.put("oscillating", true);
        payload.put("speed_level", 3);
        payload.put("direction", "FORWARD");
        payload.put("preset_mode", "breeze");
        var entity = new Fan(payload);
        var props = entity.getProperties();
        assertEquals(true, props.get("value"));
        assertEquals("ON", props.get("state"));
        assertEquals(true, props.get("oscillating"));
        assertEquals(3, props.get("speed_level"));
        assertEquals("FORWARD", props.get("direction"));
        assertEquals("breeze", props.get("preset_mode"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new Fan(basePayload());
        var props = entity.getProperties();
        assertEquals(false, props.get("value"));
        assertEquals("", props.get("state"));
        assertEquals(false, props.get("oscillating"));
        assertEquals(0, props.get("speed_level"));
        assertEquals("", props.get("direction"));
        assertEquals("", props.get("preset_mode"));
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("state", "ON");
        payload.put("direction", "REVERSE");
        var entity = new Fan(payload);
        assertTrue(entity.toString().contains("Fan"));
        assertTrue(entity.toString().contains("REVERSE"));
    }
}
