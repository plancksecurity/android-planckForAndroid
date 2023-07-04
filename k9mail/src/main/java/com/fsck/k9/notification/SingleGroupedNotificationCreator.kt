package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.notification.NotificationChannelManager.ChannelType
import timber.log.Timber
import androidx.core.app.NotificationCompat.Builder as NotificationBuilder

internal abstract class SingleGroupedNotificationCreator<in Reference: NotificationReference>(
    protected val notificationHelper: NotificationHelper,
    protected val actionCreator: NotificationActionCreator,
    protected val resourceProvider: NotificationResourceProvider,
    private val lockScreenNotificationCreator: LockScreenNotificationCreator<Reference>
) {
    fun createSingleNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData<Reference>,
        isGroupSummary: Boolean = false
    ) {
        val account = baseNotificationData.account
        val notificationId = singleNotificationData.notificationId

        val notification = getNotificationBuilder(account, singleNotificationData)
            .setGroup(baseNotificationData.groupKey)
            .setGroupSummary(isGroupSummary)
            .setColor(baseNotificationData.color)
            .setWhen(singleNotificationData.timestamp)
            .setSubText(baseNotificationData.accountName)
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

    protected abstract fun getNotificationBuilder(
        account: Account,
        singleNotificationData: SingleNotificationData<Reference>
    ): NotificationCompat.Builder

    protected fun NotificationBuilder.setBigText(text: CharSequence) = apply {
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

