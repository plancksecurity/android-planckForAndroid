package com.fsck.k9.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fsck.k9.mail.filter.Base64


class PassphraseStorage(context: Context) {

    var masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            context,
            SECRET_SHARED_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )


    fun putPassphrase(passphrase: String?) {
        passphrase?.let {
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString(NEW_KEYS_PASSPHRASE, passphrase)
            editor.apply()
        }
    }

    fun getPassphrase(): String? {
        val encrypted = sharedPreferences.getString(NEW_KEYS_PASSPHRASE, null)
        return Base64.decode(encrypted)
    }


    companion object {
        const val SECRET_SHARED_PREFS = "secret_shared_prefs"
        const val NEW_KEYS_PASSPHRASE = "pEpNewKeysPassphrase"
    }
}