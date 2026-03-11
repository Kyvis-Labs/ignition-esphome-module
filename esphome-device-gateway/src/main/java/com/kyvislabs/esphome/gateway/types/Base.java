package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Base {
    String id;
    String name;
    String icon;
    Integer entityCategory;

    public Base(HashMap<String,Object> payload){

        id = String.valueOf(payload.get("id"));
        name = TypeUtilities.toString(payload.getOrDefault("name",null));
        icon = TypeUtilities.toString(payload.getOrDefault("icon",null));
        entityCategory = TypeUtilities.toInteger(payload.getOrDefault("entity_category",null));
    }

    public LinkedHashMap<String, Object> getProperties() {
        var props = new LinkedHashMap<String, Object>();
        props.put("icon", icon != null ? icon : "");
        return props;
    }
}
