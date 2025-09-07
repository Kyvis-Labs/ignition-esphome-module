package com.kyvislabs.esphome.gateway.types;

import java.util.HashMap;

public class Button extends Base{
    String state;
    Boolean value;

    public Button(HashMap<String, Object> payload) {
        super(payload);
    }

    @Override
    public String toString() {
        return "Button{" +
                "state='" + state + '\'' +
                ", value=" + value +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
