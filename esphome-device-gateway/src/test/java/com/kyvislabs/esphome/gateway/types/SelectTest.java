package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class SelectTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "select-preset");
        payload.put("name", "Preset");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("state", "eco");
        payload.put("options", "eco, comfort, sleep");
        var entity = new Select(payload);
        var props = entity.getProperties();
        assertEquals("eco", props.get("state"));
        assertEquals("eco, comfort, sleep", props.get("options"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new Select(basePayload());
        var props = entity.getProperties();
        assertEquals("", props.get("state"));
        assertEquals("", props.get("options"));
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("state", "comfort");
        var entity = new Select(payload);
        assertTrue(entity.toString().contains("Select"));
        assertTrue(entity.toString().contains("comfort"));
    }
}
