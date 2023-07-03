package com.fsck.k9.notification

internal class AddNotificationResult<Reference: NotificationReference, Content: NotificationContent<Reference>> private constructor(
    val notificationData: NotificationData<Reference, Content>,
    val notificationHolder: NotificationHolder<Content>,
    val shouldCancelNotification: Boolean
) {
    val cancelNotificationId: Int
        get() {
            check(shouldCancelNotification) { "shouldCancelNotification == false" }
            return notificationHolder.notificationId
        }

    companion object {
        fun <Reference: NotificationReference, Content: NotificationContent<Reference>> newNotification(
            notificationData: NotificationData<Reference, Content>,
            notificationHolder: NotificationHolder<Content>
        ): AddNotificationResult<Reference, Content> {
            return AddNotificationResult(
                notificationData,
                notificationHolder,
                shouldCancelNotification = false
            )
        }

        fun <Reference: NotificationReference, Content: NotificationContent<Reference>> replaceNotification(
            notificationData: NotificationData<Reference, Content>,
            notificationHolder: NotificationHolder<Content>
        ): AddNotificationResult<Reference, Content> {
            return AddNotificationResult(
                notificationData,
                notificationHolder,
                shouldCancelNotification = true
            )
        }
    }
}
