package com.kyvislabs.esphome.gateway.nativeapi.proto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EntityMessageParser {

    public static HashMap<String, Object> parseListEntities(int messageType, byte[] payload) {
        String domain = MessageTypes.domainForListEntities(messageType);
        if (domain == null) {
            return null;
        }

        var reader = new ProtobufReader(payload);
        var result = new HashMap<String, Object>();
        int key = 0;
        String objectId = "";
        String name = "";
        String icon = "";
        int entityCategory = 0;

        // For select options
        List<String> options = new ArrayList<>();
        // For climate modes/actions
        List<String> supportedModes = new ArrayList<>();
        List<String> supportedFanModes = new ArrayList<>();
        List<String> supportedSwingModes = new ArrayList<>();
        List<String> supportedPresets = new ArrayList<>();

        while (reader.hasRemaining()) {
            int tag = reader.readTag();
            int fieldNumber = reader.getFieldNumber(tag);
            int wireType = reader.getWireType(tag);

            switch (messageType) {
                case MessageTypes.LIST_ENTITIES_BINARY_SENSOR_RESPONSE ->
                    parseBinarySensorListEntity(fieldNumber, wireType, reader, result);
                case MessageTypes.LIST_ENTITIES_SENSOR_RESPONSE ->
                    parseSensorListEntity(fieldNumber, wireType, reader, result);
                case MessageTypes.LIST_ENTITIES_SWITCH_RESPONSE ->
                    parseSwitchListEntity(fieldNumber, wireType, reader, result);
                case MessageTypes.LIST_ENTITIES_TEXT_SENSOR_RESPONSE ->
                    parseTextSensorListEntity(fieldNumber, wireType, reader, result);
                case MessageTypes.LIST_ENTITIES_NUMBER_RESPONSE ->
                    parseNumberListEntity(fieldNumber, wireType, reader, result);
                case MessageTypes.LIST_ENTITIES_LIGHT_RESPONSE ->
                    parseLightListEntity(fieldNumber, wireType, reader, result);
                case MessageTypes.LIST_ENTITIES_BUTTON_RESPONSE ->
                    parseButtonListEntity(fieldNumber, wireType, reader, result);
                case MessageTypes.LIST_ENTITIES_COVER_RESPONSE ->
                    parseCoverListEntity(fieldNumber, wireType, reader, result);
                case MessageTypes.LIST_ENTITIES_FAN_RESPONSE ->
                    parseFanListEntity(fieldNumber, wireType, reader, result);
                case MessageTypes.LIST_ENTITIES_SELECT_RESPONSE ->
                    parseSelectListEntity(fieldNumber, wireType, reader, result, options);
                case MessageTypes.LIST_ENTITIES_CLIMATE_RESPONSE ->
                    parseClimateListEntity(fieldNumber, wireType, reader, result,
                        supportedModes, supportedFanModes, supportedSwingModes, supportedPresets);
                case MessageTypes.LIST_ENTITIES_LOCK_RESPONSE ->
                    parseLockListEntity(fieldNumber, wireType, reader, result);
                default -> reader.skipField(wireType);
            }

            // Common fields across all entity types
            if (result.containsKey("_objectId")) {
                objectId = (String) result.remove("_objectId");
            }
            if (result.containsKey("_key")) {
                key = (Integer) result.remove("_key");
            }
            if (result.containsKey("_name")) {
                name = (String) result.remove("_name");
            }
            if (result.containsKey("_icon")) {
                icon = (String) result.remove("_icon");
            }
            if (result.containsKey("_entityCategory")) {
                entityCategory = (Integer) result.remove("_entityCategory");
            }
        }

        // Build final entity ID: domain-object_id
        String entityId = domain + "-" + objectId;
        result.put("id", entityId);
        result.put("key", key);
        result.put("name", name);
        result.put("icon", icon);
        result.put("entity_category", entityCategory);
        result.put("domain", domain);

        if (!options.isEmpty()) {
            result.put("options", String.join(", ", options));
        }
        if (!supportedModes.isEmpty()) {
            result.put("supported_modes", String.join(", ", supportedModes));
        }
        if (!supportedFanModes.isEmpty()) {
            result.put("supported_fan_modes", String.join(", ", supportedFanModes));
        }
        if (!supportedSwingModes.isEmpty()) {
            result.put("supported_swing_modes", String.join(", ", supportedSwingModes));
        }
        if (!supportedPresets.isEmpty()) {
            result.put("supported_presets", String.join(", ", supportedPresets));
        }

        return result;
    }

    public static HashMap<String, Object> parseStateResponse(int messageType, byte[] payload) {
        String domain = MessageTypes.domainForStateResponse(messageType);
        if (domain == null) {
            return null;
        }

        var reader = new ProtobufReader(payload);
        var result = new HashMap<String, Object>();

        while (reader.hasRemaining()) {
            int tag = reader.readTag();
            int fieldNumber = reader.getFieldNumber(tag);
            int wireType = reader.getWireType(tag);

            switch (messageType) {
                case MessageTypes.BINARY_SENSOR_STATE_RESPONSE ->
                    parseBinarySensorState(fieldNumber, wireType, reader, result);
                case MessageTypes.SENSOR_STATE_RESPONSE ->
                    parseSensorState(fieldNumber, wireType, reader, result);
                case MessageTypes.SWITCH_STATE_RESPONSE ->
                    parseSwitchState(fieldNumber, wireType, reader, result);
                case MessageTypes.TEXT_SENSOR_STATE_RESPONSE ->
                    parseTextSensorState(fieldNumber, wireType, reader, result);
                case MessageTypes.NUMBER_STATE_RESPONSE ->
                    parseNumberState(fieldNumber, wireType, reader, result);
                case MessageTypes.LIGHT_STATE_RESPONSE ->
                    parseLightState(fieldNumber, wireType, reader, result);
                case MessageTypes.COVER_STATE_RESPONSE ->
                    parseCoverState(fieldNumber, wireType, reader, result);
                case MessageTypes.FAN_STATE_RESPONSE ->
                    parseFanState(fieldNumber, wireType, reader, result);
                case MessageTypes.SELECT_STATE_RESPONSE ->
                    parseSelectState(fieldNumber, wireType, reader, result);
                case MessageTypes.CLIMATE_STATE_RESPONSE ->
                    parseClimateState(fieldNumber, wireType, reader, result);
                case MessageTypes.LOCK_STATE_RESPONSE ->
                    parseLockState(fieldNumber, wireType, reader, result);
                default -> reader.skipField(wireType);
            }
        }

        result.put("domain", domain);
        return result;
    }

    public static HashMap<String, Object> parseDeviceInfo(byte[] payload) {
        var reader = new ProtobufReader(payload);
        var result = new HashMap<String, Object>();

        while (reader.hasRemaining()) {
            int tag = reader.readTag();
            int fieldNumber = reader.getFieldNumber(tag);
            int wireType = reader.getWireType(tag);

            switch (fieldNumber) {
                case 1 -> result.put("uses_password", reader.readBool());
                case 2 -> result.put("name", reader.readString());
                case 3 -> result.put("mac_address", reader.readString());
                case 4 -> result.put("esphome_version", reader.readString());
                case 5 -> result.put("compilation_time", reader.readString());
                case 6 -> result.put("model", reader.readString());
                case 7 -> result.put("has_deep_sleep", reader.readBool());
                case 8 -> result.put("project_name", reader.readString());
                case 9 -> result.put("project_version", reader.readString());
                case 10 -> result.put("webserver_port", reader.readInt32());
                case 12 -> result.put("manufacturer", reader.readString());
                case 13 -> result.put("friendly_name", reader.readString());
                case 15 -> result.put("suggested_area", reader.readString());
                default -> reader.skipField(wireType);
            }
        }

        return result;
    }

    // ---- ListEntities parsers ----

    private static void parseBinarySensorListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("device_class", r.readString());
            case 6 -> m.put("is_status_binary_sensor", r.readBool());
            case 7 -> m.put("disabled_by_default", r.readBool());
            case 8 -> m.put("_icon", r.readString());
            case 9 -> m.put("_entityCategory", r.readEnum());
            default -> r.skipField(wireType);
        }
    }

    private static void parseSensorListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("_icon", r.readString());
            case 6 -> m.put("uom", r.readString());
            case 7 -> m.put("accuracy_decimals", r.readInt32());
            case 8 -> m.put("force_update", r.readBool());
            case 9 -> m.put("device_class", r.readString());
            case 10 -> m.put("state_class", r.readEnum());
            case 11 -> m.put("disabled_by_default", r.readBool());
            case 12 -> m.put("_entityCategory", r.readEnum());
            default -> r.skipField(wireType);
        }
    }

    private static void parseSwitchListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("_icon", r.readString());
            case 6 -> m.put("assumed_state", r.readBool() ? "true" : "false");
            case 7 -> m.put("disabled_by_default", r.readBool());
            case 8 -> m.put("_entityCategory", r.readEnum());
            case 9 -> m.put("device_class", r.readString());
            default -> r.skipField(wireType);
        }
    }

    private static void parseTextSensorListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("_icon", r.readString());
            case 6 -> m.put("disabled_by_default", r.readBool());
            case 7 -> m.put("_entityCategory", r.readEnum());
            case 8 -> m.put("device_class", r.readString());
            default -> r.skipField(wireType);
        }
    }

    private static void parseNumberListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("_icon", r.readString());
            case 6 -> m.put("min_value", (double) r.readFloat());
            case 7 -> m.put("max_value", (double) r.readFloat());
            case 8 -> m.put("step", (double) r.readFloat());
            case 9 -> m.put("disabled_by_default", r.readBool());
            case 10 -> m.put("_entityCategory", r.readEnum());
            case 11 -> m.put("uom", r.readString());
            case 12 -> m.put("mode", r.readEnum());
            case 13 -> m.put("device_class", r.readString());
            default -> r.skipField(wireType);
        }
    }

    private static void parseLightListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            // fields 5-12 are supported color modes, effects, etc.
            case 13 -> m.put("disabled_by_default", r.readBool());
            case 14 -> m.put("_icon", r.readString());
            case 15 -> m.put("_entityCategory", r.readEnum());
            default -> {
                if (field == 11 && wireType == 2) {
                    // effects list (repeated string, field 11)
                    String effect = r.readString();
                    @SuppressWarnings("unchecked")
                    List<String> effects = (List<String>) m.computeIfAbsent("_effects", k -> new ArrayList<String>());
                    effects.add(effect);
                } else {
                    r.skipField(wireType);
                }
            }
        }
    }

    private static void parseButtonListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("_icon", r.readString());
            case 6 -> m.put("disabled_by_default", r.readBool());
            case 7 -> m.put("_entityCategory", r.readEnum());
            case 8 -> m.put("device_class", r.readString());
            default -> r.skipField(wireType);
        }
    }

    private static void parseCoverListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("assumed_state", r.readBool());
            case 6 -> m.put("supports_position", r.readBool());
            case 7 -> m.put("supports_tilt", r.readBool());
            case 8 -> m.put("device_class", r.readString());
            case 9 -> m.put("disabled_by_default", r.readBool());
            case 10 -> m.put("_icon", r.readString());
            case 11 -> m.put("_entityCategory", r.readEnum());
            default -> r.skipField(wireType);
        }
    }

    private static void parseFanListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("supports_oscillation", r.readBool());
            case 6 -> m.put("supports_speed", r.readBool());
            case 7 -> m.put("supports_direction", r.readBool());
            case 8 -> m.put("supported_speed_count", r.readInt32());
            case 9 -> m.put("disabled_by_default", r.readBool());
            case 10 -> m.put("_icon", r.readString());
            case 11 -> m.put("_entityCategory", r.readEnum());
            default -> {
                if (field == 12 && wireType == 2) {
                    // preset_modes (repeated string)
                    String mode = r.readString();
                    @SuppressWarnings("unchecked")
                    List<String> modes = (List<String>) m.computeIfAbsent("_presetModes", k -> new ArrayList<String>());
                    modes.add(mode);
                } else {
                    r.skipField(wireType);
                }
            }
        }
    }

    private static void parseSelectListEntity(int field, int wireType, ProtobufReader r,
            HashMap<String, Object> m, List<String> options) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("_icon", r.readString());
            case 6 -> options.add(r.readString()); // repeated
            case 7 -> m.put("disabled_by_default", r.readBool());
            case 8 -> m.put("_entityCategory", r.readEnum());
            default -> r.skipField(wireType);
        }
    }

    private static void parseClimateListEntity(int field, int wireType, ProtobufReader r,
            HashMap<String, Object> m, List<String> modes, List<String> fanModes,
            List<String> swingModes, List<String> presets) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("supports_current_temperature", r.readBool());
            case 6 -> m.put("supports_two_point_target_temperature", r.readBool());
            case 7 -> modes.add(climateMode(r.readEnum())); // repeated
            case 8 -> m.put("visual_min_temperature", (double) r.readFloat());
            case 9 -> m.put("visual_max_temperature", (double) r.readFloat());
            case 10 -> m.put("visual_target_temperature_step", (double) r.readFloat());
            case 11 -> m.put("visual_current_temperature_step", (double) r.readFloat());
            case 12 -> fanModes.add(climateFanMode(r.readEnum())); // repeated
            case 13 -> swingModes.add(climateSwingMode(r.readEnum())); // repeated
            case 14 -> m.put("supports_action", r.readBool());
            case 15 -> presets.add(climatePreset(r.readEnum())); // repeated
            case 16 -> m.put("disabled_by_default", r.readBool());
            case 17 -> m.put("_icon", r.readString());
            case 18 -> m.put("_entityCategory", r.readEnum());
            default -> r.skipField(wireType);
        }
    }

    private static void parseLockListEntity(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("_objectId", r.readString());
            case 2 -> m.put("_key", r.readFixed32());
            case 3 -> m.put("_name", r.readString());
            case 4 -> m.put("unique_id", r.readString());
            case 5 -> m.put("_icon", r.readString());
            case 6 -> m.put("disabled_by_default", r.readBool());
            case 7 -> m.put("_entityCategory", r.readEnum());
            case 8 -> m.put("assumed_state", r.readBool());
            case 9 -> m.put("supports_open", r.readBool());
            case 10 -> m.put("requires_code", r.readBool());
            default -> r.skipField(wireType);
        }
    }

    // ---- State response parsers ----

    private static void parseBinarySensorState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> {
                boolean val = r.readBool();
                m.put("value", val);
                m.put("state", val ? "ON" : "OFF");
            }
            case 3 -> m.put("missing_state", r.readBool());
            default -> r.skipField(wireType);
        }
    }

    private static void parseSensorState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> {
                float val = r.readFloat();
                m.put("value", (double) val);
                m.put("state", String.valueOf(val));
            }
            case 3 -> m.put("missing_state", r.readBool());
            default -> r.skipField(wireType);
        }
    }

    private static void parseSwitchState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> {
                boolean val = r.readBool();
                m.put("value", val);
                m.put("state", val ? "ON" : "OFF");
            }
            default -> r.skipField(wireType);
        }
    }

    private static void parseTextSensorState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> {
                String val = r.readString();
                m.put("value", val);
                m.put("state", val);
            }
            case 3 -> m.put("missing_state", r.readBool());
            default -> r.skipField(wireType);
        }
    }

    private static void parseNumberState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> {
                float val = r.readFloat();
                m.put("value", (double) val);
                m.put("state", String.valueOf(val));
            }
            case 3 -> m.put("missing_state", r.readBool());
            default -> r.skipField(wireType);
        }
    }

    private static void parseLightState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> {
                boolean val = r.readBool();
                m.put("state", val ? "ON" : "OFF");
            }
            case 3 -> m.put("brightness", (double) r.readFloat());
            case 4 -> m.put("color_r", (double) r.readFloat());
            case 5 -> m.put("color_g", (double) r.readFloat());
            case 6 -> m.put("color_b", (double) r.readFloat());
            case 7 -> m.put("white", (double) r.readFloat());
            case 8 -> m.put("color_temperature", (double) r.readFloat());
            case 9 -> m.put("effect", r.readString());
            case 10 -> m.put("color_brightness", (double) r.readFloat());
            case 11 -> m.put("color_mode", lightColorMode(r.readEnum()));
            case 12 -> m.put("cold_white", (double) r.readFloat());
            case 13 -> m.put("warm_white", (double) r.readFloat());
            default -> r.skipField(wireType);
        }
    }

    private static void parseCoverState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> {
                // legacy_state: 0=OPEN, 1=CLOSED
                int val = r.readEnum();
                if (!m.containsKey("current_operation")) {
                    m.put("state", val == 0 ? "OPEN" : "CLOSED");
                }
            }
            case 3 -> m.put("position", (double) r.readFloat());
            case 4 -> m.put("tilt", (double) r.readFloat());
            case 5 -> m.put("current_operation", coverOperation(r.readEnum()));
            default -> r.skipField(wireType);
        }
    }

    private static void parseFanState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> {
                boolean val = r.readBool();
                m.put("value", val);
                m.put("state", val ? "ON" : "OFF");
            }
            case 3 -> m.put("oscillating", r.readBool());
            case 4 -> m.put("speed", fanSpeed(r.readEnum())); // legacy speed
            case 5 -> m.put("direction", fanDirection(r.readEnum()));
            case 6 -> m.put("speed_level", r.readInt32());
            case 7 -> m.put("preset_mode", r.readString());
            default -> r.skipField(wireType);
        }
    }

    private static void parseSelectState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> {
                String val = r.readString();
                m.put("state", val);
                m.put("value", val);
            }
            case 3 -> m.put("missing_state", r.readBool());
            default -> r.skipField(wireType);
        }
    }

    private static void parseClimateState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> m.put("mode", climateMode(r.readEnum()));
            case 3 -> m.put("current_temperature", (double) r.readFloat());
            case 4 -> m.put("target_temperature", (double) r.readFloat());
            case 5 -> m.put("target_temperature_low", (double) r.readFloat());
            case 6 -> m.put("target_temperature_high", (double) r.readFloat());
            case 7 -> { /* legacy away */ r.readBool(); }
            case 8 -> m.put("action", climateAction(r.readEnum()));
            case 9 -> m.put("fan_mode", climateFanMode(r.readEnum()));
            case 10 -> m.put("swing_mode", climateSwingMode(r.readEnum()));
            case 12 -> m.put("preset", climatePreset(r.readEnum()));
            case 11 -> m.put("custom_fan_mode", r.readString());
            case 13 -> m.put("custom_preset", r.readString());
            default -> r.skipField(wireType);
        }
    }

    private static void parseLockState(int field, int wireType, ProtobufReader r, HashMap<String, Object> m) {
        switch (field) {
            case 1 -> m.put("key", r.readFixed32());
            case 2 -> m.put("state", lockState(r.readEnum()));
            default -> r.skipField(wireType);
        }
    }

    // ---- Enum converters ----

    private static String coverOperation(int value) {
        return switch (value) {
            case 0 -> "IDLE";
            case 1 -> "OPENING";
            case 2 -> "CLOSING";
            default -> "UNKNOWN";
        };
    }

    private static String fanSpeed(int value) {
        return switch (value) {
            case 0 -> "LOW";
            case 1 -> "MEDIUM";
            case 2 -> "HIGH";
            default -> "UNKNOWN";
        };
    }

    private static String fanDirection(int value) {
        return switch (value) {
            case 0 -> "FORWARD";
            case 1 -> "REVERSE";
            default -> "UNKNOWN";
        };
    }

    private static String climateMode(int value) {
        return switch (value) {
            case 0 -> "OFF";
            case 1 -> "HEAT_COOL";
            case 2 -> "COOL";
            case 3 -> "HEAT";
            case 4 -> "FAN_ONLY";
            case 5 -> "DRY";
            case 6 -> "AUTO";
            default -> "UNKNOWN";
        };
    }

    private static String climateAction(int value) {
        return switch (value) {
            case 0 -> "OFF";
            case 1 -> "PREHEATING";
            case 2 -> "COOLING";
            case 3 -> "HEATING";
            case 4 -> "DRYING";
            case 5 -> "FAN";
            case 6 -> "IDLE";
            default -> "UNKNOWN";
        };
    }

    private static String climateFanMode(int value) {
        return switch (value) {
            case 0 -> "ON";
            case 1 -> "OFF";
            case 2 -> "AUTO";
            case 3 -> "LOW";
            case 4 -> "MEDIUM";
            case 5 -> "HIGH";
            case 6 -> "MIDDLE";
            case 7 -> "FOCUS";
            case 8 -> "DIFFUSE";
            case 9 -> "QUIET";
            default -> "UNKNOWN";
        };
    }

    private static String climateSwingMode(int value) {
        return switch (value) {
            case 0 -> "OFF";
            case 1 -> "BOTH";
            case 2 -> "VERTICAL";
            case 3 -> "HORIZONTAL";
            default -> "UNKNOWN";
        };
    }

    private static String climatePreset(int value) {
        return switch (value) {
            case 0 -> "NONE";
            case 1 -> "HOME";
            case 2 -> "AWAY";
            case 3 -> "BOOST";
            case 4 -> "COMFORT";
            case 5 -> "ECO";
            case 6 -> "SLEEP";
            case 7 -> "ACTIVITY";
            default -> "UNKNOWN";
        };
    }

    private static String lightColorMode(int value) {
        return switch (value) {
            case 0 -> "UNKNOWN";
            case 1 -> "ON_OFF";
            case 2 -> "BRIGHTNESS";
            case 7 -> "WHITE";
            case 11 -> "COLOR_TEMPERATURE";
            case 19 -> "COLD_WARM_WHITE";
            case 35 -> "RGB";
            case 39 -> "RGB_WHITE";
            case 47 -> "RGB_COLOR_TEMPERATURE";
            case 51 -> "RGB_COLD_WARM_WHITE";
            default -> "UNKNOWN";
        };
    }

    private static String lockState(int value) {
        return switch (value) {
            case 0 -> "NONE";
            case 1 -> "LOCKED";
            case 2 -> "UNLOCKED";
            case 3 -> "JAMMED";
            case 4 -> "LOCKING";
            case 5 -> "UNLOCKING";
            default -> "UNKNOWN";
        };
    }
}
