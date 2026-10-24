package com.gdgcorehq.app.data;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cryptographic engine and QR generation utility for GDG CoreHQ.
 * Strictly adheres to PRD §3.2 (Digital QR Attendance):
 * - Encrypts PRN, User ID, and Timestamp using authenticated AES-GCM (256-bit).
 * - Enforces replay-attack protection via timestamp freshness verification.
 * - Generates high-contrast QR Bitmaps via ZXing Core.
 */
public final class QrCryptoManager {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // Standard GCM recommended IV length
    private static final int GCM_TAG_LENGTH_BITS = 128; // 128-bit authentication tag
    public static final long DEFAULT_EXPIRATION_MS = 15 * 60 * 1000L; // 15-minute freshness window

    // Pre-Firebase development key (256-bit / 32 bytes)
    // To be moved to remote configuration / Firebase Secrets during backend integration
    private static final byte[] PRE_FIREBASE_AES_KEY = new byte[] {
            (byte) 0x3d, (byte) 0x8a, (byte) 0x9f, (byte) 0x12,
            (byte) 0x7c, (byte) 0x45, (byte) 0x6e, (byte) 0xb0,
            (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44,
            (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88,
            (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc,
            (byte) 0xdd, (byte) 0xee, (byte) 0xff, (byte) 0x00,
            (byte) 0x2a, (byte) 0x4b, (byte) 0x6c, (byte) 0x8d,
            (byte) 0x1e, (byte) 0x3f, (byte) 0x5a, (byte) 0x7b
    };

    private static final SecretKey SECRET_KEY = new SecretKeySpec(PRE_FIREBASE_AES_KEY, "AES");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private QrCryptoManager() { /* utility class */ }

    /**
     * Decrypted result containing validated attendance payload metadata.
     */
    public static class DecryptedPayload {
        private final boolean success;
        private final String prn;
        private final String userId;
        private final long timestamp;
        private final boolean expired;
        private final String errorMessage;

        public DecryptedPayload(boolean success, String prn, String userId, long timestamp, boolean expired, String errorMessage) {
            this.success = success;
            this.prn = prn;
            this.userId = userId;
            this.timestamp = timestamp;
            this.expired = expired;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getPrn() { return prn; }
        public String getUserId() { return userId; }
        public long getTimestamp() { return timestamp; }
        public boolean isExpired() { return expired; }
        public String getErrorMessage() { return errorMessage; }

        public static DecryptedPayload failure(String error) {
            return new DecryptedPayload(false, null, null, 0, false, error);
        }

        public static DecryptedPayload expired(String prn, String userId, long timestamp) {
            return new DecryptedPayload(false, prn, userId, timestamp, true, "QR code has expired. Please refresh the QR screen.");
        }

        public static DecryptedPayload valid(String prn, String userId, long timestamp) {
            return new DecryptedPayload(true, prn, userId, timestamp, false, null);
        }
    }

    /**
     * Encrypts member PRN and user ID into an authenticated Base64 string for QR generation.
     */
    public static String encryptPayload(String prn, String userId) throws Exception {
        return encryptPayloadWithTime(prn, userId, System.currentTimeMillis());
    }

    /**
     * Public for deterministic unit testing.
     */
    public static String encryptPayloadWithTime(String prn, String userId, long timestamp) throws Exception {
        if (prn == null || prn.trim().isEmpty()) {
            throw new IllegalArgumentException("PRN cannot be empty");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId cannot be empty");
        }

        // Random 8-byte nonce to ensure uniqueness even within the same millisecond
        byte[] nonce = new byte[8];
        SECURE_RANDOM.nextBytes(nonce);
        String nonceHex = bytesToHex(nonce);

        // Structured plaintext: PRN:<prn>|UID:<uid>|TS:<timestamp>|N:<nonce>
        String plaintext = "PRN:" + prn.trim().toUpperCase() +
                "|UID:" + userId.trim() +
                "|TS:" + timestamp +
                "|N:" + nonceHex;

        // Generate 12-byte IV for GCM
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, spec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Output buffer = IV (12 bytes) + Ciphertext + Tag
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
    }

    /**
     * Decrypts and authenticates a scanned QR token string.
     * Validates cryptographic authenticity and enforces timestamp freshness.
     */
    public static DecryptedPayload decryptPayload(String token, long maxAgeMs) {
        if (token == null || token.trim().isEmpty()) {
            return DecryptedPayload.failure("Scanned barcode is empty");
        }

        try {
            byte[] combined;
            try {
                combined = Base64.getUrlDecoder().decode(token.trim());
            } catch (IllegalArgumentException e) {
                // Fallback attempt standard base64 if url-safe was padded
                combined = Base64.getDecoder().decode(token.trim());
            }

            if (combined.length <= GCM_IV_LENGTH_BYTES) {
                return DecryptedPayload.failure("Invalid QR code payload structure");
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            int ciphertextLen = combined.length - GCM_IV_LENGTH_BYTES;
            byte[] ciphertext = new byte[ciphertextLen];
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertextLen);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, spec);

            byte[] plaintextBytes = cipher.doFinal(ciphertext);
            String plaintext = new String(plaintextBytes, StandardCharsets.UTF_8);

            // Parse structured plaintext: PRN:<prn>|UID:<uid>|TS:<timestamp>|N:<nonce>
            String prn = null;
            String uid = null;
            long timestamp = 0;

            String[] parts = plaintext.split("\\|");
            for (String part : parts) {
                if (part.startsWith("PRN:")) {
                    prn = part.substring(4);
                } else if (part.startsWith("UID:")) {
                    uid = part.substring(4);
                } else if (part.startsWith("TS:")) {
                    try {
                        timestamp = Long.parseLong(part.substring(3));
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (prn == null || prn.isEmpty()) {
                return DecryptedPayload.failure("Corrupted PRN in decrypted payload");
            }

            // Verify replay freshness window
            long now = System.currentTimeMillis();
            if (maxAgeMs > 0 && Math.abs(now - timestamp) > maxAgeMs) {
                return DecryptedPayload.expired(prn, uid, timestamp);
            }

            return DecryptedPayload.valid(prn, uid, timestamp);

        } catch (Exception e) {
            return DecryptedPayload.failure("Cryptographic verification failed: " + e.getMessage());
        }
    }

    /**
     * Convenience method using default expiration window (15 minutes).
     */
    public static DecryptedPayload decryptPayload(String token) {
        return decryptPayload(token, DEFAULT_EXPIRATION_MS);
    }

    /**
     * Generates a high-contrast QR Bitmap from raw content using ZXing Core.
     */
    public static Bitmap generateQrBitmap(String content, int sizePx) throws Exception {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("QR content cannot be empty");
        }

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];

        final int COLOR_DARK = 0xFF18233F; // Brand navy
        final int COLOR_LIGHT = Color.WHITE;

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? COLOR_DARK : COLOR_LIGHT;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
