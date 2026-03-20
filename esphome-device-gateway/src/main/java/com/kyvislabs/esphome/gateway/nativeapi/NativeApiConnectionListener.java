package com.kyvislabs.esphome.gateway.nativeapi;

import java.util.HashMap;

public interface NativeApiConnectionListener {

    void onConnected(HashMap<String, Object> deviceInfo);

    void onDisconnected(String reason);

    void onEntityDiscovered(EntityInfo entity);

    void onEntitiesDiscoveryDone();

    void onStateUpdate(int key, String domain, HashMap<String, Object> stateData);

    void onError(String message, Throwable cause);

    void onMessageStats(long receivedCount, int lastReceivedSize, long sentCount, long receivedBytes, long sentBytes);
}
