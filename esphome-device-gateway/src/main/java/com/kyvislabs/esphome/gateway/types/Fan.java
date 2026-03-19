package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Fan extends Base {
    Boolean value;
    String state;
    Boolean oscillating;
    Integer speedLevel;
    String direction;
    String presetMode;

    public Fan(HashMap<String, Object> payload) {
        super(payload);
        value = TypeUtilities.toBool(payload.getOrDefault("value", null));
        state = TypeUtilities.toString(payload.getOrDefault("state", null));
        oscillating = TypeUtilities.toBool(payload.getOrDefault("oscillating", null));
        speedLevel = TypeUtilities.toInteger(payload.getOrDefault("speed_level", null));
        direction = TypeUtilities.toString(payload.getOrDefault("direction", null));
        presetMode = TypeUtilities.toString(payload.getOrDefault("preset_mode", null));
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("value", value != null ? value : false);
        props.put("state", state != null ? state : "");
        props.put("oscillating", oscillating != null ? oscillating : false);
        props.put("speed_level", speedLevel != null ? speedLevel : 0);
        props.put("direction", direction != null ? direction : "");
        props.put("preset_mode", presetMode != null ? presetMode : "");
        return props;
    }

    @Override
    public String toString() {
        return "Fan{" +
                "value=" + value +
                ", state='" + state + '\'' +
                ", oscillating=" + oscillating +
                ", speedLevel=" + speedLevel +
                ", direction='" + direction + '\'' +
                ", presetMode='" + presetMode + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
