package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;

public class Number extends Base{
    String state;
    Double value;
    String units;
    Double minValue;
    Double maxValue;
    Double step;
    Integer mode;


    public Number(HashMap<String, Object> payload) {
        super(payload);
        state = TypeUtilities.toString(payload.getOrDefault("state",""));
        value = TypeUtilities.toDouble(payload.getOrDefault("value",null));
        units = TypeUtilities.toString(payload.getOrDefault("uom",null));
        minValue = TypeUtilities.toDouble(payload.getOrDefault("min_value",null));
        maxValue = TypeUtilities.toDouble(payload.getOrDefault("max_value",null));
        step = TypeUtilities.toDouble(payload.getOrDefault("step",null));
        mode = TypeUtilities.toInteger(payload.getOrDefault("mode",null));
    }

    @Override
    public String toString() {
        return "Number{" +
                "state='" + state + '\'' +
                ", value=" + value +
                ", units='" + units + '\'' +
                ", minValue=" + minValue +
                ", maxValue=" + maxValue +
                ", step=" + step +
                ", mode=" + mode +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
