package com.fsck.k9.activity

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.mail.Folder
import com.fsck.k9.mailstore.LocalFolder

class FolderInfoHolder : Comparable<FolderInfoHolder> {
    @JvmField
    var name: String? = null

    @JvmField
    var displayName: String? = null

    @JvmField
    var lastChecked: Long = 0

    @JvmField
    var unreadMessageCount = -1

    @JvmField
    var flaggedMessageCount = -1

    @JvmField
    var loading = false

    @JvmField
    var status: String? = null

    @JvmField
    var lastCheckFailed = false

    @JvmField
    var folder: Folder<*>? = null

    @JvmField
    var pushActive = false

    @JvmField
    var moreMessages = false
    override fun equals(other: Any?): Boolean {
        return other is FolderInfoHolder && name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun compareTo(other: FolderInfoHolder): Int {
        val s1 = name ?: return -1
        val s2 = other.name ?: return 1
        val ret = s1.compareTo(s2, ignoreCase = true)
        return if (ret != 0) {
            ret
        } else {
            s1.compareTo(s2)
        }
    }

    private fun truncateStatus(message: String?): String? {
        var mess = message
        if (mess != null && mess.length > 27) {
            mess = mess.substring(0, 27)
        }
        return mess
    }

    // constructor for an empty object for comparisons
    constructor()
    constructor(context: Context?, folder: LocalFolder, account: Account) {
        requireNotNull(context) { "null context given" }
        populate(context, folder, account)
    }

    constructor(context: Context, folder: LocalFolder, account: Account, unreadCount: Int) {
        populate(context, folder, account, unreadCount)
    }

    fun populate(context: Context, folder: LocalFolder, account: Account, unreadCount: Int) {
        populate(context, folder, account)
        unreadMessageCount = unreadCount
        folder.close()
    }

    private fun populate(context: Context, folder: LocalFolder, account: Account) {
        this.folder = folder
        name = folder.name
        lastChecked = folder.lastUpdate
        status = truncateStatus(folder.status)
        displayName = getDisplayName(context, account, name)
        setMoreMessagesFromFolder(folder)
    }

    fun setMoreMessagesFromFolder(folder: LocalFolder) {
        moreMessages = folder.hasMoreMessages()
    }

    companion object {
        /**
         * Returns the display name for a folder.
         *
         *
         *
         * This will return localized strings for special folders like the Inbox or the Trash folder.
         *
         *
         * @param context
         * A [Context] instance that is used to get the string resources.
         * @param account
         * The [Account] the folder belongs to.
         * @param name
         * The name of the folder for which to return the display name.
         *
         * @return The localized name for the provided folder if it's a special folder or the original
         * folder name if it's a non-special folder.
         */
        @JvmStatic
        fun getDisplayName(context: Context, account: Account, name: String?): String? {
            return getDisplayName(context, account, name, name)
        }

        fun getDisplayName(
            context: Context,
            account: Account,
            name: String?,
            fqn: String?
        ): String? {
            // FIXME: We really shouldn't do a case-insensitive comparison here
            return when {
                fqn == account.spamFolderName ->
                    String.format(
                        context.getString(R.string.special_mailbox_name_spam_fmt), name
                    )

                fqn == account.archiveFolderName ->
                    String.format(
                        context.getString(R.string.special_mailbox_name_archive_fmt), name
                    )

                fqn == account.sentFolderName ->
                    String.format(
                        context.getString(R.string.special_mailbox_name_sent_fmt), name
                    )

                fqn == account.trashFolderName ->
                    String.format(
                        context.getString(R.string.special_mailbox_name_trash_fmt), name
                    )

                fqn == account.draftsFolderName ->
                    String.format(
                        context.getString(R.string.special_mailbox_name_drafts_fmt), name
                    )

                fqn == account.outboxFolderName ->
                    context.getString(R.string.special_mailbox_name_outbox)

                fqn.equals(account.inboxFolderName, ignoreCase = true) ->
                    context.getString(R.string.special_mailbox_name_inbox)

                fqn == account.planckSuspiciousFolderName ->
                    context.getString(R.string.special_mailbox_name_suspicious)

                else -> name
            }
        }
    }
}