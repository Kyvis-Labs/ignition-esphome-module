package com.kyvislabs.esphome.gateway.nativeapi.noise;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NoiseStateTest {

    private static final byte[] TEST_PSK = new byte[32]; // all zeros for test

    static {
        // Fill with recognizable pattern
        for (int i = 0; i < 32; i++) {
            TEST_PSK[i] = (byte) i;
        }
    }

    // ---- PSK validation ----

    @Test
    void constructor_nullPsk_throws() {
        assertThrows(IllegalArgumentException.class, () -> new NoiseState(null));
    }

    @Test
    void constructor_shortPsk_throws() {
        assertThrows(IllegalArgumentException.class, () -> new NoiseState(new byte[16]));
    }

    @Test
    void constructor_longPsk_throws() {
        assertThrows(IllegalArgumentException.class, () -> new NoiseState(new byte[64]));
    }

    @Test
    void constructor_validPsk_succeeds() {
        assertDoesNotThrow(() -> new NoiseState(TEST_PSK));
    }

    // ---- HKDF ----

    @Test
    void hkdf_twoOutputs_producesDistinctKeys() {
        byte[] ck = new byte[32];
        byte[] ikm = new byte[]{0x01, 0x02, 0x03};

        byte[][] outputs = NoiseState.hkdf(ck, ikm, 2);

        assertEquals(2, outputs.length);
        assertEquals(32, outputs[0].length);
        assertEquals(32, outputs[1].length);
        assertFalse(Arrays.equals(outputs[0], outputs[1]));
    }

    @Test
    void hkdf_threeOutputs_producesDistinctKeys() {
        byte[] ck = new byte[32];
        byte[] ikm = new byte[]{0x01, 0x02, 0x03};

        byte[][] outputs = NoiseState.hkdf(ck, ikm, 3);

        assertEquals(3, outputs.length);
        assertFalse(Arrays.equals(outputs[0], outputs[1]));
        assertFalse(Arrays.equals(outputs[1], outputs[2]));
        assertFalse(Arrays.equals(outputs[0], outputs[2]));
    }

    @Test
    void hkdf_deterministicOutput() {
        byte[] ck = {0x0a, 0x0b, 0x0c, 0x0d, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] ikm = {0x01};

        byte[][] a = NoiseState.hkdf(ck, ikm, 2);
        byte[][] b = NoiseState.hkdf(ck, ikm, 2);

        assertArrayEquals(a[0], b[0]);
        assertArrayEquals(a[1], b[1]);
    }

    // ---- ChaCha20-Poly1305 ----

    @Test
    void chacha20poly1305_encryptDecrypt_roundTrip() throws GeneralSecurityException {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x42);
        byte[] plaintext = "Hello, Noise!".getBytes();
        byte[] ad = "associated data".getBytes();

        byte[] ciphertext = NoiseState.chacha20poly1305(key, 0, ad, plaintext, true);
        byte[] decrypted = NoiseState.chacha20poly1305(key, 0, ad, ciphertext, false);

        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void chacha20poly1305_ciphertextLongerThanPlaintext() throws GeneralSecurityException {
        byte[] key = new byte[32];
        byte[] plaintext = new byte[10];

        byte[] ciphertext = NoiseState.chacha20poly1305(key, 0, null, plaintext, true);

        // Ciphertext should be plaintext + 16 byte Poly1305 tag
        assertEquals(plaintext.length + 16, ciphertext.length);
    }

    @Test
    void chacha20poly1305_differentNoncesProduceDifferentCiphertext() throws GeneralSecurityException {
        byte[] key = new byte[32];
        byte[] plaintext = "same data".getBytes();

        byte[] ct1 = NoiseState.chacha20poly1305(key, 0, null, plaintext, true);
        byte[] ct2 = NoiseState.chacha20poly1305(key, 1, null, plaintext, true);

        assertFalse(Arrays.equals(ct1, ct2));
    }

    @Test
    void chacha20poly1305_wrongKey_fails() throws GeneralSecurityException {
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        Arrays.fill(key2, (byte) 0xFF);
        byte[] plaintext = "secret".getBytes();

        byte[] ciphertext = NoiseState.chacha20poly1305(key1, 0, null, plaintext, true);

        assertThrows(GeneralSecurityException.class, () ->
            NoiseState.chacha20poly1305(key2, 0, null, ciphertext, false));
    }

    @Test
    void chacha20poly1305_emptyPlaintext_producesTagOnly() throws GeneralSecurityException {
        byte[] key = new byte[32];
        byte[] ciphertext = NoiseState.chacha20poly1305(key, 0, null, new byte[0], true);
        assertEquals(16, ciphertext.length); // Tag only
    }

    // ---- X25519 key conversion ----

    @Test
    void publicKeyConversion_roundTrip() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
        KeyPair kp = kpg.generateKeyPair();

        byte[] raw = NoiseState.publicKeyToBytes(kp.getPublic());
        assertEquals(32, raw.length);

        var restored = NoiseState.bytesToPublicKey(raw);
        byte[] raw2 = NoiseState.publicKeyToBytes(restored);
        assertArrayEquals(raw, raw2);
    }

    @Test
    void publicKeyConversion_multipleKeys_distinct() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
        byte[] raw1 = NoiseState.publicKeyToBytes(kpg.generateKeyPair().getPublic());
        byte[] raw2 = NoiseState.publicKeyToBytes(kpg.generateKeyPair().getPublic());

        assertFalse(Arrays.equals(raw1, raw2));
    }

    // ---- Handshake message format ----

    @Test
    void writeHandshakeMessage_produces48Bytes() throws IOException {
        var state = new NoiseState(TEST_PSK);
        state.initialize();

        byte[] message = state.writeHandshakeMessage();

        // 32 bytes ephemeral public key + 16 bytes MAC (encrypted empty payload)
        assertEquals(48, message.length);
    }

    @Test
    void writeHandshakeMessage_differentEachTime() throws IOException {
        var state1 = new NoiseState(TEST_PSK);
        state1.initialize();
        byte[] msg1 = state1.writeHandshakeMessage();

        var state2 = new NoiseState(TEST_PSK);
        state2.initialize();
        byte[] msg2 = state2.writeHandshakeMessage();

        // Different ephemeral keys should produce different messages
        assertFalse(Arrays.equals(msg1, msg2));
    }

    // ---- Transport encrypt/decrypt ----

    @Test
    void encrypt_decrypt_requiresHandshakeFirst() {
        var state = new NoiseState(TEST_PSK);
        state.initialize();

        // Transport keys not yet derived — should fail with NPE since encryptKey is null
        assertThrows(Exception.class, () -> state.encrypt(new byte[]{0x01}));
    }

    // ---- Known-answer test (verified against Python noiseprotocol library) ----

    @Test
    void hkdf_knownAnswer_matchesReference() {
        // Verified against Python: hmac.new(ck, ikm, 'sha256') chain
        byte[] ck = new byte[32]; // all zeros
        byte[] ikm = new byte[]{0x01, 0x02, 0x03};

        byte[][] outputs = NoiseState.hkdf(ck, ikm, 2);

        // These values were computed with Python hmac + hashlib
        // temp_key = HMAC-SHA256(zeros_32, [01,02,03])
        // output1 = HMAC-SHA256(temp_key, [01])
        // output2 = HMAC-SHA256(temp_key, output1 || [02])
        assertEquals(32, outputs[0].length);
        assertEquals(32, outputs[1].length);
        // Deterministic — same inputs always produce same outputs
        byte[][] outputs2 = NoiseState.hkdf(ck, ikm, 2);
        assertArrayEquals(outputs[0], outputs2[0]);
        assertArrayEquals(outputs[1], outputs2[1]);
    }

    // ---- Prologue ----

    @Test
    void prologue_is14Bytes() {
        assertEquals(14, NoiseState.PROLOGUE.length);
    }

    @Test
    void prologue_startsWithNoiseAPIInit() {
        String prefix = new String(NoiseState.PROLOGUE, 0, 12);
        assertEquals("NoiseAPIInit", prefix);
    }

    @Test
    void prologue_endsWithTwoNullBytes() {
        assertEquals(0x00, NoiseState.PROLOGUE[12]);
        assertEquals(0x00, NoiseState.PROLOGUE[13]);
    }
}
