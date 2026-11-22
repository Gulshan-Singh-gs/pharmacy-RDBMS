package main;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * PasswordUtil - salted SHA-256 password hashing helpers.
 *
 * <p>Scheme: hash = hex(SHA-256(salt_hex + password)). The salt is a random
 * 16-byte value stored in hex (32 chars) alongside the hash (64 chars).
 * Verification uses a constant-time comparison.
 *
 * @author Gulshan Singh
 */
public final class PasswordUtil {

    private static final int SALT_BYTES = 16;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PasswordUtil() {
        // utility class
    }

    /** Generates a new random salt as a 32-character hex string. */
    public static String randomSaltHex() {
        byte[] bytes = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    /**
     * Computes the hex SHA-256 hash of {@code saltHex + raw}.
     *
     * @param saltHex the salt, as a hex string
     * @param raw the plaintext password
     * @return 64-character hex digest
     */
    public static String hash(String saltHex, String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((saltHex + raw).getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Verifies a plaintext password against a stored salted hash using a
     * constant-time comparison.
     */
    public static boolean verify(String saltHex, String raw, String expectedHash) {
        if (saltHex == null || expectedHash == null) {
            return false;
        }
        String actual = hash(saltHex, raw);
        byte[] a = actual.getBytes(StandardCharsets.UTF_8);
        byte[] b = expectedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
