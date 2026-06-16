package com.onlytechtake.repairapp.core.security

import android.content.Context
import androidx.core.content.edit
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gestisce la generazione e conservazione sicura della chiave per il database cifrato.
 * Utilizza Android KeyStore per proteggere una chiave AES-256 "master" locale,
 * che a sua volta cifra la chiave reale utilizzata da SQLCipher.
 */
class DatabaseKeyManager(private val context: Context) {

    fun getDatabaseKey(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedKeyBase64 = prefs.getString(KEY_ENCRYPTED_DB_PASSWORD, null)
        val ivBase64 = prefs.getString(KEY_IV, null)

        return if (encryptedKeyBase64 != null && ivBase64 != null) {
            decryptKey(encryptedKeyBase64, ivBase64)
        } else {
            val newKey = generateRandomKey()
            val (encryptedKey, iv) = encryptKey(newKey)
            prefs.edit {
                putString(KEY_ENCRYPTED_DB_PASSWORD, encryptedKey)
                putString(KEY_IV, iv)
            }
            newKey
        }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        keyStore.getKey(MASTER_KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encryptKey(rawKey: String): Pair<String, String> {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val encryptedBytes = cipher.doFinal(rawKey.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP) to
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun decryptKey(encryptedKeyBase64: String, ivBase64: String): String {
        val encryptedBytes = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), spec)
        return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
    }

    private fun generateRandomKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private companion object {
        const val PREFS_NAME = "database_security_prefs"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val MASTER_KEY_ALIAS = "onlytechtake_db_master_key"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ENCRYPTED_DB_PASSWORD = "encrypted_db_password"
        const val KEY_IV = "db_password_iv"
    }
}
