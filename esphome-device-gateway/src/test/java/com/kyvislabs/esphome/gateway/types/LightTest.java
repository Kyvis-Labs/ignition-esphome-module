package com.kyvislabs.esphome.gateway.types;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LightTest {

    private HashMap<String, Object> basePayload() {
        var payload = new HashMap<String, Object>();
        payload.put("id", "light-test");
        payload.put("name", "Test Light");
        return payload;
    }

    // ---- Native API path: flat keys color_r, color_g, color_b (0.0–1.0 floats) ----

    @Test
    void nativeApi_pureRed() {
        var payload = basePayload();
        payload.put("color_r", 1.0);
        payload.put("color_g", 0.0);
        payload.put("color_b", 0.0);
        var light = new Light(payload);
        assertEquals("#FF0000", light.getProperties().get("color"));
    }

    @Test
    void nativeApi_pureGreen() {
        var payload = basePayload();
        payload.put("color_r", 0.0);
        payload.put("color_g", 1.0);
        payload.put("color_b", 0.0);
        var light = new Light(payload);
        assertEquals("#00FF00", light.getProperties().get("color"));
    }

    @Test
    void nativeApi_pureBlue() {
        var payload = basePayload();
        payload.put("color_r", 0.0);
        payload.put("color_g", 0.0);
        payload.put("color_b", 1.0);
        var light = new Light(payload);
        assertEquals("#0000FF", light.getProperties().get("color"));
    }

    @Test
    void nativeApi_white() {
        var payload = basePayload();
        payload.put("color_r", 1.0);
        payload.put("color_g", 1.0);
        payload.put("color_b", 1.0);
        var light = new Light(payload);
        assertEquals("#FFFFFF", light.getProperties().get("color"));
    }

    @Test
    void nativeApi_black() {
        var payload = basePayload();
        payload.put("color_r", 0.0);
        payload.put("color_g", 0.0);
        payload.put("color_b", 0.0);
        var light = new Light(payload);
        assertEquals("#000000", light.getProperties().get("color"));
    }

    @Test
    void nativeApi_midValue() {
        var payload = basePayload();
        // 0.502 * 255 = 128.01 → rounds to 128 = 0x80
        payload.put("color_r", 128.0 / 255.0);
        payload.put("color_g", 128.0 / 255.0);
        payload.put("color_b", 128.0 / 255.0);
        var light = new Light(payload);
        assertEquals("#808080", light.getProperties().get("color"));
    }

    @Test
    void nativeApi_arbitrary() {
        var payload = basePayload();
        payload.put("color_r", 1.0);
        payload.put("color_g", 128.0 / 255.0);
        payload.put("color_b", 0.0);
        var light = new Light(payload);
        assertEquals("#FF8000", light.getProperties().get("color"));
    }

    // ---- SSE path: nested color map with r, g, b keys ----

    @Test
    void sse_pureRed() {
        var payload = basePayload();
        payload.put("color", Map.of("r", 1.0, "g", 0.0, "b", 0.0));
        var light = new Light(payload);
        assertEquals("#FF0000", light.getProperties().get("color"));
    }

    @Test
    void sse_white() {
        var payload = basePayload();
        payload.put("color", Map.of("r", 1.0, "g", 1.0, "b", 1.0));
        var light = new Light(payload);
        assertEquals("#FFFFFF", light.getProperties().get("color"));
    }

    @Test
    void sse_arbitrary() {
        var payload = basePayload();
        payload.put("color", Map.of("r", 1.0, "g", 128.0 / 255.0, "b", 0.0));
        var light = new Light(payload);
        assertEquals("#FF8000", light.getProperties().get("color"));
    }

    // ---- Missing color keys default to #000000 ----

    @Test
    void missingColorKeys_defaultsToBlack() {
        var payload = basePayload();
        var light = new Light(payload);
        assertEquals("#000000", light.getProperties().get("color"));
    }

    // ---- getProperties contains color key ----

    @Test
    void getProperties_containsColorKey() {
        var payload = basePayload();
        payload.put("color_r", 1.0);
        payload.put("color_g", 0.0);
        payload.put("color_b", 0.0);
        var light = new Light(payload);
        var props = light.getProperties();
        assertTrue(props.containsKey("color"));
        assertEquals("#FF0000", props.get("color"));
    }

    // ---- toString contains color hex ----

    @Test
    void toString_containsColorHex() {
        var payload = basePayload();
        payload.put("color_r", 0.0);
        payload.put("color_g", 1.0);
        payload.put("color_b", 0.0);
        var light = new Light(payload);
        assertTrue(light.toString().contains("#00FF00"));
    }
}
