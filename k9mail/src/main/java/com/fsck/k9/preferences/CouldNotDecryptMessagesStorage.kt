package com.fsck.k9.preferences

import android.content.Context
import android.content.SharedPreferences

class CouldNotDecryptMessagesStorage(val context: Context) {
    private val couldNotDecryptMessagesPreferences: SharedPreferences =
        context.getSharedPreferences(
            COULD_NOT_DECRYPT_MESSAGES_PREFERENCES,
            Context.MODE_PRIVATE
        )
    private val couldNotDecryptMessages: MutableSet<String> =
        couldNotDecryptMessagesPreferences.getString(COULD_NOT_DECRYPT_MESSAGES, null)
            .orEmpty().split(",").filter { it.isNotBlank() }.toMutableSet()


    fun getEncryptedMessages(): Set<String> {
        return couldNotDecryptMessages
    }

    @Synchronized
    fun addMessageId(messageId: String): Boolean {
        couldNotDecryptMessages.add(messageId)
        return couldNotDecryptMessagesPreferences
            .edit()
            .putString(COULD_NOT_DECRYPT_MESSAGES, couldNotDecryptMessages.joinToString(","))
            .commit()
    }

    @Synchronized
    fun removeMessageId(messageId: String): Boolean {
        couldNotDecryptMessages.remove(messageId)
        return couldNotDecryptMessagesPreferences
            .edit()
            .putString(COULD_NOT_DECRYPT_MESSAGES, couldNotDecryptMessages.joinToString(","))
            .commit()
    }

    private fun getMutableSet(): MutableSet<String> {
        return couldNotDecryptMessagesPreferences.getString(COULD_NOT_DECRYPT_MESSAGES, null)
            .orEmpty().split(",").filter { it.isNotBlank() }.toMutableSet()
    }

    companion object {
        const val COULD_NOT_DECRYPT_MESSAGES_PREFERENCES = "could_not_decrypt_messages_preferences"
        const val COULD_NOT_DECRYPT_MESSAGES = "ONGOING_DECRYPT_MESSAGES"
    }

}