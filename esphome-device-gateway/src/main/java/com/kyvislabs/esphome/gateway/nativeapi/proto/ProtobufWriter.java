package com.kyvislabs.esphome.gateway.nativeapi.proto;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ProtobufWriter {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public void writeVarInt(long value) {
        while ((value & ~0x7FL) != 0) {
            buffer.write((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        buffer.write((byte) (value & 0x7F));
    }

    private void writeTag(int fieldNumber, int wireType) {
        writeVarInt((fieldNumber << 3) | wireType);
    }

    public void writeVarIntField(int fieldNumber, long value) {
        writeTag(fieldNumber, 0);
        writeVarInt(value);
    }

    public void writeBoolField(int fieldNumber, boolean value) {
        writeVarIntField(fieldNumber, value ? 1 : 0);
    }

    public void writeStringField(int fieldNumber, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeTag(fieldNumber, 2);
        writeVarInt(bytes.length);
        buffer.write(bytes, 0, bytes.length);
    }

    public void writeFixed32Field(int fieldNumber, int value) {
        writeTag(fieldNumber, 5);
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
        buffer.write(bytes, 0, bytes.length);
    }

    public void writeFloatField(int fieldNumber, float value) {
        writeFixed32Field(fieldNumber, Float.floatToIntBits(value));
    }

    public byte[] toByteArray() {
        return buffer.toByteArray();
    }
}
