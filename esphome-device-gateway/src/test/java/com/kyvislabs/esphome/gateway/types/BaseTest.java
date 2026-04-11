package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class BaseTest {

    @Test
    void constructor_populatedFields() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "sensor-temp");
        payload.put("name", "Temperature");
        payload.put("icon", "mdi:thermometer");
        payload.put("entity_category", 1);
        var base = new Base(payload);
        assertEquals("mdi:thermometer", base.getProperties().get("icon"));
    }

    @Test
    void getProperties_nullDefaults() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "sensor-temp");
        var base = new Base(payload);
        var props = base.getProperties();
        assertTrue(props.containsKey("icon"));
        assertEquals("", props.get("icon"));
    }

    @Test
    void getProperties_onlyContainsIconKey() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "sensor-temp");
        var base = new Base(payload);
        var props = base.getProperties();
        assertEquals(1, props.size());
    }
}
