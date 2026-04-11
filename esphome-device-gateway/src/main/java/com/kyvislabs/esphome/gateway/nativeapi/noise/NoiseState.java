package com.kyvislabs.esphome.gateway.nativeapi.noise;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
import java.util.Arrays;

/**
 * Implements the Noise_NNpsk0_25519_ChaChaPoly_SHA256 handshake and transport
 * encryption used by the ESPHome Native API encrypted protocol.
 *
 * <p>The NNpsk0 pattern:
 * <pre>
 *   -> psk, e
 *   <- e, ee
 * </pre>
 *
 * <p>This class is I/O-free — it operates purely on byte arrays. Wire framing
 * is handled by {@link NoiseFrameHelper}.
 */
public class NoiseState {

    private static final String PROTOCOL_NAME = "Noise_NNpsk0_25519_ChaChaPoly_SHA256";
    static final byte[] PROLOGUE = buildPrologue();

    private static final int KEY_LEN = 32;
    private static final int TAG_LEN = 16;

    // Handshake state
    private byte[] h;   // handshake hash
    private byte[] ck;  // chaining key
    private byte[] k;   // current cipher key (null until first MixKey/MixKeyAndHash)
    private long n;      // handshake nonce

    private final byte[] psk;
    private KeyPair ephemeralKeyPair;

    // Transport cipher state (set after split)
    private byte[] encryptKey;
    private long encryptNonce;
    private byte[] decryptKey;
    private long decryptNonce;

    public NoiseState(byte[] psk) {
        if (psk == null || psk.length != KEY_LEN) {
            throw new IllegalArgumentException("PSK must be exactly 32 bytes");
        }
        this.psk = Arrays.copyOf(psk, psk.length);
    }

    /**
     * Initialize the handshake state: hash the protocol name, set chaining key,
     * and mix in the prologue.
     */
    public void initialize() {
        byte[] protocolBytes = PROTOCOL_NAME.getBytes(StandardCharsets.US_ASCII);
        // Protocol name is 37 bytes (> 32), so hash it
        h = sha256(protocolBytes);
        ck = Arrays.copyOf(h, KEY_LEN);
        k = null;
        n = 0;

        mixHash(PROLOGUE);
    }

    /**
     * Generate the client's first handshake message (initiator message 1).
     * NNpsk0 pattern: {@code -> psk, e}
     *
     * @return handshake message bytes (32-byte ephemeral pubkey + 16-byte MAC)
     */
    public byte[] writeHandshakeMessage() throws IOException {
        try {
            // psk token: MixKeyAndHash(psk)
            mixKeyAndHash(psk);

            // e token: generate ephemeral keypair, MixHash(e.public)
            // Per Noise spec §9.2, PSK handshakes also MixKey(e.public)
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
            ephemeralKeyPair = kpg.generateKeyPair();
            byte[] ephemeralPub = publicKeyToBytes(ephemeralKeyPair.getPublic());
            mixHash(ephemeralPub);
            mixKey(ephemeralPub);

            // Encrypt empty payload (produces 16-byte MAC tag)
            byte[] ciphertext = encryptAndHash(new byte[0]);

            // Message: e.public || encrypted_empty
            byte[] message = new byte[ephemeralPub.length + ciphertext.length];
            System.arraycopy(ephemeralPub, 0, message, 0, ephemeralPub.length);
            System.arraycopy(ciphertext, 0, message, ephemeralPub.length, ciphertext.length);
            return message;
        } catch (GeneralSecurityException e) {
            throw new IOException("Noise handshake write failed", e);
        }
    }

    /**
     * Process the server's handshake response (responder message).
     * NNpsk0 pattern: {@code <- e, ee}
     *
     * @param message the server's handshake message bytes
     * @return decrypted payload from server (may be empty)
     */
    public byte[] readHandshakeMessage(byte[] message) throws IOException {
        try {
            if (message.length < KEY_LEN) {
                throw new IOException("Handshake message too short: " + message.length);
            }

            // e token: extract server ephemeral public key
            // Per Noise spec §9.2, PSK handshakes also MixKey(e.public)
            byte[] remotePub = Arrays.copyOfRange(message, 0, KEY_LEN);
            mixHash(remotePub);
            mixKey(remotePub);
            PublicKey remoteKey = bytesToPublicKey(remotePub);

            // ee token: DH(e, re)
            byte[] dhResult = x25519(ephemeralKeyPair.getPrivate(), remoteKey);
            mixKey(dhResult);

            // Decrypt server payload
            byte[] encrypted = Arrays.copyOfRange(message, KEY_LEN, message.length);
            byte[] payload = decryptAndHash(encrypted);

            // Split: derive transport keys
            split();

            return payload;
        } catch (GeneralSecurityException e) {
            throw new IOException("Noise handshake read failed", e);
        }
    }

    /**
     * Encrypt a message using the transport encryption key.
     * Nonce auto-increments after each call.
     */
    public byte[] encrypt(byte[] plaintext) throws IOException {
        try {
            byte[] result = chacha20poly1305(encryptKey, encryptNonce, null, plaintext, true);
            encryptNonce++;
            return result;
        } catch (GeneralSecurityException e) {
            throw new IOException("Noise encrypt failed", e);
        }
    }

    /**
     * Decrypt a message using the transport decryption key.
     * Nonce auto-increments after each call.
     */
    public byte[] decrypt(byte[] ciphertext) throws IOException {
        try {
            byte[] result = chacha20poly1305(decryptKey, decryptNonce, null, ciphertext, false);
            decryptNonce++;
            return result;
        } catch (GeneralSecurityException e) {
            throw new IOException("Noise decrypt failed", e);
        }
    }

    // ---- Noise framework primitives ----

    private void mixHash(byte[] data) {
        byte[] combined = new byte[h.length + data.length];
        System.arraycopy(h, 0, combined, 0, h.length);
        System.arraycopy(data, 0, combined, h.length, data.length);
        h = sha256(combined);
    }

    private void mixKey(byte[] inputKeyMaterial) {
        byte[][] outputs = hkdf(ck, inputKeyMaterial, 2);
        ck = outputs[0];
        k = outputs[1];
        n = 0;
    }

    private void mixKeyAndHash(byte[] inputKeyMaterial) {
        byte[][] outputs = hkdf(ck, inputKeyMaterial, 3);
        ck = outputs[0];
        mixHash(outputs[1]);
        k = outputs[2];
        n = 0;
    }

    private byte[] encryptAndHash(byte[] plaintext) throws GeneralSecurityException {
        byte[] ciphertext;
        if (k != null) {
            ciphertext = chacha20poly1305(k, n, h, plaintext, true);
            n++;
        } else {
            ciphertext = plaintext;
        }
        mixHash(ciphertext);
        return ciphertext;
    }

    private byte[] decryptAndHash(byte[] ciphertext) throws GeneralSecurityException {
        byte[] plaintext;
        if (k != null) {
            plaintext = chacha20poly1305(k, n, h, ciphertext, false);
            n++;
        } else {
            plaintext = ciphertext;
        }
        mixHash(ciphertext);
        return plaintext;
    }

    private void split() {
        byte[][] outputs = hkdf(ck, new byte[0], 2);
        encryptKey = outputs[0];
        encryptNonce = 0;
        decryptKey = outputs[1];
        decryptNonce = 0;
    }

    // ---- Crypto helpers ----

    static byte[][] hkdf(byte[] chainingKey, byte[] inputKeyMaterial, int numOutputs) {
        try {
            // Extract
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chainingKey, "HmacSHA256"));
            byte[] tempKey = mac.doFinal(inputKeyMaterial);

            // Expand
            byte[][] outputs = new byte[numOutputs][];
            mac.init(new SecretKeySpec(tempKey, "HmacSHA256"));
            outputs[0] = mac.doFinal(new byte[]{0x01});

            for (int i = 1; i < numOutputs; i++) {
                mac.init(new SecretKeySpec(tempKey, "HmacSHA256"));
                mac.update(outputs[i - 1]);
                outputs[i] = mac.doFinal(new byte[]{(byte) (i + 1)});
            }

            return outputs;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("HKDF failed", e);
        }
    }

    private static byte[] x25519(PrivateKey privateKey, PublicKey publicKey) throws GeneralSecurityException {
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(privateKey);
        ka.doPhase(publicKey, true);
        return ka.generateSecret();
    }

    static byte[] chacha20poly1305(byte[] key, long nonce, byte[] ad, byte[] data, boolean encrypt)
            throws GeneralSecurityException {
        // Build 12-byte IV: 4 zero bytes + 8-byte little-endian nonce
        byte[] iv = new byte[12];
        ByteBuffer.wrap(iv, 4, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(nonce);

        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        SecretKeySpec keySpec = new SecretKeySpec(key, "ChaCha20");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec, ivSpec);
        if (ad != null) {
            cipher.updateAAD(ad);
        }
        return cipher.doFinal(data);
    }

    /**
     * Convert a JDK XEC public key to 32 raw bytes in X25519 (little-endian) format.
     */
    static byte[] publicKeyToBytes(PublicKey publicKey) {
        XECPublicKey xecKey = (XECPublicKey) publicKey;
        BigInteger u = xecKey.getU();
        byte[] uBytes = u.toByteArray();

        // BigInteger is big-endian and may have a leading zero byte for sign.
        // X25519 wire format is little-endian, 32 bytes.
        byte[] result = new byte[KEY_LEN];

        // Strip leading zero if present
        int offset = (uBytes.length > KEY_LEN && uBytes[0] == 0) ? 1 : 0;
        int len = Math.min(uBytes.length - offset, KEY_LEN);

        // Copy big-endian bytes into result, reversing to little-endian
        for (int i = 0; i < len; i++) {
            result[i] = uBytes[uBytes.length - 1 - i];
        }
        return result;
    }

    /**
     * Convert 32 raw bytes (little-endian X25519 format) to a JDK XEC public key.
     */
    static PublicKey bytesToPublicKey(byte[] raw) throws GeneralSecurityException {
        if (raw.length != KEY_LEN) {
            throw new IllegalArgumentException("X25519 public key must be 32 bytes");
        }
        // Reverse from little-endian to big-endian for BigInteger
        byte[] reversed = new byte[KEY_LEN];
        for (int i = 0; i < KEY_LEN; i++) {
            reversed[i] = raw[KEY_LEN - 1 - i];
        }
        BigInteger u = new BigInteger(1, reversed);
        KeyFactory kf = KeyFactory.getInstance("X25519");
        return kf.generatePublic(new XECPublicKeySpec(NamedParameterSpec.X25519, u));
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static byte[] buildPrologue() {
        byte[] prologue = new byte[14];
        byte[] prefix = "NoiseAPIInit".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(prefix, 0, prologue, 0, prefix.length);
        // prologue[12] and [13] are already 0x00
        return prologue;
    }
}
