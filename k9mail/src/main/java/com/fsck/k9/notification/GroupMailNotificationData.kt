package com.fsck.k9.notification

import com.fsck.k9.Account
import security.planck.notification.GroupMailInvite

internal data class GroupMailNotificationData(
    val cancelNotificationIds: List<Int>,
    val baseNotificationData: BaseNotificationData,
    val singleNotificationData: List<GroupMailSingleNotificationData>,
    val summaryNotificationData: GroupMailSummaryNotificationData?
)

internal data class BaseGroupNotificationData(
    val account: Account,
    val accountName: String,
    val groupKey: String,
    val color: Int,
    val groupNotificationsCount: Int,
    val lockScreenNotificationData: LockScreenNotificationData,
    val appearance: NotificationAppearance
)

internal data class GroupMailSingleNotificationData(
    val notificationId: Int,
    val isSilent: Boolean,
    val timestamp: Long,
    val content: GroupMailInvite,
    val addLockScreenNotification: Boolean
)

internal sealed interface GroupMailSummaryNotificationData

internal data class GroupMailSummarySingleNotificationData(
    val singleNotificationData: GroupMailSingleNotificationData
) : GroupMailSummaryNotificationData

internal data class GroupMailSummaryInboxNotificationData(
    val notificationId: Int,
    val isSilent: Boolean,
    val timestamp: Long,
    val content: List<CharSequence>,
    val nonVisibleNotificationsCount: Int,
) : GroupMailSummaryNotificationData