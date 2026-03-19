package com.kyvislabs.esphome.gateway.nativeapi.proto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtobufWriterTest {

    // ---- writeVarInt ----

    @Test
    void writeVarInt_singleByte() {
        var writer = new ProtobufWriter();
        writer.writeVarInt(1);
        assertArrayEquals(new byte[]{0x01}, writer.toByteArray());
    }

    @Test
    void writeVarInt_zero() {
        var writer = new ProtobufWriter();
        writer.writeVarInt(0);
        assertArrayEquals(new byte[]{0x00}, writer.toByteArray());
    }

    @Test
    void writeVarInt_127() {
        var writer = new ProtobufWriter();
        writer.writeVarInt(127);
        assertArrayEquals(new byte[]{0x7F}, writer.toByteArray());
    }

    @Test
    void writeVarInt_128_multiByte() {
        var writer = new ProtobufWriter();
        writer.writeVarInt(128);
        assertArrayEquals(new byte[]{(byte) 0x80, 0x01}, writer.toByteArray());
    }

    @Test
    void writeVarInt_300_multiByte() {
        var writer = new ProtobufWriter();
        writer.writeVarInt(300);
        assertArrayEquals(new byte[]{(byte) 0xAC, 0x02}, writer.toByteArray());
    }

    // ---- writeFloatField ----

    @Test
    void writeFloatField_encodesTagAndFixed32() {
        var writer = new ProtobufWriter();
        writer.writeFloatField(3, 1.0f);
        byte[] result = writer.toByteArray();

        var reader = new ProtobufReader(result);
        int tag = reader.readTag();
        assertEquals(3, reader.getFieldNumber(tag));
        assertEquals(5, reader.getWireType(tag)); // fixed32 wire type
        assertEquals(1.0f, reader.readFloat());
    }

    // ---- writeStringField ----

    @Test
    void writeStringField_encodesTagLengthAndBytes() {
        var writer = new ProtobufWriter();
        writer.writeStringField(2, "hello");
        byte[] result = writer.toByteArray();

        var reader = new ProtobufReader(result);
        int tag = reader.readTag();
        assertEquals(2, reader.getFieldNumber(tag));
        assertEquals(2, reader.getWireType(tag)); // length-delimited wire type
        assertEquals("hello", reader.readString());
    }

    @Test
    void writeStringField_emptyString_skipped() {
        var writer = new ProtobufWriter();
        writer.writeStringField(1, "");
        assertEquals(0, writer.toByteArray().length);
    }

    @Test
    void writeStringField_null_skipped() {
        var writer = new ProtobufWriter();
        writer.writeStringField(1, null);
        assertEquals(0, writer.toByteArray().length);
    }

    // ---- writeBoolField ----

    @Test
    void writeBoolField_true() {
        var writer = new ProtobufWriter();
        writer.writeBoolField(1, true);
        byte[] result = writer.toByteArray();

        var reader = new ProtobufReader(result);
        int tag = reader.readTag();
        assertEquals(1, reader.getFieldNumber(tag));
        assertEquals(0, reader.getWireType(tag)); // varint wire type
        assertTrue(reader.readBool());
    }

    @Test
    void writeBoolField_false() {
        var writer = new ProtobufWriter();
        writer.writeBoolField(1, false);
        byte[] result = writer.toByteArray();

        var reader = new ProtobufReader(result);
        int tag = reader.readTag();
        assertEquals(1, reader.getFieldNumber(tag));
        assertFalse(reader.readBool());
    }

    // ---- Round-trip tests ----

    @Test
    void roundTrip_mixedFields() {
        var writer = new ProtobufWriter();
        writer.writeVarIntField(1, 42);
        writer.writeStringField(2, "test");
        writer.writeFloatField(3, 3.14f);
        writer.writeBoolField(4, true);

        var reader = new ProtobufReader(writer.toByteArray());

        int tag1 = reader.readTag();
        assertEquals(1, reader.getFieldNumber(tag1));
        assertEquals(42, reader.readVarInt());

        int tag2 = reader.readTag();
        assertEquals(2, reader.getFieldNumber(tag2));
        assertEquals("test", reader.readString());

        int tag3 = reader.readTag();
        assertEquals(3, reader.getFieldNumber(tag3));
        assertEquals(3.14f, reader.readFloat());

        int tag4 = reader.readTag();
        assertEquals(4, reader.getFieldNumber(tag4));
        assertTrue(reader.readBool());

        assertFalse(reader.hasRemaining());
    }
}
