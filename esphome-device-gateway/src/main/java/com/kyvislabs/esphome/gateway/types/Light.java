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
    Double colorR;
    Double colorG;
    Double colorB;
    String effects;

    @SuppressWarnings("unchecked")
    public Light(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",""));
        effect = TypeUtilities.toString(payload.getOrDefault("effect",null));
        colorMode = TypeUtilities.toString(payload.getOrDefault("color_mode",null));
        brightness = TypeUtilities.toDouble(payload.getOrDefault("brightness",null));

        if (payload.containsKey("color") && payload.get("color") instanceof Map) {
            Map<String, Object> colorMap = (Map<String, Object>) payload.get("color");
            colorR = TypeUtilities.toDouble(colorMap.getOrDefault("r", null));
            colorG = TypeUtilities.toDouble(colorMap.getOrDefault("g", null));
            colorB = TypeUtilities.toDouble(colorMap.getOrDefault("b", null));
        }

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
        props.put("color_r", colorR != null ? colorR : 0.0);
        props.put("color_g", colorG != null ? colorG : 0.0);
        props.put("color_b", colorB != null ? colorB : 0.0);
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
                ", colorR=" + colorR +
                ", colorG=" + colorG +
                ", colorB=" + colorB +
                ", effects='" + effects + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
