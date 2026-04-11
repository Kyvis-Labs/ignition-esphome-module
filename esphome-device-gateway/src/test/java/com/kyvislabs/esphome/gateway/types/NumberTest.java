package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class NumberTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "number-brightness");
        payload.put("name", "Brightness");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("state", "50.0");
        payload.put("value", 50.0);
        payload.put("uom", "%");
        payload.put("min_value", 0.0);
        payload.put("max_value", 100.0);
        payload.put("step", 1.0);
        payload.put("mode", 1);
        var entity = new Number(payload);
        var props = entity.getProperties();
        assertEquals(50.0, props.get("value"));
        assertEquals("50.0", props.get("state"));
        assertEquals("%", props.get("uom"));
        assertEquals(0.0, props.get("min_value"));
        assertEquals(100.0, props.get("max_value"));
        assertEquals(1.0, props.get("step"));
        assertEquals(1, props.get("mode"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new Number(basePayload());
        var props = entity.getProperties();
        assertEquals(0.0, props.get("value"));
        assertEquals("", props.get("state"));
        assertEquals("", props.get("uom"));
        assertEquals(0.0, props.get("min_value"));
        assertEquals(0.0, props.get("max_value"));
        assertEquals(0.0, props.get("step"));
        assertEquals(0, props.get("mode"));
    }

    @Test
    void getValue_returnsValue() {
        var payload = basePayload();
        payload.put("value", 75.0);
        assertEquals(75.0, new Number(payload).getValue());
    }

    @Test
    void getValue_nullReturnsZero() {
        assertEquals(0.0, new Number(basePayload()).getValue());
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("value", 50.0);
        payload.put("uom", "%");
        var entity = new Number(payload);
        assertTrue(entity.toString().contains("Number"));
        assertTrue(entity.toString().contains("50.0"));
    }
}
