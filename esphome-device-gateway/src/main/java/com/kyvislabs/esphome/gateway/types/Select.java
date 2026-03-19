package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Select extends Base {
    String state;
    String options;

    public Select(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state", null));
        options = TypeUtilities.toString(payload.getOrDefault("options", null));
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("state", state != null ? state : "");
        props.put("options", options != null ? options : "");
        return props;
    }

    @Override
    public String toString() {
        return "Select{" +
                "state='" + state + '\'' +
                ", options='" + options + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
