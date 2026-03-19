package com.kyvislabs.esphome.gateway.nativeapi.proto;

public final class MessageTypes {

    private MessageTypes() {}

    // Connection
    public static final int HELLO_REQUEST = 1;
    public static final int HELLO_RESPONSE = 2;
    public static final int CONNECT_REQUEST = 3;
    public static final int CONNECT_RESPONSE = 4;
    public static final int DISCONNECT_REQUEST = 5;
    public static final int DISCONNECT_RESPONSE = 6;
    public static final int PING_REQUEST = 7;
    public static final int PING_RESPONSE = 8;
    public static final int DEVICE_INFO_REQUEST = 9;
    public static final int DEVICE_INFO_RESPONSE = 10;

    // List entities
    public static final int LIST_ENTITIES_REQUEST = 11;
    public static final int LIST_ENTITIES_BINARY_SENSOR_RESPONSE = 12;
    public static final int LIST_ENTITIES_COVER_RESPONSE = 13;
    public static final int LIST_ENTITIES_FAN_RESPONSE = 14;
    public static final int LIST_ENTITIES_LIGHT_RESPONSE = 15;
    public static final int LIST_ENTITIES_SENSOR_RESPONSE = 16;
    public static final int LIST_ENTITIES_SWITCH_RESPONSE = 17;
    public static final int LIST_ENTITIES_TEXT_SENSOR_RESPONSE = 18;
    public static final int LIST_ENTITIES_DONE_RESPONSE = 19;

    // Subscribe states
    public static final int SUBSCRIBE_STATES_REQUEST = 20;
    public static final int BINARY_SENSOR_STATE_RESPONSE = 21;
    public static final int COVER_STATE_RESPONSE = 22;
    public static final int FAN_STATE_RESPONSE = 23;
    public static final int LIGHT_STATE_RESPONSE = 24;
    public static final int SENSOR_STATE_RESPONSE = 25;
    public static final int SWITCH_STATE_RESPONSE = 26;
    public static final int TEXT_SENSOR_STATE_RESPONSE = 27;

    // Commands
    public static final int COVER_COMMAND_REQUEST = 30;
    public static final int FAN_COMMAND_REQUEST = 31;
    public static final int LIGHT_COMMAND_REQUEST = 32;
    public static final int SWITCH_COMMAND_REQUEST = 33;

    // Additional entity types
    public static final int LIST_ENTITIES_CLIMATE_RESPONSE = 46;
    public static final int CLIMATE_STATE_RESPONSE = 47;
    public static final int CLIMATE_COMMAND_REQUEST = 48;
    public static final int LIST_ENTITIES_NUMBER_RESPONSE = 49;
    public static final int NUMBER_STATE_RESPONSE = 50;
    public static final int NUMBER_COMMAND_REQUEST = 51;
    public static final int LIST_ENTITIES_SELECT_RESPONSE = 52;
    public static final int SELECT_STATE_RESPONSE = 53;
    public static final int SELECT_COMMAND_REQUEST = 54;
    public static final int LIST_ENTITIES_LOCK_RESPONSE = 58;
    public static final int LOCK_STATE_RESPONSE = 59;
    public static final int LOCK_COMMAND_REQUEST = 60;
    public static final int LIST_ENTITIES_BUTTON_RESPONSE = 61;
    public static final int BUTTON_COMMAND_REQUEST = 62;

    public static String domainForListEntities(int messageType) {
        return switch (messageType) {
            case LIST_ENTITIES_BINARY_SENSOR_RESPONSE -> "binary_sensor";
            case LIST_ENTITIES_COVER_RESPONSE -> "cover";
            case LIST_ENTITIES_FAN_RESPONSE -> "fan";
            case LIST_ENTITIES_LIGHT_RESPONSE -> "light";
            case LIST_ENTITIES_SENSOR_RESPONSE -> "sensor";
            case LIST_ENTITIES_SWITCH_RESPONSE -> "switch";
            case LIST_ENTITIES_TEXT_SENSOR_RESPONSE -> "text_sensor";
            case LIST_ENTITIES_NUMBER_RESPONSE -> "number";
            case LIST_ENTITIES_SELECT_RESPONSE -> "select";
            case LIST_ENTITIES_LOCK_RESPONSE -> "lock";
            case LIST_ENTITIES_BUTTON_RESPONSE -> "button";
            case LIST_ENTITIES_CLIMATE_RESPONSE -> "climate";
            default -> null;
        };
    }

    public static String domainForStateResponse(int messageType) {
        return switch (messageType) {
            case BINARY_SENSOR_STATE_RESPONSE -> "binary_sensor";
            case COVER_STATE_RESPONSE -> "cover";
            case FAN_STATE_RESPONSE -> "fan";
            case LIGHT_STATE_RESPONSE -> "light";
            case SENSOR_STATE_RESPONSE -> "sensor";
            case SWITCH_STATE_RESPONSE -> "switch";
            case TEXT_SENSOR_STATE_RESPONSE -> "text_sensor";
            case NUMBER_STATE_RESPONSE -> "number";
            case SELECT_STATE_RESPONSE -> "select";
            case CLIMATE_STATE_RESPONSE -> "climate";
            case LOCK_STATE_RESPONSE -> "lock";
            default -> null;
        };
    }

    public static boolean isListEntitiesResponse(int messageType) {
        return domainForListEntities(messageType) != null;
    }

    public static boolean isStateResponse(int messageType) {
        return domainForStateResponse(messageType) != null;
    }
}
