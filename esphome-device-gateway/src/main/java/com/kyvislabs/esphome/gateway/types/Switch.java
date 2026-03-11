package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Switch extends Base{
    String state;
    Boolean value;
    String assumedState;

    public Switch(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",""));
        value = TypeUtilities.toBool(payload.getOrDefault("value",null));
        assumedState = TypeUtilities.toString(payload.getOrDefault("assumed_state",null));
    }

    public Boolean getValue() {
        return value != null ? value : false;
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("value", value != null ? value : false);
        props.put("state", state != null ? state : "");
        props.put("assumed_state", assumedState != null ? assumedState : "");
        return props;
    }

    @Override
    public String toString() {
        return "Switch{" +
                "state='" + state + '\'' +
                ", value=" + value +
                ", assumedState='" + assumedState + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
