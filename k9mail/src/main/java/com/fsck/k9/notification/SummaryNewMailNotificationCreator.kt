package com.fsck.k9.notification

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference

internal class SummaryNewMailNotificationCreator(
    notificationHelper: NotificationHelper,
    actionCreator: NotificationActionCreator,
    lockScreenNotificationCreator: LockScreenNotificationCreator,
    singleNotificationCreator: SingleGroupedNotificationCreator<MessageReference>,
    resourceProvider: NotificationResourceProvider
): SummaryGroupedNotificationCreator<MessageReference>(
    notificationHelper,
    actionCreator,
    lockScreenNotificationCreator,
    singleNotificationCreator,
    resourceProvider
) {
    override fun getNotificationBuilder(
        account: Account,
        notificationData: SummaryInboxNotificationData<MessageReference>,
        baseNotificationData: BaseNotificationData,
        summary: String
    ): NotificationCompat.Builder {
        val notificationsCount = baseNotificationData.notificationsCount
        val title = resourceProvider.newMessagesTitle(notificationsCount)
        return notificationHelper.createNotificationBuilder(account, NotificationChannelManager.ChannelType.MESSAGES)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setContentTitle(title)
            .setInboxStyle(title, summary, notificationData.content)
            .setContentIntent(createViewIntent(account, notificationData.references as List<MessageReference>))
            .setDeleteIntent(actionCreator.createDismissAllMessagesPendingIntent(account))
            .setDeviceActions(account, notificationData)
            .setWearActions(account, notificationData)
    }

    private fun createViewIntent(account: Account, references: List<MessageReference>): PendingIntent {
        return actionCreator.createViewMessagesPendingIntent(account, references)
    }

    override fun getInboxSummaryText(accountName: String, count: Int): String {
        return resourceProvider.additionalMessages(count, accountName)
    }

    private fun NotificationCompat.Builder.setDeviceActions(
        account: Account,
        notificationData: SummaryInboxNotificationData<MessageReference>
    ) = apply {
        for (action in notificationData.actions) {
            when (action) {
                SummaryNotificationAction.MarkAsRead -> addMarkAllAsReadAction(account, notificationData)
                SummaryNotificationAction.Delete -> addDeleteAllAction(account, notificationData)
            }
        }
    }

    private fun NotificationCompat.Builder.addMarkAllAsReadAction(
        account: Account,
        notificationData: SummaryInboxNotificationData<MessageReference>
    ) {
        val icon = resourceProvider.iconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val messageReferences = notificationData.references
        val markAllAsReadPendingIntent = actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences)

        addAction(icon, title, markAllAsReadPendingIntent)
    }

    private fun NotificationCompat.Builder.addDeleteAllAction(
        account: Account,
        notificationData: SummaryInboxNotificationData<MessageReference>
    ) {
        val icon = resourceProvider.iconDelete
        val title = resourceProvider.actionDelete()
        val messageReferences = notificationData.references
        val action = actionCreator.createDeleteAllPendingIntent(account, messageReferences)

        addAction(icon, title, action)
    }

    private fun NotificationCompat.Builder.setWearActions(
        account: Account,
        notificationData: SummaryInboxNotificationData<MessageReference>
    ) = apply {
        val wearableExtender = NotificationCompat.WearableExtender().apply {
            for (action in notificationData.wearActions) {
                when (action) {
                    SummaryWearNotificationAction.MarkAsRead -> addMarkAllAsReadAction(account, notificationData)
                    SummaryWearNotificationAction.Delete -> addDeleteAllAction(account, notificationData)
                    SummaryWearNotificationAction.Archive -> addArchiveAllAction(account, notificationData)
                }
            }
        }

        extend(wearableExtender)
    }

    private fun NotificationCompat.WearableExtender.addMarkAllAsReadAction(
        account: Account,
        notificationData: SummaryInboxNotificationData<MessageReference>
    ) {
        val icon = resourceProvider.wearIconMarkAsRead
        val title = resourceProvider.actionMarkAllAsRead()
        val messageReferences = notificationData.references
        val action = actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences)
        val markAsReadAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(markAsReadAction)
    }

    private fun NotificationCompat.WearableExtender.addDeleteAllAction(account: Account, notificationData: SummaryInboxNotificationData<MessageReference>) {
        val icon = resourceProvider.wearIconDelete
        val title = resourceProvider.actionDeleteAll()
        val messageReferences = notificationData.references
        val action = actionCreator.createDeleteAllPendingIntent(account, messageReferences)
        val deleteAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(deleteAction)
    }

    private fun NotificationCompat.WearableExtender.addArchiveAllAction(account: Account, notificationData: SummaryInboxNotificationData<MessageReference>) {
        val icon = resourceProvider.wearIconArchive
        val title = resourceProvider.actionArchiveAll()
        val messageReferences = notificationData.references
        val action = actionCreator.createArchiveAllPendingIntent(account, messageReferences)
        val archiveAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(archiveAction)
    }
}