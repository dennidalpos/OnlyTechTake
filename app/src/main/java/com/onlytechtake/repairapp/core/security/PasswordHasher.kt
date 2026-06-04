package com.onlytechtake.repairapp.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordHash(
    val hash: String,
    val salt: String,
)

class PasswordHasher(
    private val iterations: Int = 120_000,
    private val keyLengthBits: Int = 256,
    private val saltLengthBytes: Int = 32,
    private val random: SecureRandom = SecureRandom(),
) {
    fun hash(password: CharArray): PasswordHash {
        require(password.isNotEmpty()) { "Password must not be empty" }

        val saltBytes = ByteArray(saltLengthBytes)
        random.nextBytes(saltBytes)
        val hashBytes = deriveHash(password, saltBytes)

        return PasswordHash(
            hash = Base64.getEncoder().encodeToString(hashBytes),
            salt = Base64.getEncoder().encodeToString(saltBytes),
        )
    }

    fun verify(password: CharArray, expectedHash: String, salt: String): Boolean {
        if (password.isEmpty() || expectedHash.isBlank() || salt.isBlank()) return false

        val saltBytes = runCatching {
            Base64.getDecoder().decode(salt)
        }.getOrElse {
            return false
        }
        val expectedHashBytes = runCatching {
            Base64.getDecoder().decode(expectedHash)
        }.getOrElse {
            return false
        }
        val actualHashBytes = deriveHash(password, saltBytes)

        return constantTimeEquals(expectedHashBytes, actualHashBytes)
    }

    private fun deriveHash(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLengthBits)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun constantTimeEquals(left: ByteArray, right: ByteArray): Boolean {
        if (left.size != right.size) return false

        var diff = 0
        for (index in left.indices) {
            diff = diff or (left[index].toInt() xor right[index].toInt())
        }
        return diff == 0
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }
}
