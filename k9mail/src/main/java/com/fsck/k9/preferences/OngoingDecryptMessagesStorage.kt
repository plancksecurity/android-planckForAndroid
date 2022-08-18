package com.fsck.k9.preferences

import android.content.Context
import android.content.SharedPreferences

class OngoingDecryptMessagesStorage(val context: Context) {
    private val ongoingDecryptMessagesPreferences: SharedPreferences = context.getSharedPreferences(
        ONGOING_DECRYPT_MESSAGES_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val ongoingDecryptMessages: MutableSet<String> =
        getMutableSet(ONGOING_DECRYPT_MESSAGES)

    private val tempFilePaths: MutableSet<String> = getMutableSet(TEMPORARY_FILES)

    fun getOngoingDecryptMessages(): Set<String> {
        return ongoingDecryptMessages
    }

    fun getTempFilePaths(): Set<String> {
        return tempFilePaths
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

    @Synchronized
    fun addTempFilePaths(filePaths: Collection<String>): Boolean {
        tempFilePaths.addAll(filePaths)
        return ongoingDecryptMessagesPreferences
            .edit()
            .putString(TEMPORARY_FILES, tempFilePaths.joinToString(","))
            .commit()
    }

    @Synchronized
    fun clearTempFilePaths(): Boolean {
        tempFilePaths.clear()
        return ongoingDecryptMessagesPreferences
            .edit()
            .remove(TEMPORARY_FILES)
            .commit()
    }

    private fun getMutableSet(key: String): MutableSet<String> {
        return ongoingDecryptMessagesPreferences.getString(key, null)
            .orEmpty().split(",").filter { it.isNotBlank() }.toMutableSet()
    }

    companion object {
        const val ONGOING_DECRYPT_MESSAGES_PREFERENCES = "ongoing_decrypt_messages_preferences"
        const val ONGOING_DECRYPT_MESSAGES = "ONGOING_DECRYPT_MESSAGES"
        const val TEMPORARY_FILES = "TEMPORARY_FILES"
        const val REMOVE_FAILED_MESSAGE_ID = "REMOVE_FAILED_MESSAGE_ID"
    }

}