package com.onlytechtake.repairapp.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun hashVerifiesOriginalPasswordOnly() {
        val hasher = PasswordHasher(iterations = 1_000)

        val result = hasher.hash("password-sicura".toCharArray())

        assertTrue(hasher.verify("password-sicura".toCharArray(), result.hash, result.salt))
        assertFalse(hasher.verify("password-errata".toCharArray(), result.hash, result.salt))
    }

    @Test
    fun hashUsesRandomSalt() {
        val hasher = PasswordHasher(iterations = 1_000)

        val first = hasher.hash("password-sicura".toCharArray())
        val second = hasher.hash("password-sicura".toCharArray())

        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
    }
}
