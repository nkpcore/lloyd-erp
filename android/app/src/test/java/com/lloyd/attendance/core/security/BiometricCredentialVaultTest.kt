package com.lloyd.attendance.core.security

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BiometricCredentialVaultTest {

    private lateinit var fakePrefs: FakeSharedPreferences

    private val testCryptoEngine = object : CryptoEngine {
        override fun encrypt(plainText: String): EncryptedPayload {
            return EncryptedPayload(
                cipherTextBase64 = "encrypted_$plainText",
                ivBase64 = "test_iv_123"
            )
        }

        override fun decrypt(payload: EncryptedPayload): String? {
            if (payload.cipherTextBase64.startsWith("encrypted_")) {
                return payload.cipherTextBase64.removePrefix("encrypted_")
            }
            return null
        }
    }

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
    }

    @Test
    fun testSaveCredentials_persistsEncryptedPasswordAndUsername() {
        val vault = BiometricCredentialVault(
            sharedPrefs = fakePrefs,
            cryptoEngine = testCryptoEngine
        )

        assertFalse(vault.hasSavedCredentials())

        vault.saveCredentials("24GSOB1010001", "SecretPass#123")

        assertTrue(vault.hasSavedCredentials())
        assertEquals("24GSOB1010001", vault.getSavedUsername())
        assertEquals("SecretPass#123", vault.getSavedPassword())
    }

    @Test
    fun testClearCredentials_removesSavedValues() {
        val vault = BiometricCredentialVault(
            sharedPrefs = fakePrefs,
            cryptoEngine = testCryptoEngine
        )

        vault.saveCredentials("student_user", "MyStrongPass#")
        assertTrue(vault.hasSavedCredentials())

        vault.clearCredentials()
        assertFalse(vault.hasSavedCredentials())
        assertNull(vault.getSavedUsername())
        assertNull(vault.getSavedPassword())
    }

    @Test
    fun testAppLockToggle_persistsFlag() {
        val vault = BiometricCredentialVault(
            sharedPrefs = fakePrefs,
            cryptoEngine = testCryptoEngine
        )

        assertTrue(vault.isAppLockEnabled()) // default true

        vault.setAppLockEnabled(false)
        assertFalse(vault.isAppLockEnabled())

        vault.setAppLockEnabled(true)
        assertTrue(vault.isAppLockEnabled())
    }

    // Lightweight in-memory test doubles avoiding Android framework dependencies


    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)
        override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (map[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private val toRemove = mutableSetOf<String>()
            private var clear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) temp[key] = values
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) toRemove.add(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clear = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clear) storage.clear()
                toRemove.forEach { storage.remove(it) }
                temp.forEach { (k, v) -> storage[k] = v }
            }
        }
    }
}
