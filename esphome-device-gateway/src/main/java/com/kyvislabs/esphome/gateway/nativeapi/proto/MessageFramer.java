package com.kyvislabs.esphome.gateway.nativeapi.proto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MessageFramer {

    public record RawMessage(int messageType, byte[] payload) {}

    public static RawMessage readFrame(InputStream in) throws IOException {
        // Read preamble byte — 0x00 for plaintext, 0x01 for Noise encryption
        int preamble = in.read();
        if (preamble == -1) {
            throw new IOException("Connection closed");
        }
        if (preamble == 0x01) {
            throw new IOException(
                "Device is using Noise encrypted API protocol (indicator 0x01). " +
                "Configure the encryption key in the device settings, or remove the " +
                "'encryption:' block from the device's 'api:' configuration and reflash.");
        }
        if (preamble != 0x00) {
            throw new IOException(
                "Invalid frame indicator: 0x" + Integer.toHexString(preamble) +
                " (expected 0x00 for plaintext Native API)");
        }

        // Read length varint — this is the protobuf payload size only
        int length = readVarIntFromStream(in);

        // Read message type varint
        int messageType = readVarIntFromStream(in);

        // Read exactly 'length' bytes of protobuf payload
        byte[] payload;
        if (length > 0) {
            payload = in.readNBytes(length);
            if (payload.length != length) {
                throw new IOException("Unexpected end of stream reading frame data");
            }
        } else {
            payload = new byte[0];
        }

        return new RawMessage(messageType, payload);
    }

    public static void writeFrame(OutputStream out, int messageType, byte[] payload) throws IOException {
        out.write(buildFrame(messageType, payload));
        out.flush();
    }

    public static byte[] buildFrame(int messageType, byte[] payload) throws IOException {
        var buf = new ByteArrayOutputStream();

        // Length is the protobuf payload size only (does NOT include msg_type varint)
        int length = payload != null ? payload.length : 0;

        buf.write(0x00);
        writeVarIntToBytes(buf, length);
        writeVarIntToBytes(buf, messageType);

        if (payload != null && payload.length > 0) {
            buf.write(payload);
        }

        return buf.toByteArray();
    }

    private static int readVarIntFromStream(InputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        while (true) {
            int b = in.read();
            if (b == -1) {
                throw new IOException("Connection closed while reading varint");
            }
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
            if (shift >= 32) {
                throw new IOException("VarInt too long");
            }
        }
    }

    private static void writeVarIntToBytes(OutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.write((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.write((byte) (value & 0x7F));
    }
}
