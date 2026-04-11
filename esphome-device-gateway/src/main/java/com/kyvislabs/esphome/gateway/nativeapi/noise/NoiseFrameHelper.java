package com.kyvislabs.esphome.gateway.nativeapi.noise;

import com.kyvislabs.esphome.gateway.nativeapi.proto.MessageFramer.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Handles Noise-encrypted frame I/O for the ESPHome Native API.
 *
 * <p>Encrypted frame wire format:
 * <pre>
 *   [0x01 preamble] [2-byte big-endian length] [encrypted payload]
 * </pre>
 *
 * <p>After decryption, the inner payload structure is:
 * <pre>
 *   [2-byte BE message_type] [2-byte BE data_length] [protobuf data]
 * </pre>
 */
public class NoiseFrameHelper {

    private static final byte[] NOISE_HELLO = {0x01, 0x00, 0x00};
    private static final int TAG_LEN = 16;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InputStream in;
    private final OutputStream out;
    private final NoiseState noiseState;

    private String serverName;

    public NoiseFrameHelper(InputStream in, OutputStream out, byte[] psk) {
        this.in = in;
        this.out = out;
        this.noiseState = new NoiseState(psk);
    }

    /**
     * Perform the full Noise handshake over the wire.
     *
     * <ol>
     *   <li>Send NOISE_HELLO (0x01 0x00 0x00)</li>
     *   <li>Send client handshake frame</li>
     *   <li>Read server hello frame</li>
     *   <li>Read server handshake response</li>
     * </ol>
     *
     * After this method returns, {@link #readMessage()} and {@link #writeMessage}
     * can be used for encrypted communication.
     */
    public void performHandshake() throws IOException {
        noiseState.initialize();

        // Build client handshake message
        byte[] handshakePayload = noiseState.writeHandshakeMessage();

        // Prepend 0x00 type byte to handshake payload
        byte[] framedPayload = new byte[1 + handshakePayload.length];
        framedPayload[0] = 0x00;
        System.arraycopy(handshakePayload, 0, framedPayload, 1, handshakePayload.length);

        // Send NOISE_HELLO + handshake frame together
        byte[] handshakeFrame = buildNoiseFrame(framedPayload);
        byte[] combined = new byte[NOISE_HELLO.length + handshakeFrame.length];
        System.arraycopy(NOISE_HELLO, 0, combined, 0, NOISE_HELLO.length);
        System.arraycopy(handshakeFrame, 0, combined, NOISE_HELLO.length, handshakeFrame.length);

        out.write(combined);
        out.flush();

        // Read server hello
        byte[] serverHelloPayload = readNoiseFrame();
        parseServerHello(serverHelloPayload);

        // Read server handshake response
        byte[] serverHandshake = readNoiseFrame();
        if (serverHandshake.length < 1) {
            throw new IOException("Empty server handshake response");
        }
        if (serverHandshake[0] != 0x00) {
            throw new IOException("Noise handshake failed: server returned error code 0x" +
                    Integer.toHexString(serverHandshake[0] & 0xFF));
        }

        // Process the Noise handshake message (skip the 0x00 success byte)
        byte[] noiseMessage = new byte[serverHandshake.length - 1];
        System.arraycopy(serverHandshake, 1, noiseMessage, 0, noiseMessage.length);
        noiseState.readHandshakeMessage(noiseMessage);

        logger.info("Noise handshake complete with server: {}", serverName);
    }

    /**
     * Read and decrypt one application message from the encrypted channel.
     *
     * @return a RawMessage with the decrypted message type and protobuf payload
     */
    public RawMessage readMessage() throws IOException {
        byte[] encrypted = readNoiseFrame();
        byte[] decrypted = noiseState.decrypt(encrypted);

        if (decrypted.length < 4) {
            throw new IOException("Decrypted Noise payload too short: " + decrypted.length);
        }

        // Parse inner structure: [2-byte BE type] [2-byte BE length] [data]
        int messageType = ((decrypted[0] & 0xFF) << 8) | (decrypted[1] & 0xFF);
        int dataLength = ((decrypted[2] & 0xFF) << 8) | (decrypted[3] & 0xFF);

        byte[] payload;
        if (decrypted.length > 4) {
            payload = new byte[decrypted.length - 4];
            System.arraycopy(decrypted, 4, payload, 0, payload.length);
        } else {
            payload = new byte[0];
        }

        return new RawMessage(messageType, payload);
    }

    /**
     * Encrypt and write one application message to the encrypted channel.
     */
    public void writeMessage(int messageType, byte[] payload) throws IOException {
        // Build inner content: [2-byte BE type] [2-byte BE length] [data]
        int dataLength = (payload != null) ? payload.length : 0;
        byte[] inner = new byte[4 + dataLength];
        inner[0] = (byte) ((messageType >> 8) & 0xFF);
        inner[1] = (byte) (messageType & 0xFF);
        inner[2] = (byte) ((dataLength >> 8) & 0xFF);
        inner[3] = (byte) (dataLength & 0xFF);
        if (payload != null && payload.length > 0) {
            System.arraycopy(payload, 0, inner, 4, payload.length);
        }

        byte[] encrypted = noiseState.encrypt(inner);
        byte[] frame = buildNoiseFrame(encrypted);

        out.write(frame);
        out.flush();
    }

    /**
     * Calculate the total wire size of an encrypted frame.
     */
    public int frameSize(int messageType, byte[] payload) {
        int dataLength = (payload != null) ? payload.length : 0;
        int innerLength = 4 + dataLength;
        // 1 (preamble) + 2 (length) + innerLength + 16 (MAC tag)
        return 3 + innerLength + TAG_LEN;
    }

    public String getServerName() {
        return serverName;
    }

    // ---- Frame I/O helpers ----

    private byte[] readNoiseFrame() throws IOException {
        // Read preamble
        int preamble = in.read();
        if (preamble == -1) {
            throw new IOException("Connection closed during Noise frame read");
        }
        if (preamble != 0x01) {
            throw new IOException("Expected Noise frame indicator 0x01, got 0x" +
                    Integer.toHexString(preamble));
        }

        // Read 2-byte big-endian length
        int hi = in.read();
        int lo = in.read();
        if (hi == -1 || lo == -1) {
            throw new IOException("Connection closed reading Noise frame length");
        }
        int length = (hi << 8) | lo;

        // Read payload
        if (length == 0) {
            return new byte[0];
        }
        byte[] payload = in.readNBytes(length);
        if (payload.length != length) {
            throw new IOException("Unexpected end of stream reading Noise frame data");
        }
        return payload;
    }

    private byte[] buildNoiseFrame(byte[] payload) {
        int length = payload.length;
        byte[] frame = new byte[3 + length];
        frame[0] = 0x01;
        frame[1] = (byte) ((length >> 8) & 0xFF);
        frame[2] = (byte) (length & 0xFF);
        System.arraycopy(payload, 0, frame, 3, length);
        return frame;
    }

    private void parseServerHello(byte[] payload) throws IOException {
        if (payload.length < 1) {
            throw new IOException("Server hello too short");
        }

        // First byte is chosen protocol (must be 0x01 for Noise)
        if (payload[0] != 0x01) {
            throw new IOException("Server chose unsupported protocol: 0x" +
                    Integer.toHexString(payload[0] & 0xFF));
        }

        // Parse null-terminated server name starting at byte 1
        serverName = "";
        if (payload.length > 1) {
            int nameEnd = indexOf(payload, (byte) 0x00, 1);
            if (nameEnd == -1) {
                nameEnd = payload.length;
            }
            if (nameEnd > 1) {
                serverName = new String(payload, 1, nameEnd - 1, java.nio.charset.StandardCharsets.UTF_8);
            }
        }
    }

    private static int indexOf(byte[] array, byte value, int fromIndex) {
        for (int i = fromIndex; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
