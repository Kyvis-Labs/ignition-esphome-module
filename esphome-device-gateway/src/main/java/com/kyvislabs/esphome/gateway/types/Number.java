package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

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

    public Double getValue() {
        return value != null ? value : 0.0;
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("value", value != null ? value : 0.0);
        props.put("state", state != null ? state : "");
        props.put("uom", units != null ? units : "");
        props.put("min_value", minValue != null ? minValue : 0.0);
        props.put("max_value", maxValue != null ? maxValue : 0.0);
        props.put("step", step != null ? step : 0.0);
        props.put("mode", mode != null ? mode : 0);
        return props;
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
