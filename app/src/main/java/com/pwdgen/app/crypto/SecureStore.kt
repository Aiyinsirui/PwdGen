package com.pwdgen.app.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Small encrypted key/value store (AES-256-GCM via Android Keystore).
 *
 * Used for:
 *  - the cloud access token (GitHub PAT),
 *  - the optional "remember master password" feature.
 *
 * Note on Android 8.0 (API 24): [EncryptedSharedPreferences] works, but the
 * MasterKey is backed by the Keystore which is available since API 23.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value.isNullOrEmpty()) remove(key) else putString(key, value)
        }.apply()
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "pwdgen_secure_prefs"
        const val KEY_CLOUD_TOKEN = "cloud_token"
        const val KEY_SAVED_MASTER = "saved_master"
        const val KEY_SITES_LOCK = "sites_lock"
    }
}