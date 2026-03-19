package com.kyvislabs.esphome.gateway.nativeapi;

import java.util.HashMap;

public record EntityInfo(int key, String entityId, String domain, String name, HashMap<String, Object> metadata) {
}
