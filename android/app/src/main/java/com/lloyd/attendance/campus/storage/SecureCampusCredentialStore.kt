package com.lloyd.attendance.campus.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import com.lloyd.attendance.campus.CampusNetworkProfile
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keystore-backed AES-256-GCM secure storage exclusively for Campus Wi-Fi and Captive Portal credentials.
 *
 * Strict Security Boundaries:
 * 1. Hardware Keystore backed (AES-256-GCM).
 * 2. Completely isolated from ERP JWT tokens.
 * 3. Wi-Fi credentials are NEVER transmitted back to the Lloyd ERP cloud backend.
 */
class SecureCampusCredentialStore(context: Context) {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        getOrCreateKey()
    }

    private fun getOrCreateKey(): SecretKey {
        val existingEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) return existingEntry.secretKey

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

    @Synchronized
    private fun encrypt(plainText: String?): Pair<String?, String?> {
        if (plainText.isNullOrEmpty()) return Pair(null, null)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Pair(
                Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    @Synchronized
    private fun decrypt(encryptedBase64: String?, ivBase64: String?): String? {
        if (encryptedBase64.isNullOrEmpty() || ivBase64.isNullOrEmpty()) return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, Base64.decode(ivBase64, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            val decodedBytes = cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun saveCredentials(username: String, password: String) {
        val (encUser, ivUser) = encrypt(username)
        val (encPass, ivPass) = encrypt(password)

        prefs.edit()
            .putString(KEY_ENC_USER, encUser)
            .putString(KEY_IV_USER, ivUser)
            .putString(KEY_ENC_PASS, encPass)
            .putString(KEY_IV_PASS, ivPass)
            .apply()
    }

    fun getWifiUsername(): String? {
        val enc = prefs.getString(KEY_ENC_USER, null)
        val iv = prefs.getString(KEY_IV_USER, null)
        return decrypt(enc, iv)
    }

    fun getWifiPassword(): String? {
        val enc = prefs.getString(KEY_ENC_PASS, null)
        val iv = prefs.getString(KEY_IV_PASS, null)
        return decrypt(enc, iv)
    }

    fun hasCredentials(): Boolean {
        val user = getWifiUsername()
        val pass = getWifiPassword()
        return !user.isNullOrBlank() && !pass.isNullOrBlank()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_ENC_USER)
            .remove(KEY_IV_USER)
            .remove(KEY_ENC_PASS)
            .remove(KEY_IV_PASS)
            .apply()
    }

    var isAutoConnectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    var isAutoLoginEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOGIN, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LOGIN, value).apply()

    var lastLoginTimestamp: Long
        get() = prefs.getLong(KEY_LAST_LOGIN, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_LOGIN, value).apply()

    var lastSessionDurationMs: Long
        get() = prefs.getLong(KEY_LAST_SESSION_DURATION, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SESSION_DURATION, value).apply()

    var savedProfile: CampusNetworkProfile
        get() {
            val json = prefs.getString(KEY_SAVED_PROFILE, null) ?: return CampusNetworkProfile.DEFAULT_PROFILE
            return try {
                gson.fromJson(json, CampusNetworkProfile::class.java) ?: CampusNetworkProfile.DEFAULT_PROFILE
            } catch (e: Exception) {
                CampusNetworkProfile.DEFAULT_PROFILE
            }
        }
        set(value) {
            prefs.edit().putString(KEY_SAVED_PROFILE, gson.toJson(value)).apply()
        }

    companion object {
        private const val PREFS_NAME = "lloyd_campus_vault"
        private const val KEY_ALIAS = "lloyd_campus_key_alias"

        private const val KEY_ENC_USER = "enc_wifi_user"
        private const val KEY_IV_USER = "iv_wifi_user"
        private const val KEY_ENC_PASS = "enc_wifi_pass"
        private const val KEY_IV_PASS = "iv_wifi_pass"

        private const val KEY_AUTO_CONNECT = "pref_auto_connect"
        private const val KEY_AUTO_LOGIN = "pref_auto_login"
        private const val KEY_LAST_LOGIN = "pref_last_login_ts"
        private const val KEY_LAST_SESSION_DURATION = "pref_last_duration"
        private const val KEY_SAVED_PROFILE = "pref_saved_profile"

        @Volatile
        private var instance: SecureCampusCredentialStore? = null

        fun getInstance(context: Context): SecureCampusCredentialStore {
            return instance ?: synchronized(this) {
                instance ?: SecureCampusCredentialStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
