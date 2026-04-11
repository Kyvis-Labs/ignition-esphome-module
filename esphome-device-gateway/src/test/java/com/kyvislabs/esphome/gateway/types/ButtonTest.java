package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ButtonTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "button-restart");
        payload.put("name", "Restart");
        return payload;
    }

    @Test
    void constructor_valueAlwaysFalse() {
        var entity = new Button(basePayload());
        assertEquals(false, entity.getValue());
    }

    @Test
    void getProperties_containsValue() {
        var entity = new Button(basePayload());
        var props = entity.getProperties();
        assertEquals(false, props.get("value"));
        assertTrue(props.containsKey("icon"));
    }

    @Test
    void toString_containsExpectedValues() {
        var entity = new Button(basePayload());
        assertTrue(entity.toString().contains("Button"));
        assertTrue(entity.toString().contains("value=false"));
    }
}
