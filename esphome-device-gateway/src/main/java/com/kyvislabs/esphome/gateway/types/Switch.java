package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;

public class Switch extends Base{
    String state;
    Double value;
    String assumedState;

    public Switch(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",""));
        value = TypeUtilities.toDouble(payload.getOrDefault("value",null));
        assumedState = TypeUtilities.toString(payload.getOrDefault("assumed_state",null));
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
