package com.lloyd.attendance.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreTokenStore(private val context: Context) : SecureTokenStore {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val sharedPrefs = context.getSharedPreferences("lloyd_secure_vault", Context.MODE_PRIVATE)

    @Volatile
    private var cachedAccessToken: String? = null

    init {
        getOrCreateKey()
    }

    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) return existingKey.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun getAccessToken(): String? = cachedAccessToken

    override fun setAccessToken(token: String?) {
        cachedAccessToken = token
    }

    override fun getRefreshToken(): String? {
        val encrypted = sharedPrefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val iv = sharedPrefs.getString(KEY_IV, null) ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            val decoded = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    override fun setRefreshToken(token: String?) {
        if (token.isNullOrEmpty()) {
            sharedPrefs.edit().remove(KEY_REFRESH_TOKEN).remove(KEY_IV).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))

        sharedPrefs.edit()
            .putString(KEY_REFRESH_TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    override fun getVerifiedStudentId(): Int = sharedPrefs.getInt(KEY_VERIFIED_STUDENT_ID, 0)

    override fun setVerifiedStudentId(id: Int) {
        sharedPrefs.edit().putInt(KEY_VERIFIED_STUDENT_ID, id).apply()
    }

    override fun clearTokens() {
        cachedAccessToken = null
        sharedPrefs.edit()
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_IV)
            .remove(KEY_VERIFIED_STUDENT_ID)
            .apply()
    }

    override fun hasValidRefreshToken(): Boolean = !getRefreshToken().isNullOrEmpty()

    companion object {
        private const val KEY_ALIAS = "lloyd_erp_auth_token_key"
        private const val KEY_REFRESH_TOKEN = "enc_r_token"
        private const val KEY_IV = "enc_iv"
        private const val KEY_VERIFIED_STUDENT_ID = "verified_student_id"
    }
}
