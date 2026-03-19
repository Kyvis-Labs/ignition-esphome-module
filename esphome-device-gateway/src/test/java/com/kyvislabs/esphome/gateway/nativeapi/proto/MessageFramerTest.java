package com.kyvislabs.esphome.gateway.nativeapi.proto;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MessageFramerTest {

    // ---- buildFrame + readFrame round-trip ----

    @Test
    void roundTrip_simplePayload() throws IOException {
        byte[] payload = {0x01, 0x02, 0x03};
        int messageType = 7;

        byte[] frame = MessageFramer.buildFrame(messageType, payload);
        var msg = MessageFramer.readFrame(new ByteArrayInputStream(frame));

        assertEquals(messageType, msg.messageType());
        assertArrayEquals(payload, msg.payload());
    }

    @Test
    void roundTrip_emptyPayload() throws IOException {
        byte[] frame = MessageFramer.buildFrame(1, new byte[0]);
        var msg = MessageFramer.readFrame(new ByteArrayInputStream(frame));

        assertEquals(1, msg.messageType());
        assertArrayEquals(new byte[0], msg.payload());
    }

    @Test
    void roundTrip_nullPayload() throws IOException {
        byte[] frame = MessageFramer.buildFrame(5, null);
        var msg = MessageFramer.readFrame(new ByteArrayInputStream(frame));

        assertEquals(5, msg.messageType());
        assertArrayEquals(new byte[0], msg.payload());
    }

    @Test
    void roundTrip_multiByteMessageType() throws IOException {
        // Message type 300 requires a multi-byte varint
        byte[] payload = {0x0A, 0x0B};
        byte[] frame = MessageFramer.buildFrame(300, payload);
        var msg = MessageFramer.readFrame(new ByteArrayInputStream(frame));

        assertEquals(300, msg.messageType());
        assertArrayEquals(payload, msg.payload());
    }

    // ---- buildFrame format verification ----

    @Test
    void buildFrame_startsWithPreamble() throws IOException {
        byte[] frame = MessageFramer.buildFrame(1, new byte[]{0x42});
        assertEquals(0x00, frame[0]);
    }

    @Test
    void buildFrame_lengthIsPayloadSizeOnly() throws IOException {
        byte[] payload = {0x01, 0x02, 0x03};
        byte[] frame = MessageFramer.buildFrame(7, payload);
        // frame[0] = 0x00 preamble
        // frame[1] = length varint (payload size = 3, single byte)
        assertEquals(3, frame[1]);
    }

    // ---- readFrame error cases ----

    @Test
    void readFrame_eof_throwsConnectionClosed() {
        var in = new ByteArrayInputStream(new byte[0]);
        var ex = assertThrows(IOException.class, () -> MessageFramer.readFrame(in));
        assertTrue(ex.getMessage().contains("Connection closed"));
    }

    @Test
    void readFrame_encryptionIndicator_throwsError() {
        var in = new ByteArrayInputStream(new byte[]{0x01});
        var ex = assertThrows(IOException.class, () -> MessageFramer.readFrame(in));
        assertTrue(ex.getMessage().contains("Noise encrypted"));
    }

    @Test
    void readFrame_invalidPreamble_throwsError() {
        var in = new ByteArrayInputStream(new byte[]{0x02});
        var ex = assertThrows(IOException.class, () -> MessageFramer.readFrame(in));
        assertTrue(ex.getMessage().contains("Invalid frame indicator"));
    }
}
