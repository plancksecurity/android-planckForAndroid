package com.fsck.k9.notification

internal class AddNotificationResult<Reference: NotificationReference> private constructor(
    val notificationData: NotificationData<Reference>,
    val notificationHolder: NotificationHolder<Reference>,
    val shouldCancelNotification: Boolean
) {
    val cancelNotificationId: Int
        get() {
            check(shouldCancelNotification) { "shouldCancelNotification == false" }
            return notificationHolder.notificationId
        }

    companion object {
        fun <Reference: NotificationReference> newNotification(
            notificationData: NotificationData<Reference>,
            notificationHolder: NotificationHolder<Reference>
        ): AddNotificationResult<Reference> {
            return AddNotificationResult(
                notificationData,
                notificationHolder,
                shouldCancelNotification = false
            )
        }

        fun <Reference: NotificationReference> replaceNotification(
            notificationData: NotificationData<Reference>,
            notificationHolder: NotificationHolder<Reference>
        ): AddNotificationResult<Reference> {
            return AddNotificationResult(
                notificationData,
                notificationHolder,
                shouldCancelNotification = true
            )
        }
    }
}
