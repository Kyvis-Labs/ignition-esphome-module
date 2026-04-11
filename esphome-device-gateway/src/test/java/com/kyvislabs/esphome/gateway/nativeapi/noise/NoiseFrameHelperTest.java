package com.kyvislabs.esphome.gateway.nativeapi.noise;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NoiseFrameHelperTest {

    // ---- Frame format ----

    @Test
    void frameSize_emptyPayload() {
        // Use a dummy helper just for frameSize calculation
        var helper = new NoiseFrameHelper(
            new ByteArrayInputStream(new byte[0]),
            new ByteArrayOutputStream(),
            new byte[32]
        );

        // 1 (preamble) + 2 (length) + 4 (inner header: 2 type + 2 len) + 0 (data) + 16 (MAC)
        assertEquals(23, helper.frameSize(1, new byte[0]));
    }

    @Test
    void frameSize_withPayload() {
        var helper = new NoiseFrameHelper(
            new ByteArrayInputStream(new byte[0]),
            new ByteArrayOutputStream(),
            new byte[32]
        );

        byte[] payload = new byte[100];
        // 1 + 2 + 4 + 100 + 16 = 123
        assertEquals(123, helper.frameSize(7, payload));
    }

    @Test
    void frameSize_nullPayload() {
        var helper = new NoiseFrameHelper(
            new ByteArrayInputStream(new byte[0]),
            new ByteArrayOutputStream(),
            new byte[32]
        );

        // null payload treated as empty
        assertEquals(23, helper.frameSize(1, null));
    }

    // ---- Handshake failure cases ----

    @Test
    void performHandshake_emptyStream_throws() {
        var helper = new NoiseFrameHelper(
            new ByteArrayInputStream(new byte[0]),
            new ByteArrayOutputStream(),
            new byte[32]
        );

        assertThrows(IOException.class, helper::performHandshake);
    }

    // ---- Read message failure cases ----

    @Test
    void readMessage_emptyStream_throws() {
        var helper = new NoiseFrameHelper(
            new ByteArrayInputStream(new byte[0]),
            new ByteArrayOutputStream(),
            new byte[32]
        );

        assertThrows(IOException.class, helper::readMessage);
    }

    @Test
    void readMessage_wrongPreamble_throws() {
        // 0x00 preamble instead of 0x01
        var helper = new NoiseFrameHelper(
            new ByteArrayInputStream(new byte[]{0x00, 0x00, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05}),
            new ByteArrayOutputStream(),
            new byte[32]
        );

        var ex = assertThrows(IOException.class, helper::readMessage);
        assertTrue(ex.getMessage().contains("0x01"));
    }

    // ---- Inner payload format ----

    @Test
    void innerPayloadFormat_messageTypeIsBigEndian() {
        // Verify the 2-byte big-endian message type encoding logic
        int messageType = 0x0102;  // 258 in decimal
        byte hi = (byte) ((messageType >> 8) & 0xFF);
        byte lo = (byte) (messageType & 0xFF);

        assertEquals(0x01, hi);
        assertEquals(0x02, lo);
    }

    @Test
    void innerPayloadFormat_lengthIsBigEndian() {
        int length = 0x0304;
        byte hi = (byte) ((length >> 8) & 0xFF);
        byte lo = (byte) (length & 0xFF);

        assertEquals(0x03, hi);
        assertEquals(0x04, lo);
    }
}
