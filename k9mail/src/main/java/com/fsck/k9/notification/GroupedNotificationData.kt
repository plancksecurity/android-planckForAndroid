package com.fsck.k9.notification

import com.fsck.k9.Account

internal data class GroupedNotificationData<out Reference: NotificationReference>(
    val cancelNotificationIds: List<Int>,
    val baseNotificationData: BaseNotificationData,
    val singleNotificationData: List<SingleNotificationData<Reference>>,
    val summaryNotificationData: SummaryNotificationData<Reference>?
)

internal data class BaseNotificationData constructor(
    val account: Account,
    val accountName: String,
    val groupKey: String,
    val color: Int,
    val notificationsCount: Int,
    val lockScreenNotificationData: LockScreenNotificationData,
    val appearance: NotificationAppearance
)

internal sealed interface LockScreenNotificationData {
    object None : LockScreenNotificationData
    object AppName : LockScreenNotificationData
    object Public : LockScreenNotificationData
    object MessageCount : LockScreenNotificationData
    data class SenderNames(val senderNames: String) : LockScreenNotificationData
}

internal data class NotificationAppearance(
    val ringtone: String?,
    val vibrationPattern: LongArray?,
    val ledColor: Int?
)

internal data class SingleNotificationData<out Reference: NotificationReference>(
    val notificationId: Int,
    val isSilent: Boolean,
    val timestamp: Long,
    val content: NotificationContent<Reference>,
    val actions: List<NotificationAction>,
    val wearActions: List<WearNotificationAction>,
    val addLockScreenNotification: Boolean
)

internal sealed interface SummaryNotificationData<out Reference>

internal data class SummarySingleNotificationData<out Reference: NotificationReference>(
    val singleNotificationData: SingleNotificationData<Reference>
) : SummaryNotificationData<Reference>

internal data class SummaryInboxNotificationData<out Reference>(
    val notificationId: Int,
    val isSilent: Boolean,
    val timestamp: Long,
    val content: List<CharSequence>,
    val nonVisibleNotificationsCount: Int,
    val references: List<Reference>,
    val actions: List<SummaryNotificationAction>,
    val wearActions: List<SummaryWearNotificationAction>
) : SummaryNotificationData<Reference>

internal enum class NotificationAction {
    Reply,
    MarkAsRead,
    Delete
}

internal enum class WearNotificationAction {
    Reply,
    MarkAsRead,
    Delete,
    Archive,
    Spam
}

internal enum class SummaryNotificationAction {
    MarkAsRead,
    Delete
}

internal enum class SummaryWearNotificationAction {
    MarkAsRead,
    Delete,
    Archive
}
