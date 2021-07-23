package com.fsck.k9.preferences

import android.content.Context
import android.content.SharedPreferences

const val FAILED_TO_DECRYPT_PREFERENCES = "FAILED_TO_DECRYPT_PREFERENCES"
const val FAILED_TO_DECRYPT_MESSAGES = "FAILED_TO_DECRYPT_MESSAGES"

class FailedToDecryptPreferences(val context: Context) {
    private val failedToDecryptPreferences: SharedPreferences = context.getSharedPreferences(
        FAILED_TO_DECRYPT_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val failedToDecryptMessages: MutableSet<String> =
        failedToDecryptPreferences.getStringSet(FAILED_TO_DECRYPT_MESSAGES, null)
            ?: mutableSetOf()

    fun getFailedToDecryptMessages(): Set<String?> {
        return failedToDecryptMessages
    }

    @Synchronized
    fun addMessageId(id: String): Boolean {
        failedToDecryptMessages.add(id)
        return failedToDecryptPreferences
            .edit()
            .putStringSet(FAILED_TO_DECRYPT_MESSAGES, failedToDecryptMessages)
            .commit()
    }

    @Synchronized
    fun removeMessageId(id: String): Boolean {
        failedToDecryptMessages.remove(id)
        return failedToDecryptPreferences
            .edit()
            .putStringSet(FAILED_TO_DECRYPT_MESSAGES, failedToDecryptMessages)
            .commit()
    }
}