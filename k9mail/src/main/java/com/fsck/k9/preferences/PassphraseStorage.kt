package com.fsck.k9.preferences

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fsck.k9.K9
import com.fsck.k9.pEp.PEpUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import security.pEp.ui.passphrase.requestPassphraseForNewKeysBrokenKey
import timber.log.Timber
import java.security.KeyStoreException
import java.security.UnrecoverableKeyException

const val ENCRYPTED_PREFERENCES_PATH = "/shared_prefs/secret_preferences.xml"

class PassphraseStorage(val context: Context) {

    private var masterKeyAlias = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private lateinit var preferences: SharedPreferences

    var dataCorrupted = false

    init {
        createEncryptedSharedPreferences(context)
    }

    private fun createEncryptedSharedPreferences(context: Context) {
        runBlocking {
            createEncryptedSharedPreferencesInternal(context)
        }
    }

    private suspend fun createEncryptedSharedPreferencesInternal(context: Context) {
        try {
            withContext(Dispatchers.IO) {
                preferences = EncryptedSharedPreferences.create(
                    context,
                    SECRET_SHARED_PREFS_FILENAME,
                    masterKeyAlias,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                dataCorrupted = false
            }
        } catch (e: Exception) {
            when (e) {
                is KeyStoreException,
                is UnrecoverableKeyException -> {
                    dataCorrupted = true
                }
            }
        }
    }

    fun resetEncryptedSharedPreferences() {
        PEpUtils.removeEncryptedSharedPreferencesFile(context)
        createEncryptedSharedPreferences(context)
        dataCorrupted = false
    }

    fun brokenKeyStore(activity: Activity): Boolean {
        return if (dataCorrupted) {
            if (K9.ispEpUsePassphraseForNewKeys()) {
                activity.requestPassphraseForNewKeysBrokenKey()
            } else {
                resetEncryptedSharedPreferences()
            }
            true
        } else {
            false
        }
    }

    fun putPassphrase(passphrase: String?) {
        try {
            if (!::preferences.isInitialized) {
                throw UninitializedPropertyAccessException("Preferences not initialized")
            }
            val editor: SharedPreferences.Editor = preferences.edit()
            editor.putString(NEW_KEYS_PASSPHRASE_KEY, passphrase)
            editor.apply()
        } catch (e: Exception) {
            Timber.e(e, "Could not put passphrase into PassphraseStorage")
        }
    }

    fun getPassphrase() = try {
        if (!::preferences.isInitialized) {
            throw UninitializedPropertyAccessException("Preferences not initialized")
        }
        preferences.getString(NEW_KEYS_PASSPHRASE_KEY, "")
    } catch (e: Exception) {
        Timber.e(e, "Could not get passphrase from PassphraseStorage")
        ""
    }

    companion object {
        private const val SECRET_SHARED_PREFS_FILENAME = "secret_preferences"
        private const val NEW_KEYS_PASSPHRASE_KEY = "pEpNewKeysPassphrase"
    }
}