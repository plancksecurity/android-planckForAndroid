package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.WearableExtender
import com.fsck.k9.notification.NotificationChannelManager.ChannelType
import timber.log.Timber
import androidx.core.app.NotificationCompat.Builder as NotificationBuilder

internal class SingleGroupMailNotificationCreator(
    private val notificationHelper: NotificationHelper,
    //private val actionCreator: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider,
    private val lockScreenNotificationCreator: LockScreenNotificationCreator
) {
    fun createSingleNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: GroupMailSingleNotificationData,
        isGroupSummary: Boolean = false
    ) {
        val account = baseNotificationData.account
        val notificationId = singleNotificationData.notificationId
        val content = singleNotificationData.content

        val notification = notificationHelper.createNotificationBuilder(account, ChannelType.MESSAGES)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setGroup(baseNotificationData.groupKey)
            .setGroupSummary(isGroupSummary)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setColor(baseNotificationData.color)
            .setWhen(singleNotificationData.timestamp)
            .setTicker(content.groupAddress)
            .setContentTitle(content.senderAddress)
            .setContentText(content.senderAddress) // get this with the resource provider, a simple text
            .setSubText(baseNotificationData.accountName)
            .setBigText(content.senderAddress) // get this with the resource provider, a simple text
            //.setContentIntent(actionCreator.createViewMessagePendingIntent(content.messageReference))
            //.setDeleteIntent(actionCreator.createDismissMessagePendingIntent(content))
            //.setDeviceActions(singleNotificationData)
            //.setWearActions(singleNotificationData)
            .setAppearance(singleNotificationData.isSilent, baseNotificationData.appearance)
            .setLockScreenNotification(baseNotificationData, singleNotificationData.addLockScreenNotification)
            .build()

        if (isGroupSummary) {
            Timber.v(
                "Creating single summary notification (silent=%b): %s",
                singleNotificationData.isSilent,
                notification
            )
        }
        notificationHelper.notify(account, notificationId, notification)
    }

    private fun NotificationBuilder. setBigText(text: CharSequence) = apply {
        setStyle(NotificationCompat.BigTextStyle().bigText(text))
    }

    private fun NotificationBuilder.setLockScreenNotification(
        notificationData: BaseNotificationData,
        addLockScreenNotification: Boolean
    ) = apply {
        if (addLockScreenNotification) {
            lockScreenNotificationCreator.configureLockScreenNotification(this, notificationData)
        }
    }
}
