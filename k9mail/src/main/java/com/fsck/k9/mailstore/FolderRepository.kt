package com.fsck.k9.mailstore

import com.fsck.k9.Account

class FolderRepository(private val account: Account) {

    fun getRemoteFolders(): List<Folder> {
        val folders = account.localStore.getPersonalNamespaces(false)
        val outbox = account.outboxFolderName

        return folders
                .filter { it.name != outbox }
                .map { Folder(it.id, it.name, it.name, folderTypeOf(it)) }
    }

    private fun folderTypeOf(folder: LocalFolder) = when (folder.name) {
        account.inboxFolderName -> FolderType.INBOX
        account.sentFolderName -> FolderType.SENT
        account.trashFolderName -> FolderType.TRASH
        account.draftsFolderName -> FolderType.DRAFTS
        account.archiveFolderName -> FolderType.ARCHIVE
        account.spamFolderName -> FolderType.SPAM
        else -> FolderType.REGULAR
    }
}

data class Folder(val id: Long, val serverId: String, val name: String, val type: FolderType)

enum class FolderType {
    REGULAR,
    INBOX,
    SENT,
    TRASH,
    DRAFTS,
    ARCHIVE,
    SPAM
}
