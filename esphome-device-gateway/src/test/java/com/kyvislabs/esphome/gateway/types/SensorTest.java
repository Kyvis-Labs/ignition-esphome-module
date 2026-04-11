package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class SensorTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "sensor-temperature");
        payload.put("name", "Temperature");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("state", "23.5");
        payload.put("value", 23.5);
        payload.put("uom", "\u00b0C");
        var entity = new Sensor(payload);
        var props = entity.getProperties();
        assertEquals(23.5, props.get("value"));
        assertEquals("23.5", props.get("state"));
        assertEquals("\u00b0C", props.get("uom"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new Sensor(basePayload());
        var props = entity.getProperties();
        assertEquals(0.0, props.get("value"));
        assertEquals("", props.get("state"));
        assertEquals("", props.get("uom"));
    }

    @Test
    void getValue_returnsValue() {
        var payload = basePayload();
        payload.put("value", 42.0);
        assertEquals(42.0, new Sensor(payload).getValue());
    }

    @Test
    void getValue_nullReturnsZero() {
        assertEquals(0.0, new Sensor(basePayload()).getValue());
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("state", "23.5");
        payload.put("uom", "\u00b0C");
        var entity = new Sensor(payload);
        assertTrue(entity.toString().contains("Sensor"));
        assertTrue(entity.toString().contains("23.5"));
    }
}
