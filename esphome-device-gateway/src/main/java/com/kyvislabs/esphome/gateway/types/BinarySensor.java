package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class BinarySensor extends Base{
    String state;
    Boolean value;

    public BinarySensor(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",null));
        value = TypeUtilities.toBool(payload.getOrDefault("value", null));
    }

    public Boolean getValue() {
        return value;
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("value", value != null ? value : false);
        props.put("state", state != null ? state : "");
        return props;
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
