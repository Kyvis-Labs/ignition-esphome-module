package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Light extends Base{
    String state;
    String effect;
    String colorMode;
    Double brightness;
    String color;
    String effects;

    @SuppressWarnings("unchecked")
    public Light(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",""));
        effect = TypeUtilities.toString(payload.getOrDefault("effect",null));
        colorMode = TypeUtilities.toString(payload.getOrDefault("color_mode",null));
        brightness = TypeUtilities.toDouble(payload.getOrDefault("brightness",null));

        double r = 0, g = 0, b = 0;
        if (payload.containsKey("color") && payload.get("color") instanceof Map) {
            Map<String, Object> colorMap = (Map<String, Object>) payload.get("color");
            r = TypeUtilities.toDouble(colorMap.getOrDefault("r", 0.0));
            g = TypeUtilities.toDouble(colorMap.getOrDefault("g", 0.0));
            b = TypeUtilities.toDouble(colorMap.getOrDefault("b", 0.0));
        } else {
            r = TypeUtilities.toDouble(payload.getOrDefault("color_r", 0.0));
            g = TypeUtilities.toDouble(payload.getOrDefault("color_g", 0.0));
            b = TypeUtilities.toDouble(payload.getOrDefault("color_b", 0.0));
        }
        color = String.format("#%02X%02X%02X",
                (int) Math.round(r * 255), (int) Math.round(g * 255), (int) Math.round(b * 255));

        if (payload.containsKey("effects") && payload.get("effects") instanceof List) {
            List<Object> effectsList = (List<Object>) payload.get("effects");
            effects = String.join(", ", effectsList.stream().map(String::valueOf).toList());
        }
    }

    public String getState() {
        return state != null ? state : "";
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("state", state != null ? state : "");
        props.put("effect", effect != null ? effect : "");
        props.put("color_mode", colorMode != null ? colorMode : "");
        props.put("brightness", brightness != null ? brightness : 0.0);
        props.put("color", color != null ? color : "#000000");
        props.put("effects", effects != null ? effects : "");
        return props;
    }

    @Override
    public String toString() {
        return "Light{" +
                "state='" + state + '\'' +
                ", effect='" + effect + '\'' +
                ", colorMode='" + colorMode + '\'' +
                ", brightness=" + brightness +
                ", color='" + color + '\'' +
                ", effects='" + effects + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
