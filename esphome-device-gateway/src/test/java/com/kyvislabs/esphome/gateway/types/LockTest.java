package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class LockTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "lock-front-door");
        payload.put("name", "Front Door");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("state", "LOCKED");
        var entity = new Lock(payload);
        assertEquals("LOCKED", entity.getProperties().get("state"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new Lock(basePayload());
        var props = entity.getProperties();
        assertEquals("", props.get("state"));
        assertTrue(props.containsKey("icon"));
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("state", "UNLOCKED");
        var entity = new Lock(payload);
        assertTrue(entity.toString().contains("Lock"));
        assertTrue(entity.toString().contains("UNLOCKED"));
    }
}
