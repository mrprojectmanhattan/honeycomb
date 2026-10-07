package com.mark.moodlogger.data

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PIN hashing for the app lock (2026-09-28). Never store or compare the plain PIN -
 * only a salted SHA-256 hash lives in Settings. A fresh random salt is generated once,
 * the first time a PIN is set, and reused for every check after that.
 */
object AppLock {
    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, storedHash: String, salt: String): Boolean {
        if (storedHash.isEmpty() || salt.isEmpty()) return false
        return hashPin(pin, salt) == storedHash
    }
}
