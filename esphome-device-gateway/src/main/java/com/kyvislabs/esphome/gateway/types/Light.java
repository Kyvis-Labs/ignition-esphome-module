package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import com.inductiveautomation.ignition.common.gson.internal.LinkedTreeMap;

public class Light extends Base{
    String state;
    String value;
    String effect;
    String colorMode;
    Short brightness;
    LinkedTreeMap<String,Short> color;
    List<String> effects;

    public Light(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",""));
        effect = TypeUtilities.toString(payload.getOrDefault("effect",null));
        colorMode = TypeUtilities.toString(payload.getOrDefault("color_mode",null));
        brightness = TypeUtilities.toShort(payload.getOrDefault("brightness",null));
        if (payload.containsKey("color")){
            color = (LinkedTreeMap<String,Short>) payload.get("color");
        }

        if (payload.containsKey("effects")){
            effects = (List<String>) payload.get("effects");
        }

    }

    @Override
    public String toString() {
        return "Light{" +
                "state='" + state + '\'' +
                ", value='" + value + '\'' +
                ", effect='" + effect + '\'' +
                ", colorMode='" + colorMode + '\'' +
                ", brightness=" + brightness +
                ", color=" + color +
                ", effects=" + effects +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
