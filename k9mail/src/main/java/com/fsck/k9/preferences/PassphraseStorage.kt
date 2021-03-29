package com.fsck.k9.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.*
import timber.log.Timber


class PassphraseStorage(context: Context) {

    private var masterKeyAlias = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private lateinit var preferences: SharedPreferences

    init {
        createEncryptedSharedPreferences(context)
    }

    private fun createEncryptedSharedPreferences(context: Context) {
        runBlocking {
            createEncryptedSharedPreferencesInternal(context)
        }
    }

    private suspend fun createEncryptedSharedPreferencesInternal(context: Context) =
            withContext(Dispatchers.IO) {
        preferences = EncryptedSharedPreferences.create(
                context,
                SECRET_SHARED_PREFS_FILENAME,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


    fun putPassphrase(passphrase: String?) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(NEW_KEYS_PASSPHRASE_KEY, passphrase)
        editor.apply()
    }

    fun getPassphrase() = preferences.getString(NEW_KEYS_PASSPHRASE_KEY, "")

    companion object {
        private const val SECRET_SHARED_PREFS_FILENAME = "secret_preferences"
        private const val NEW_KEYS_PASSPHRASE_KEY = "pEpNewKeysPassphrase"
    }
}