package com.kyvislabs.esphome.gateway.types;

import com.inductiveautomation.ignition.common.TypeUtilities;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Climate extends Base {
    String mode;
    Double currentTemperature;
    Double targetTemperature;
    String action;
    String fanMode;
    String swingMode;
    String supportedModes;
    String supportedFanModes;
    String supportedSwingModes;
    String supportedPresets;

    public Climate(HashMap<String, Object> payload) {
        super(payload);
        mode = TypeUtilities.toString(payload.getOrDefault("mode", null));
        currentTemperature = TypeUtilities.toDouble(payload.getOrDefault("current_temperature", null));
        targetTemperature = TypeUtilities.toDouble(payload.getOrDefault("target_temperature", null));
        action = TypeUtilities.toString(payload.getOrDefault("action", null));
        fanMode = TypeUtilities.toString(payload.getOrDefault("fan_mode", null));
        swingMode = TypeUtilities.toString(payload.getOrDefault("swing_mode", null));
        supportedModes = TypeUtilities.toString(payload.getOrDefault("supported_modes", null));
        supportedFanModes = TypeUtilities.toString(payload.getOrDefault("supported_fan_modes", null));
        supportedSwingModes = TypeUtilities.toString(payload.getOrDefault("supported_swing_modes", null));
        supportedPresets = TypeUtilities.toString(payload.getOrDefault("supported_presets", null));
    }

    @Override
    public LinkedHashMap<String, Object> getProperties() {
        var props = super.getProperties();
        props.put("mode", mode != null ? mode : "");
        props.put("current_temperature", currentTemperature != null ? currentTemperature : 0.0);
        props.put("target_temperature", targetTemperature != null ? targetTemperature : 0.0);
        props.put("action", action != null ? action : "");
        props.put("fan_mode", fanMode != null ? fanMode : "");
        props.put("swing_mode", swingMode != null ? swingMode : "");
        props.put("supported_modes", supportedModes != null ? supportedModes : "");
        props.put("supported_fan_modes", supportedFanModes != null ? supportedFanModes : "");
        props.put("supported_swing_modes", supportedSwingModes != null ? supportedSwingModes : "");
        props.put("supported_presets", supportedPresets != null ? supportedPresets : "");
        return props;
    }

    @Override
    public String toString() {
        return "Climate{" +
                "mode='" + mode + '\'' +
                ", currentTemperature=" + currentTemperature +
                ", targetTemperature=" + targetTemperature +
                ", action='" + action + '\'' +
                ", fanMode='" + fanMode + '\'' +
                ", swingMode='" + swingMode + '\'' +
                ", supportedModes='" + supportedModes + '\'' +
                ", supportedFanModes='" + supportedFanModes + '\'' +
                ", supportedSwingModes='" + supportedSwingModes + '\'' +
                ", supportedPresets='" + supportedPresets + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", entityCategory=" + entityCategory +
                '}';
    }
}
