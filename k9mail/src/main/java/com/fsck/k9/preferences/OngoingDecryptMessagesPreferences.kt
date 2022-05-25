package com.fsck.k9.preferences

import android.content.Context
import android.content.SharedPreferences

class OngoingDecryptMessagesPreferences(val context: Context) {
    private val ongoingDecryptMessagesPreferences: SharedPreferences = context.getSharedPreferences(
        ONGOING_DECRYPT_MESSAGES_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val ongoingDecryptMessages: MutableSet<String> =
        ongoingDecryptMessagesPreferences.getString(ONGOING_DECRYPT_MESSAGES, null)
            .orEmpty().split(",").filter { it.isNotBlank() }.toMutableSet()

    fun getOngoingDecryptMessages(): Set<String> {
        return ongoingDecryptMessages
    }

    @Synchronized
    fun addMessageId(messageId: String): Boolean {
        ongoingDecryptMessages.add(messageId)
        return ongoingDecryptMessagesPreferences
            .edit()
            .putString(ONGOING_DECRYPT_MESSAGES, ongoingDecryptMessages.joinToString(","))
            .commit()
    }

    @Synchronized
    fun removeMessageId(messageId: String): Boolean {
        ongoingDecryptMessages.remove(messageId)
        return ongoingDecryptMessagesPreferences
            .edit()
            .putString(ONGOING_DECRYPT_MESSAGES, ongoingDecryptMessages.joinToString(","))
            .commit()
    }

    companion object {
        const val ONGOING_DECRYPT_MESSAGES_PREFERENCES = "ONGOING_DECRYPT_MESSAGES_PREFERENCES"
        const val ONGOING_DECRYPT_MESSAGES = "ONGOING_DECRYPT_MESSAGES"
        const val DO_NOT_REMOVE_ID = "DO_NOT_REMOVE_ID"
    }

}