package com.fsck.k9.fragment


import android.database.Cursor
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.fragment.MLFProjectionInfo.SENDER_LIST_COLUMN
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.pEp.withContextCatchException
import kotlinx.coroutines.*
import timber.log.Timber


object MlfUtils {

    @Throws(MessagingException::class)
    @JvmStatic
    fun getOpenFolder(
        folderName: String,
        account: Account
    ): LocalFolder {
        val localStore = account.localStore
        val localFolder = localStore.getFolder(folderName)
        runBlocking {
            open(localFolder)
        }
        return localFolder
    }

    @JvmStatic
    fun getOpenFolderWithCallback(folderName: String,
        account: Account,
        callback: (localFolder: LocalFolder) -> Unit?
    ) {
        val localStore = account.localStore
        val localFolder = localStore.getFolder(folderName)
        val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val outerStackTrace = Exception().stackTrace
        coroutineScope.launch {
            open(localFolder, outerStackTrace)
            callback.invoke(localFolder)
        }
    }

    private suspend fun open(
        localFolder: LocalFolder,
        externalTrace: Array<StackTraceElement> = arrayOf()
    ) = withContextCatchException<Unit, MessagingException>(Dispatchers.IO, externalTrace,
            tryBlock = {
                localFolder.open(Folder.OPEN_MODE_RO)
            }, catchBlock = {exception -> throw RuntimeException(exception)})



    @JvmStatic
    fun setLastSelectedFolderName(
        preferences: Preferences,
        messages: List<MessageReference>,
        destFolderName: String
    ) {
        try {
            val firstMsg = messages[0]
            val account = preferences.getAccount(firstMsg.accountUuid)
            val firstMsgFolder = getOpenFolder(firstMsg.folderName, account)
            firstMsgFolder.setLastSelectedFolderName(destFolderName)
        } catch (e: MessagingException) {
            Timber.e(e, "Error getting folder for setLastSelectedFolderName()")
        }
    }

    @JvmStatic
    fun getSenderAddressFromCursor(cursor: Cursor): String? {
        val fromList = cursor.getString(SENDER_LIST_COLUMN)
        val fromAddress = Address.unpack(fromList)
        return if (fromAddress.isNotEmpty()) fromAddress[0].address else null
    }

    @JvmStatic
    fun buildSubject(
        subjectFromCursor: String,
        emptySubject: String,
        threadCount: Int
    ): String {
        if (subjectFromCursor.isEmpty()) {
            return emptySubject
        } else if (threadCount > 1) {
            // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
            return Utility.stripSubject(subjectFromCursor)
        }
        return subjectFromCursor
    }

    @JvmStatic
    fun getFolderById(
        account: Account,
        folderId: Long
    ): LocalFolder {
        try {
            val localStore = account.localStore
            val localFolder = localStore.getFolderById(folderId)
            localFolder.open(Folder.OPEN_MODE_RO)
            return localFolder
        } catch (e: MessagingException) {
            throw RuntimeException(e)
        }
    }
}
