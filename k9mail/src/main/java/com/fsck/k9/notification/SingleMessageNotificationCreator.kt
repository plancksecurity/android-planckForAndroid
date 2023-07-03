package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference

internal class SingleMessageNotificationCreator(
    notificationHelper: NotificationHelper,
    actionCreator: NotificationActionCreator,
    resourceProvider: NotificationResourceProvider,
    lockScreenNotificationCreator: LockScreenNotificationCreator
) : SingleGroupedNotificationCreator<MessageReference, NewMailNotificationContent>(
    notificationHelper,
    actionCreator,
    resourceProvider,
    lockScreenNotificationCreator,
) {
    override fun getNotificationBuilder(
        account: Account,
        singleNotificationData: SingleNotificationData<NewMailNotificationContent>
    ): NotificationCompat.Builder {
        val content = singleNotificationData.content
        return notificationHelper.createNotificationBuilder(account, NotificationChannelManager.ChannelType.MESSAGES)
            .setTicker(content.summary)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setContentTitle(content.sender)
            .setContentText(content.subject)
            .setBigText(content.preview)
            .setContentIntent(actionCreator.createViewMessagePendingIntent(content.reference))
            .setDeleteIntent(actionCreator.createDismissMessagePendingIntent(content.reference))
            .setDeviceActions(singleNotificationData)
            .setWearActions(singleNotificationData)
    }

    private fun NotificationCompat.Builder.setDeviceActions(notificationData: SingleNotificationData<NewMailNotificationContent>) = apply {
        val actions = notificationData.actions
        for (action in actions) {
            when (action) {
                NotificationAction.Reply -> addReplyAction(notificationData)
                NotificationAction.MarkAsRead -> addMarkAsReadAction(notificationData)
                NotificationAction.Delete -> addDeleteAction(notificationData)
            }
        }
    }

    private fun NotificationCompat.Builder.addReplyAction(notificationData: SingleNotificationData<NewMailNotificationContent>) {
        val icon = resourceProvider.iconReply
        val title = resourceProvider.actionReply()
        val content = notificationData.content
        val messageReference = content.reference
        val replyToMessagePendingIntent = actionCreator.createReplyPendingIntent(messageReference as MessageReference)

        addAction(icon, title, replyToMessagePendingIntent)
    }

    private fun NotificationCompat.Builder.addMarkAsReadAction(notificationData: SingleNotificationData<NewMailNotificationContent>) {
        val icon = resourceProvider.iconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val content = notificationData.content
        val messageReference = content.reference
        val action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference as MessageReference)

        addAction(icon, title, action)
    }

    private fun NotificationCompat.Builder.addDeleteAction(notificationData: SingleNotificationData<NewMailNotificationContent>) {
        val icon = resourceProvider.iconDelete
        val title = resourceProvider.actionDelete()
        val content = notificationData.content
        val messageReference = content.reference as MessageReference
        val action = actionCreator.createDeleteMessagePendingIntent(messageReference)

        addAction(icon, title, action)
    }

    private fun NotificationCompat.Builder.setWearActions(notificationData: SingleNotificationData<NewMailNotificationContent>) = apply {
        val wearableExtender = NotificationCompat.WearableExtender().apply {
            for (action in notificationData.wearActions) {
                when (action) {
                    WearNotificationAction.Reply -> addReplyAction(notificationData)
                    WearNotificationAction.MarkAsRead -> addMarkAsReadAction(notificationData)
                    WearNotificationAction.Delete -> addDeleteAction(notificationData)
                    WearNotificationAction.Archive -> addArchiveAction(notificationData)
                    WearNotificationAction.Spam -> addMarkAsSpamAction(notificationData)
                }
            }
        }

        extend(wearableExtender)
    }

    private fun NotificationCompat.WearableExtender.addReplyAction(notificationData: SingleNotificationData<NewMailNotificationContent>) {
        val icon = resourceProvider.wearIconReplyAll
        val title = resourceProvider.actionReply()
        val messageReference = notificationData.content.reference
        val action = actionCreator.createReplyPendingIntent(messageReference as MessageReference)
        val replyAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(replyAction)
    }

    private fun NotificationCompat.WearableExtender.addMarkAsReadAction(notificationData: SingleNotificationData<NewMailNotificationContent>) {
        val icon = resourceProvider.wearIconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val messageReference = notificationData.content.reference
        val action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference as MessageReference)
        val markAsReadAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(markAsReadAction)
    }

    private fun NotificationCompat.WearableExtender.addDeleteAction(notificationData: SingleNotificationData<NewMailNotificationContent>) {
        val icon = resourceProvider.wearIconDelete
        val title = resourceProvider.actionDelete()
        val messageReference = notificationData.content.reference as MessageReference
        val action = actionCreator.createDeleteMessagePendingIntent(messageReference)
        val deleteAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(deleteAction)
    }

    private fun NotificationCompat.WearableExtender.addArchiveAction(notificationData: SingleNotificationData<NewMailNotificationContent>) {
        val icon = resourceProvider.wearIconArchive
        val title = resourceProvider.actionArchive()
        val messageReference = notificationData.content.reference as MessageReference
        val action = actionCreator.createArchiveMessagePendingIntent(messageReference)
        val archiveAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(archiveAction)
    }

    private fun NotificationCompat.WearableExtender.addMarkAsSpamAction(notificationData: SingleNotificationData<NewMailNotificationContent>) {
        val icon = resourceProvider.wearIconMarkAsSpam
        val title = resourceProvider.actionMarkAsSpam()
        val messageReference = notificationData.content.reference as MessageReference
        val action = actionCreator.createMarkMessageAsSpamPendingIntent(messageReference)
        val spamAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(spamAction)
    }
}