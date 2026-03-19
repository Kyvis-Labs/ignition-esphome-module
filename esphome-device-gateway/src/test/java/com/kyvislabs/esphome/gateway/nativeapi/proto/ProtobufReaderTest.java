package com.kyvislabs.esphome.gateway.nativeapi.proto;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProtobufReaderTest {

    // ---- readVarInt ----

    @Test
    void readVarInt_zero() {
        var reader = new ProtobufReader(new byte[]{0x00});
        assertEquals(0, reader.readVarInt());
    }

    @Test
    void readVarInt_one() {
        var reader = new ProtobufReader(new byte[]{0x01});
        assertEquals(1, reader.readVarInt());
    }

    @Test
    void readVarInt_127() {
        var reader = new ProtobufReader(new byte[]{0x7F});
        assertEquals(127, reader.readVarInt());
    }

    @Test
    void readVarInt_128_multiByteEncoding() {
        // 128 = 0x80 → [0x80, 0x01]
        var reader = new ProtobufReader(new byte[]{(byte) 0x80, 0x01});
        assertEquals(128, reader.readVarInt());
    }

    @Test
    void readVarInt_300_multiByteEncoding() {
        // 300 = 0x12C → low 7 bits = 0x2C | 0x80 = 0xAC, high bits = 0x02
        var reader = new ProtobufReader(new byte[]{(byte) 0xAC, 0x02});
        assertEquals(300, reader.readVarInt());
    }

    @Test
    void readVarInt_16384_threeByteEncoding() {
        // 16384 = 0x4000 → [0x80, 0x80, 0x01]
        var reader = new ProtobufReader(new byte[]{(byte) 0x80, (byte) 0x80, 0x01});
        assertEquals(16384, reader.readVarInt());
    }

    @Test
    void readVarInt_largeValue() {
        // 2^31 - 1 = 2147483647
        // varint encoding of 2147483647: [0xFF, 0xFF, 0xFF, 0xFF, 0x07]
        var reader = new ProtobufReader(new byte[]{
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x07
        });
        assertEquals(Integer.MAX_VALUE, reader.readVarInt());
    }

    // ---- readFloat ----

    @Test
    void readFloat_one() {
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(Float.floatToIntBits(1.0f)).array();
        var reader = new ProtobufReader(bytes);
        assertEquals(1.0f, reader.readFloat());
    }

    @Test
    void readFloat_zero() {
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(Float.floatToIntBits(0.0f)).array();
        var reader = new ProtobufReader(bytes);
        assertEquals(0.0f, reader.readFloat());
    }

    @Test
    void readFloat_negativeOne() {
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(Float.floatToIntBits(-1.0f)).array();
        var reader = new ProtobufReader(bytes);
        assertEquals(-1.0f, reader.readFloat());
    }

    // ---- readFixed32 ----

    @Test
    void readFixed32_littleEndian() {
        // 0x04030201 in little-endian: [0x01, 0x02, 0x03, 0x04]
        var reader = new ProtobufReader(new byte[]{0x01, 0x02, 0x03, 0x04});
        assertEquals(0x04030201, reader.readFixed32());
    }

    // ---- readString ----

    @Test
    void readString_simple() {
        byte[] strBytes = "hello".getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[1 + strBytes.length];
        data[0] = (byte) strBytes.length;
        System.arraycopy(strBytes, 0, data, 1, strBytes.length);
        var reader = new ProtobufReader(data);
        assertEquals("hello", reader.readString());
    }

    @Test
    void readString_empty() {
        var reader = new ProtobufReader(new byte[]{0x00});
        assertEquals("", reader.readString());
    }

    @Test
    void readString_utf8() {
        byte[] strBytes = "\u00e9\u00fc".getBytes(StandardCharsets.UTF_8); // éü
        byte[] data = new byte[1 + strBytes.length];
        data[0] = (byte) strBytes.length;
        System.arraycopy(strBytes, 0, data, 1, strBytes.length);
        var reader = new ProtobufReader(data);
        assertEquals("\u00e9\u00fc", reader.readString());
    }

    // ---- readBool ----

    @Test
    void readBool_true() {
        var reader = new ProtobufReader(new byte[]{0x01});
        assertTrue(reader.readBool());
    }

    @Test
    void readBool_false() {
        var reader = new ProtobufReader(new byte[]{0x00});
        assertFalse(reader.readBool());
    }

    // ---- readEnum (same as varint) ----

    @Test
    void readEnum_value() {
        var reader = new ProtobufReader(new byte[]{0x03});
        assertEquals(3, reader.readEnum());
    }

    // ---- Tag parsing: getFieldNumber and getWireType ----

    @Test
    void tagParsing_fieldNumberAndWireType() {
        // tag = (fieldNumber << 3) | wireType
        // field 5, wire type 2 → (5 << 3) | 2 = 42
        var reader = new ProtobufReader(new byte[]{42});
        int tag = reader.readTag();
        assertEquals(5, reader.getFieldNumber(tag));
        assertEquals(2, reader.getWireType(tag));
    }

    @Test
    void tagParsing_field1_wireType0() {
        // field 1, wire type 0 → (1 << 3) | 0 = 8
        var reader = new ProtobufReader(new byte[]{8});
        int tag = reader.readTag();
        assertEquals(1, reader.getFieldNumber(tag));
        assertEquals(0, reader.getWireType(tag));
    }

    // ---- skipField ----

    @Test
    void skipField_varint() {
        // varint value 300 = [0xAC, 0x02], then a readable byte 0x42
        var reader = new ProtobufReader(new byte[]{(byte) 0xAC, 0x02, 0x42});
        reader.skipField(0); // wire type 0 = varint
        assertEquals(0x42, reader.readVarInt());
    }

    @Test
    void skipField_fixed64() {
        byte[] data = new byte[9];
        // 8 bytes for fixed64, then 0x01
        data[8] = 0x01;
        var reader = new ProtobufReader(data);
        reader.skipField(1); // wire type 1 = fixed64
        assertEquals(1, reader.readVarInt());
    }

    @Test
    void skipField_lengthDelimited() {
        // length=3, then 3 garbage bytes, then 0x05
        var reader = new ProtobufReader(new byte[]{0x03, 0x00, 0x00, 0x00, 0x05});
        reader.skipField(2); // wire type 2 = length-delimited
        assertEquals(5, reader.readVarInt());
    }

    @Test
    void skipField_fixed32() {
        byte[] data = new byte[5];
        // 4 bytes for fixed32, then 0x07
        data[4] = 0x07;
        var reader = new ProtobufReader(data);
        reader.skipField(5); // wire type 5 = fixed32
        assertEquals(7, reader.readVarInt());
    }

    // ---- Error cases ----

    @Test
    void readVarInt_truncated_throwsException() {
        // continuation bit set but no more data
        var reader = new ProtobufReader(new byte[]{(byte) 0x80});
        assertThrows(IllegalStateException.class, reader::readVarInt);
    }

    @Test
    void readFixed32_truncated_throwsException() {
        var reader = new ProtobufReader(new byte[]{0x01, 0x02});
        assertThrows(IllegalStateException.class, reader::readFixed32);
    }

    @Test
    void readString_truncated_throwsException() {
        // length says 10 bytes but only 2 available
        var reader = new ProtobufReader(new byte[]{0x0A, 0x01, 0x02});
        assertThrows(IllegalStateException.class, reader::readString);
    }

    // ---- hasRemaining ----

    @Test
    void hasRemaining_afterReadingAll() {
        var reader = new ProtobufReader(new byte[]{0x01});
        assertTrue(reader.hasRemaining());
        reader.readVarInt();
        assertFalse(reader.hasRemaining());
    }
}
