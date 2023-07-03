package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9

internal class BaseGroupMailNotificationDataCreator {

    fun createBaseNotificationData(notificationData: NotificationDataGroupMail): BaseNotificationData {
        val account = notificationData.account
        return BaseNotificationData(
            account = account,
            groupKey = NotificationGroupKeys.getGroupMailGroupKey(account),
            accountName = account.name,
            color = account.chipColor,
            notificationsCount = notificationData.notificationsCount,
            lockScreenNotificationData = createLockScreenNotificationData(notificationData),
            appearance = createNotificationAppearance(account)
        )
    }

    private fun createLockScreenNotificationData(data: NotificationDataGroupMail): LockScreenNotificationData {
        return when (K9.getLockScreenNotificationVisibility()) {
            K9.LockScreenNotificationVisibility.APP_NAME -> LockScreenNotificationData.AppName
            K9.LockScreenNotificationVisibility.EVERYTHING -> LockScreenNotificationData.Public
            K9.LockScreenNotificationVisibility.MESSAGE_COUNT -> LockScreenNotificationData.MessageCount
            else -> LockScreenNotificationData.None
        }
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