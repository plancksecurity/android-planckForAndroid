package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9

private const val MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION = 5

internal class BaseNotificationDataCreator {

    fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createBaseNotificationData(
        notificationData: NotificationData<Reference, Content>
    ): BaseNotificationData {
        val account = notificationData.account
        return BaseNotificationData(
            account = account,
            groupKey = NotificationGroupKeys.getNewMailGroupKey(account),
            accountName = account.name,
            color = account.chipColor,
            notificationsCount = notificationData.newMessagesCount,
            lockScreenNotificationData = createLockScreenNotificationData(notificationData),
            appearance = createNotificationAppearance(account)
        )
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createLockScreenNotificationData(
        data: NotificationData<Reference, Content>
    ): LockScreenNotificationData {
        return when (K9.getLockScreenNotificationVisibility()) {
            K9.LockScreenNotificationVisibility.APP_NAME -> LockScreenNotificationData.AppName
            K9.LockScreenNotificationVisibility.EVERYTHING -> LockScreenNotificationData.Public
            K9.LockScreenNotificationVisibility.MESSAGE_COUNT -> LockScreenNotificationData.MessageCount
            K9.LockScreenNotificationVisibility.SENDERS -> LockScreenNotificationData.SenderNames(getSenderNames(data))
            else -> LockScreenNotificationData.None
        }
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> getSenderNames(
        data: NotificationData<Reference, Content>
    ): String {
        return data.activeNotifications.asSequence()
            .map { it.content.sender }
            .distinct()
            .take(MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION)
            .joinToString()
    }

    private fun createNotificationAppearance(account: Account): NotificationAppearance {
        return with(account.notificationSetting) {
            val vibrationPattern = vibration.takeIf { isVibrateEnabled }
            NotificationAppearance(ringtone, vibrationPattern, account.ledColor)
        }
    }

    private val Account.ledColor: Int?
        get() = if (notificationSetting.isLedEnabled) notificationSetting.ledColor else null
}