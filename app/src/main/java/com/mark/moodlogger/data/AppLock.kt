package com.mark.moodlogger.data

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN hashing for the app lock (2026-09-28, strengthened 2026-10-07). Never store or
 * compare the plain PIN - only a salted PBKDF2 hash lives in Settings. A fresh random
 * salt is generated once, the first time a PIN is set, and reused for every check after
 * that.
 *
 * PBKDF2 matters here specifically because the PIN is only 6 digits - a million
 * possible values. A single fast hash round (the original implementation) makes the
 * whole keyspace exhaustible in well under a second if the salt+hash ever leak off the
 * device. 210,000 PBKDF2-HMAC-SHA256 rounds raises that cost by roughly five orders of
 * magnitude while staying fast enough (well under half a second) for a real unlock
 * screen. Flagged by an outside reviewer on the public repo.
 */
object AppLock {
    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, storedHash: String, salt: String): Boolean {
        if (storedHash.isEmpty() || salt.isEmpty()) return false
        return hashPin(pin, salt) == storedHash
    }
}
