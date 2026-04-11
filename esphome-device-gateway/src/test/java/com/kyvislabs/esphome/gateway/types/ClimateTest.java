package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ClimateTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "climate-hvac");
        payload.put("name", "HVAC");
        return payload;
    }

    @Test
    void constructor_populatedFields() {
        var payload = basePayload();
        payload.put("mode", "COOL");
        payload.put("current_temperature", 24.5);
        payload.put("target_temperature", 22.0);
        payload.put("action", "COOLING");
        payload.put("fan_mode", "AUTO");
        payload.put("swing_mode", "VERTICAL");
        payload.put("supported_modes", "OFF, COOL, HEAT, AUTO");
        payload.put("supported_fan_modes", "ON, OFF, AUTO, LOW, HIGH");
        payload.put("supported_swing_modes", "OFF, VERTICAL, HORIZONTAL");
        payload.put("supported_presets", "HOME, AWAY, ECO");
        var entity = new Climate(payload);
        var props = entity.getProperties();
        assertEquals("COOL", props.get("mode"));
        assertEquals(24.5, props.get("current_temperature"));
        assertEquals(22.0, props.get("target_temperature"));
        assertEquals("COOLING", props.get("action"));
        assertEquals("AUTO", props.get("fan_mode"));
        assertEquals("VERTICAL", props.get("swing_mode"));
        assertEquals("OFF, COOL, HEAT, AUTO", props.get("supported_modes"));
        assertEquals("ON, OFF, AUTO, LOW, HIGH", props.get("supported_fan_modes"));
        assertEquals("OFF, VERTICAL, HORIZONTAL", props.get("supported_swing_modes"));
        assertEquals("HOME, AWAY, ECO", props.get("supported_presets"));
    }

    @Test
    void getProperties_nullDefaults() {
        var entity = new Climate(basePayload());
        var props = entity.getProperties();
        assertEquals("", props.get("mode"));
        assertEquals(0.0, props.get("current_temperature"));
        assertEquals(0.0, props.get("target_temperature"));
        assertEquals("", props.get("action"));
        assertEquals("", props.get("fan_mode"));
        assertEquals("", props.get("swing_mode"));
        assertEquals("", props.get("supported_modes"));
        assertEquals("", props.get("supported_fan_modes"));
        assertEquals("", props.get("supported_swing_modes"));
        assertEquals("", props.get("supported_presets"));
    }

    @Test
    void toString_containsExpectedValues() {
        var payload = basePayload();
        payload.put("mode", "HEAT");
        payload.put("target_temperature", 25.0);
        payload.put("supported_modes", "OFF, HEAT");
        var entity = new Climate(payload);
        var str = entity.toString();
        assertTrue(str.contains("Climate"));
        assertTrue(str.contains("HEAT"));
        assertTrue(str.contains("25.0"));
        assertTrue(str.contains("OFF, HEAT"));
    }
}
