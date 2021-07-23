package com.fsck.k9.preferences

import android.content.Context
import android.content.SharedPreferences

class OngoingDecryptMessagesPreferences(val context: Context) {
    private val ongoingDecryptMessagesPreferences: SharedPreferences = context.getSharedPreferences(
        ONGOING_DECRYPT_MESSAGES_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val ongoingDecryptMessages: MutableSet<String> =
        ongoingDecryptMessagesPreferences.getStringSet(ONGOING_DECRYPT_MESSAGES, null)
            ?: mutableSetOf()

    fun getOngoingDecryptMessages(): Set<String> {
        return ongoingDecryptMessages
    }

    @Synchronized
    fun addOngoingDecryptMessageId(id: String): Boolean {
        ongoingDecryptMessages.add(id)
        return ongoingDecryptMessagesPreferences
            .edit()
            .putStringSet(ONGOING_DECRYPT_MESSAGES, ongoingDecryptMessages)
            .commit()
    }

    @Synchronized
    fun removeOngoingDecryptMessageId(id: String): Boolean {
        ongoingDecryptMessages.remove(id)
        return ongoingDecryptMessagesPreferences
            .edit()
            .putStringSet(ONGOING_DECRYPT_MESSAGES, ongoingDecryptMessages)
            .commit()
    }

    companion object {
        const val ONGOING_DECRYPT_MESSAGES_PREFERENCES = "ONGOING_DECRYPT_MESSAGES_PREFERENCES"
        const val ONGOING_DECRYPT_MESSAGES = "ONGOING_DECRYPT_MESSAGES"
        const val DONT_REMOVE_ID = "dontRemoveId"
    }

}