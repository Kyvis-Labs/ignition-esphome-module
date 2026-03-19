package com.kyvislabs.esphome.gateway.nativeapi.proto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ProtobufReader {

    private final byte[] data;
    private int pos;

    public ProtobufReader(byte[] data) {
        this.data = data;
        this.pos = 0;
    }

    public boolean hasRemaining() {
        return pos < data.length;
    }

    public int readTag() {
        return (int) readVarInt();
    }

    public int getFieldNumber(int tag) {
        return tag >>> 3;
    }

    public int getWireType(int tag) {
        return tag & 0x07;
    }

    public long readVarInt() {
        long result = 0;
        int shift = 0;
        while (pos < data.length) {
            byte b = data[pos++];
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
            if (shift >= 64) {
                throw new IllegalStateException("VarInt too long");
            }
        }
        throw new IllegalStateException("Unexpected end of data reading varint");
    }

    public int readFixed32() {
        if (pos + 4 > data.length) {
            throw new IllegalStateException("Unexpected end of data reading fixed32");
        }
        int value = ByteBuffer.wrap(data, pos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        pos += 4;
        return value;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readFixed32());
    }

    public long readFixed64() {
        if (pos + 8 > data.length) {
            throw new IllegalStateException("Unexpected end of data reading fixed64");
        }
        long value = ByteBuffer.wrap(data, pos, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        pos += 8;
        return value;
    }

    public double readDouble() {
        return Double.longBitsToDouble(readFixed64());
    }

    public String readString() {
        int length = (int) readVarInt();
        if (pos + length > data.length) {
            throw new IllegalStateException("Unexpected end of data reading string");
        }
        String result = new String(data, pos, length, StandardCharsets.UTF_8);
        pos += length;
        return result;
    }

    public byte[] readBytes() {
        int length = (int) readVarInt();
        if (pos + length > data.length) {
            throw new IllegalStateException("Unexpected end of data reading bytes");
        }
        byte[] result = new byte[length];
        System.arraycopy(data, pos, result, 0, length);
        pos += length;
        return result;
    }

    public boolean readBool() {
        return readVarInt() != 0;
    }

    public int readInt32() {
        return (int) readVarInt();
    }

    public int readEnum() {
        return (int) readVarInt();
    }

    public void skipField(int wireType) {
        switch (wireType) {
            case 0 -> readVarInt();
            case 1 -> {
                pos += 8;
            }
            case 2 -> {
                int length = (int) readVarInt();
                pos += length;
            }
            case 5 -> {
                pos += 4;
            }
            default -> throw new IllegalStateException("Unknown wire type: " + wireType);
        }
    }
}
