package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class CoverTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "cover-garage");
        payload.put("name", "Garage Door");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("position", 0.75);
        payload.put("tilt", 0.5);
        payload.put("current_operation", "OPENING");
        payload.put("state", "OPEN");
        var entity = new Cover(payload);
        var props = entity.getProperties();
        assertEquals(0.75, props.get("position"));
        assertEquals(0.5, props.get("tilt"));
        assertEquals("OPENING", props.get("current_operation"));
        assertEquals("OPEN", props.get("state"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new Cover(basePayload());
        var props = entity.getProperties();
        assertEquals(0.0, props.get("position"));
        assertEquals(0.0, props.get("tilt"));
        assertEquals("", props.get("current_operation"));
        assertEquals("", props.get("state"));
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("state", "CLOSED");
        payload.put("current_operation", "IDLE");
        var entity = new Cover(payload);
        assertTrue(entity.toString().contains("Cover"));
        assertTrue(entity.toString().contains("CLOSED"));
        assertTrue(entity.toString().contains("IDLE"));
    }
}
