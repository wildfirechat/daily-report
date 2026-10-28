package cn.wildfirechat.app.shiro;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Password hashing helper.
 *
 * <p>New passwords are stored as bcrypt. Existing MD5+salt hashes are still accepted so the
 * change can be deployed without a database migration; they can be upgraded on the next password
 * change. MD5 is only kept as a legacy compatibility path.</p>
 */
public final class PasswordHasher {
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private PasswordHasher() {
    }

    public static String encode(String rawPassword) {
        return BCRYPT.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedPassword, String salt) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
            || storedPassword.startsWith("$2y$")) {
            return BCRYPT.matches(rawPassword, storedPassword);
        }

        // Legacy MD5(password + salt), kept only for accounts that have not changed password yet.
        String legacy = new Base64().encodeToString(
            DigestUtils.getDigest("MD5").digest((rawPassword + salt).getBytes(StandardCharsets.UTF_8)));
        return MessageDigest.isEqual(
            legacy.getBytes(StandardCharsets.UTF_8),
            storedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
