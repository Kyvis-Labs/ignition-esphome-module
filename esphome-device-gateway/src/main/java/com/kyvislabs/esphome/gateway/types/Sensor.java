package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;

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
