package com.fsck.k9.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.core.app.TaskStackBuilder
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.FolderList
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.activity.MessageList
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.activity.NotificationDeleteConfirmation
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.activity.SettingsActivity.Companion.createUnreadSearch
import com.fsck.k9.activity.compose.MessageActions
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.helper.PendingIntentCompat.FLAG_IMMUTABLE
import com.fsck.k9.search.LocalSearch
import security.planck.notification.GroupMailInvite

/**
 * This class contains methods to create the [PendingIntent]s for the actions of our notifications.
 *
 * **Note:**
 * We need to take special care to ensure the `PendingIntent`s are unique as defined in the documentation of
 * [PendingIntent]. Otherwise selecting a notification action might perform the action on the wrong message.
 *
 * We add unique values to `Intent.data` so we end up with unique `PendingIntent`s.
 *
 * In the past we've used the notification ID as `requestCode` argument when creating a `PendingIntent`. But since we're
 * reusing notification IDs, it's safer to make sure the `Intent` itself is unique.
 */
internal open class NotificationActionCreator(
    private val context: Context,
    private val dataCreator: NotificationIntentDataCreator = NotificationIntentDataCreator()
) {
    fun getEditServerSettingsIntent(account: Account, incoming: Boolean): PendingIntent {
        val editServerSettingsIntent =
            if (incoming) AccountSetupBasics.intentActionEditIncomingSettings(
                context, account.uuid
            ) else AccountSetupBasics.intentActionEditOutgoingSettings(
                context, account.uuid
            )
        return PendingIntent.getActivity(
            context, account.accountNumber, editServerSettingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createMessageComposePendingIntent(messageReference: MessageReference): PendingIntent? {
        val stack = buildMessageComposeBackStack(messageReference)
        return stack.getPendingIntent(
            0,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_ONE_SHOT
        )
    }

    fun createViewMessagePendingIntent(messageReference: MessageReference): PendingIntent? {
        val stack = buildMessageViewBackStack(messageReference)
        return stack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    fun createViewFolderPendingIntent(account: Account, folderName: String): PendingIntent? {
        val stack = buildMessageListBackStack(account, folderName)
        return stack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    fun createViewMessagesPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>
    ): PendingIntent? {
        val stack: TaskStackBuilder = if (account.goToUnreadMessageSearch()) {
            buildUnreadBackStack(account)
        } else {
            val folderName = getFolderNameOfAllMessages(messageReferences)
            folderName?.let { buildMessageListBackStack(account, it) } ?: buildMessageListBackStack(
                account, account.autoExpandFolderName
            )
        }
        return stack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    fun createDismissAllMessagesPendingIntent(account: Account): PendingIntent? {
        val intent = NotificationActionService.createDismissAllMessagesIntent(
            context, account
        ).apply {
            data = dataCreator.getDismissAllMessagesData(account)
        }
        return PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createDismissMessagePendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createDismissMessageIntent(
            context, messageReference
        ).apply {
            data = dataCreator.getDismissMessageData(messageReference)
        }
        return PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createDismissGroupMailNotificationPendingIntent(groupMailInvite: GroupMailInvite): PendingIntent {
        val intent = NotificationActionService.createDismissGroupMailNotificationIntent(
            context, groupMailInvite
        ).apply {
            data = dataCreator.getDismissGroupMailData(groupMailInvite)
        }
        return PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createDismissAllGroupMailNotificationsPendingIntent(account: Account): PendingIntent {
        val intent = NotificationActionService.createDismissAllGroupMailNotificationsIntent(
            context, account
        ).apply {
            data = dataCreator.getDismissAllGroupMailData(account)
        }
        return PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createReplyPendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = MessageActions.getActionReplyIntent(context, messageReference).apply {
            data = dataCreator.getReplyMessageData(messageReference)
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createMarkMessageAsReadPendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createMarkMessageAsReadIntent(
            context, messageReference
        ).apply {
            data = dataCreator.getMarkMessageAsReadData(messageReference)
        }
        return PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createMarkAllAsReadPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>
    ): PendingIntent {
        val intent = NotificationActionService.createMarkAllAsReadIntent(context, account.uuid, messageReferences).apply {
            data = dataCreator.getMarkAllMessagesAsReadData(account)
        }
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    fun createDeleteMessagePendingIntent(messageReference: MessageReference): PendingIntent? {
        return if (K9.confirmDeleteFromNotification()) {
            createDeleteConfirmationPendingIntent(messageReference)
        } else {
            createDeleteServicePendingIntent(messageReference)
        }
    }

    private fun createDeleteServicePendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createDeleteMessageIntent(
            context, messageReference
        ).apply {
            data = dataCreator.getDeleteMessageData(messageReference)
        }
        return PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    private fun createDeleteConfirmationPendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationDeleteConfirmation.getIntent(context, messageReference).apply {
            data = dataCreator.getDeleteMessageConfirmationData(messageReference)
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createDeleteAllPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>
    ): PendingIntent {
        return if (K9.confirmDeleteFromNotification()) {
            getDeleteAllConfirmationPendingIntent(
                messageReferences,
                PendingIntent.FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
            )
        } else {
            getDeleteAllServicePendingIntent(
                account, messageReferences,
                PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
            )
        }
    }

    private fun getDeleteAllConfirmationPendingIntent(
        messageReferences: List<MessageReference>,
        flags: Int
    ): PendingIntent {
        val intent = NotificationDeleteConfirmation.getIntent(context, messageReferences).apply {
            data = dataCreator.getDeleteAllMessageConfirmationData()
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun getDeleteAllServicePendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        flags: Int
    ): PendingIntent {
        val intent = NotificationActionService.createDeleteAllMessagesIntent(
            context, account.uuid, messageReferences
        ).apply {
            data = dataCreator.getDeleteAllMessagesData(account)
        }
        return PendingIntent.getService(context, 0, intent, flags)
    }

    fun createArchiveMessagePendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createArchiveMessageIntent(
            context, messageReference
        ).apply {
            data = dataCreator.getArchiveMessageData(messageReference)
        }
        return PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createArchiveAllPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>
    ): PendingIntent {
        val intent = NotificationActionService.createArchiveAllIntent(
            context, account, messageReferences
        ).apply {
            data = dataCreator.getArchiveAllMessagesData(account)
        }
        return PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    fun createMarkMessageAsSpamPendingIntent(messageReference: MessageReference): PendingIntent {
        val intent = NotificationActionService.createMarkMessageAsSpamIntent(
            context, messageReference
        ).apply {
            data = Uri.parse("data:,spam/${messageReference.toIdentityString()}")
        }
        return PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
    }

    private fun buildAccountsBackStack(): TaskStackBuilder {
        val stack = TaskStackBuilder.create(
            context
        )
        if (!skipAccountsInBackStack()) {
            val intent = Intent(context, SettingsActivity::class.java)
            intent.putExtra(SettingsActivity.EXTRA_STARTUP, false)
            stack.addNextIntent(intent)
        }
        return stack
    }

    private fun buildFolderListBackStack(account: Account): TaskStackBuilder {
        val stack = buildAccountsBackStack()
        val intent = FolderList.actionHandleAccountIntent(context, account, false)
        stack.addNextIntent(intent)
        return stack
    }

    private fun buildUnreadBackStack(account: Account): TaskStackBuilder {
        val stack = buildAccountsBackStack()
        val search = createUnreadSearch(context, account)
        val intent = MessageList.intentDisplaySearch(context, search, true, false, false)
        stack.addNextIntent(intent)
        return stack
    }

    private fun buildMessageListBackStack(account: Account, folderName: String): TaskStackBuilder {
        val stack = if (skipFolderListInBackStack(
                account,
                folderName
            )
        ) buildAccountsBackStack() else buildFolderListBackStack(account)
        val search = LocalSearch(folderName)
        search.addAllowedFolder(folderName)
        search.addAccountUuid(account.uuid)
        val intent = MessageList.intentDisplaySearch(context, search, false, true, true)
        stack.addNextIntent(intent)
        return stack
    }

    private fun buildMessageViewBackStack(message: MessageReference): TaskStackBuilder {
        val account = Preferences.getPreferences(context).getAccount(message.accountUuid)
        val folderName = message.folderName
        val stack = buildMessageListBackStack(account, folderName)
        val intent = MessageList.actionDisplayMessageIntent(context, message)
        stack.addNextIntent(intent)
        return stack
    }

    private fun buildMessageComposeBackStack(message: MessageReference): TaskStackBuilder {
        val account = Preferences.getPreferences(context).getAccount(message.accountUuid)
        val folderName = message.folderName
        val stack = buildMessageListBackStack(account, folderName)
        val intent = MessageCompose.actionEditDraftIntent(context, message)
        stack.addNextIntent(intent)
        return stack
    }

    private fun getFolderNameOfAllMessages(messageReferences: List<MessageReference>): String? {
        if (messageReferences.isEmpty()) return null
        val firstMessage = messageReferences.first()
        val folderName = firstMessage.folderName
        for (messageReference in messageReferences) {
            if (!TextUtils.equals(folderName, messageReference.folderName)) {
                return null
            }
        }
        return folderName
    }

    private fun skipFolderListInBackStack(account: Account, folderName: String?): Boolean {
        return folderName != null && folderName == account.autoExpandFolderName
    }

    private fun skipAccountsInBackStack(): Boolean {
        return Preferences.getPreferences(context).accounts.size == 1
    }
}