package com.kyvislabs.esphome.gateway.types;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Button extends Base{
    Boolean value;

    public Button(HashMap<String, Object> payload) {
        super(payload);
        value = false;
    }

    public Boolean getValue() {
        return value;
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("value", value);
        return props;
    }

    @Override
    public String toString() {
        return "Button{" +
                "value=" + value +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
