package com.lloyd.attendance.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedPayload(
    val cipherTextBase64: String,
    val ivBase64: String
)

interface CryptoEngine {
    fun encrypt(plainText: String): EncryptedPayload
    fun decrypt(payload: EncryptedPayload): String?
}

class AndroidKeystoreCryptoEngine(
    private val keyAlias: String = "lloyd_erp_credential_vault_key"
) : CryptoEngine {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun encrypt(plainText: String): EncryptedPayload {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            cipherTextBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    override fun decrypt(payload: EncryptedPayload): String? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val ivBytes = Base64.decode(payload.ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(payload.cipherTextBase64, Base64.NO_WRAP)
            val spec = GCMParameterSpec(128, ivBytes)

            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            val decoded = cipher.doFinal(cipherBytes)
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}

class BiometricCredentialVault(
    private val context: Context? = null,
    private val sharedPrefs: SharedPreferences = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        ?: throw IllegalArgumentException("Context or SharedPreferences must be provided"),
    private val cryptoEngine: CryptoEngine = AndroidKeystoreCryptoEngine()
) {

    fun saveCredentials(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) return

        val payload = cryptoEngine.encrypt(password)
        sharedPrefs.edit()
            .putString(KEY_SAVED_USERNAME, username.trim())
            .putString(KEY_ENC_PASSWORD, payload.cipherTextBase64)
            .putString(KEY_PASSWORD_IV, payload.ivBase64)
            .putBoolean(KEY_BIOMETRIC_LOGIN_ENABLED, true)
            .apply()
    }

    fun getSavedUsername(): String? {
        return sharedPrefs.getString(KEY_SAVED_USERNAME, null)?.takeIf { it.isNotBlank() }
    }

    fun getSavedPassword(): String? {
        val enc = sharedPrefs.getString(KEY_ENC_PASSWORD, null) ?: return null
        val iv = sharedPrefs.getString(KEY_PASSWORD_IV, null) ?: return null
        return cryptoEngine.decrypt(EncryptedPayload(enc, iv))
    }

    fun hasSavedCredentials(): Boolean {
        val user = getSavedUsername()
        val hasEnc = sharedPrefs.contains(KEY_ENC_PASSWORD) && sharedPrefs.contains(KEY_PASSWORD_IV)
        return !user.isNullOrBlank() && hasEnc
    }

    fun clearCredentials() {
        sharedPrefs.edit()
            .remove(KEY_SAVED_USERNAME)
            .remove(KEY_ENC_PASSWORD)
            .remove(KEY_PASSWORD_IV)
            .apply()
    }

    fun isBiometricLoginEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_BIOMETRIC_LOGIN_ENABLED, true)
    }

    fun setBiometricLoginEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_BIOMETRIC_LOGIN_ENABLED, enabled).apply()
    }

    fun isAppLockEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_APP_LOCK_ENABLED, true)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    companion object {
        const val PREF_NAME = "lloyd_biometric_vault_prefs"
        const val KEY_SAVED_USERNAME = "vault_username"
        const val KEY_ENC_PASSWORD = "vault_enc_password"
        const val KEY_PASSWORD_IV = "vault_password_iv"
        const val KEY_BIOMETRIC_LOGIN_ENABLED = "vault_bio_login_enabled"
        const val KEY_APP_LOCK_ENABLED = "vault_app_lock_enabled"

        @Volatile
        private var instance: BiometricCredentialVault? = null

        @JvmStatic
        fun getInstance(context: Context): BiometricCredentialVault {
            return instance ?: synchronized(this) {
                instance ?: BiometricCredentialVault(context.applicationContext).also { instance = it }
            }
        }
    }
}
