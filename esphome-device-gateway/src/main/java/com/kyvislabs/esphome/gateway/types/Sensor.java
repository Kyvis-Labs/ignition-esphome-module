package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Sensor extends Base{
    String state;
    Double value;
    String units;

    public Sensor(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",""));
        value = TypeUtilities.toDouble(payload.getOrDefault("value",null));
        units = TypeUtilities.toString(payload.getOrDefault("uom",null));
    }

    public Double getValue() {
        return value != null ? value : 0.0;
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("value", value != null ? value : 0.0);
        props.put("state", state != null ? state : "");
        props.put("uom", units != null ? units : "");
        return props;
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "state='" + state + '\'' +
                ", value=" + value +
                ", units='" + units + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
