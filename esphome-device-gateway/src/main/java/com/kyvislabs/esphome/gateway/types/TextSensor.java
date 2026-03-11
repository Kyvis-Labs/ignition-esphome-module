package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class TextSensor extends Base{
    String state;
    String value;

    public TextSensor(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",""));
        value = TypeUtilities.toString(payload.getOrDefault("value",null));
    }

    public String getValue() {
        return value != null ? value : "";
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("value", value != null ? value : "");
        props.put("state", state != null ? state : "");
        return props;
    }

    @Override
    public String toString() {
        return "TextSensor{" +
                "state='" + state + '\'' +
                ", value='" + value + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
