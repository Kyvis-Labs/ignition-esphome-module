package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Cover extends Base {
    Double position;
    Double tilt;
    String currentOperation;
    String state;

    public Cover(HashMap<String, Object> payload) {
        super(payload);
        position = TypeUtilities.toDouble(payload.getOrDefault("position", null));
        tilt = TypeUtilities.toDouble(payload.getOrDefault("tilt", null));
        currentOperation = TypeUtilities.toString(payload.getOrDefault("current_operation", null));
        state = TypeUtilities.toString(payload.getOrDefault("state", null));
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("position", position != null ? position : 0.0);
        props.put("tilt", tilt != null ? tilt : 0.0);
        props.put("current_operation", currentOperation != null ? currentOperation : "");
        props.put("state", state != null ? state : "");
        return props;
    }

    @Override
    public String toString() {
        return "Cover{" +
                "position=" + position +
                ", tilt=" + tilt +
                ", currentOperation='" + currentOperation + '\'' +
                ", state='" + state + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
