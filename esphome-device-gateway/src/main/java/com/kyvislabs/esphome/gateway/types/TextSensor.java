package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;

public class TextSensor extends Base{
    String state;
    String value;

    public TextSensor(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",""));
        value = TypeUtilities.toString(payload.getOrDefault("value",null));
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
