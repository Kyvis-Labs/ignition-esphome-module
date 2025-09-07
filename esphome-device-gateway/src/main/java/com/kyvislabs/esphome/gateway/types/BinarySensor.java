package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;

public class BinarySensor extends Base{
    String state;
    Boolean value;

    public BinarySensor(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",null));
        value = TypeUtilities.toBool(payload.getOrDefault("value", null));
    }

    @Override
    public String toString() {
        return "BinarySensor{" +
                "state='" + state + '\'' +
                ", value=" + value +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
