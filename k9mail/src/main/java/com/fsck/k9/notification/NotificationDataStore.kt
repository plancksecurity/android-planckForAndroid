package com.fsck.k9.notification

import com.fsck.k9.Account

internal const val MAX_NUMBER_OF_STACKED_NOTIFICATIONS = 8

/**
 * Stores information about new notifications belonging to a certain notification group, for all accounts.
 * Current notification groups:
 * - new message notifications
 * - group mail notifications
 *
 * We only use a limited number of system notifications per account (see [MAX_NUMBER_OF_STACKED_NOTIFICATIONS]);
 * those are called active notifications. The rest are called inactive notifications. When an active notification is
 * removed, the latest inactive notification is promoted to an active notification.
 */
internal class NotificationDataStore<Reference: NotificationReference, Content: NotificationContent<Reference>>(
    private val notificationGroupType: NotificationGroupType
) {
    private val notificationDataMap = mutableMapOf<String, NotificationData<Reference, Content>>()

    @Synchronized
    fun isAccountInitialized(account: Account): Boolean {
        return notificationDataMap[account.uuid] != null
    }

    @Synchronized
    fun initializeAccount(
        account: Account,
        activeNotifications: List<NotificationHolder<Content>>,
        inactiveNotifications: List<InactiveNotificationHolder<Content>>
    ): NotificationData<Reference, Content> {
        require(activeNotifications.size <= MAX_NUMBER_OF_STACKED_NOTIFICATIONS)

        return NotificationData(account, activeNotifications, inactiveNotifications, notificationGroupType).also { notificationData ->
            notificationDataMap[account.uuid] = notificationData
        }
    }

    @Synchronized
    fun addNotification(account: Account, content: Content, timestamp: Long): AddNotificationResult<Reference, Content>? {
        val notificationData = getNotificationData(account)
        val reference = content.reference

        val activeNotification = notificationData.activeNotifications.firstOrNull { notificationHolder ->
            notificationHolder.content.reference == reference
        }
        val inactiveNotification = notificationData.inactiveNotifications.firstOrNull { inactiveNotificationHolder ->
            inactiveNotificationHolder.content.reference == reference
        }

        return if (activeNotification != null) {
            val newActiveNotification = activeNotification.copy(content = content)
            val notificationHolder = activeNotification.copy(
                content = content
            )

            val newActiveNotifications = notificationData.activeNotifications
                .replace(activeNotification, newActiveNotification)
            val newNotificationData = notificationData.copy(
                activeNotifications = newActiveNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddNotificationResult.newNotification(newNotificationData, notificationHolder)
        } else if (inactiveNotification != null) {
            val newInactiveNotification = inactiveNotification.copy(content = content)
            val newInactiveNotifications = notificationData.inactiveNotifications
                .replace(inactiveNotification, newInactiveNotification)

            val newNotificationData = notificationData.copy(
                inactiveNotifications = newInactiveNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            null
        } else if (notificationData.isMaxNumberOfActiveNotificationsReached) {
            val lastNotificationHolder = notificationData.activeNotifications.last()
            val inactiveNotificationHolder = lastNotificationHolder.toInactiveNotificationHolder()

            val notificationId = lastNotificationHolder.notificationId
            val notificationHolder = NotificationHolder(notificationId, timestamp, content)

            val newNotificationData = notificationData.copy(
                activeNotifications = listOf(notificationHolder) + notificationData.activeNotifications.dropLast(1),
                inactiveNotifications = listOf(inactiveNotificationHolder) + notificationData.inactiveNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddNotificationResult.replaceNotification(newNotificationData, notificationHolder)
        } else {
            val notificationId = notificationData.getNewNotificationId()
            val notificationHolder = NotificationHolder(notificationId, timestamp, content)

            val newNotificationData = notificationData.copy(
                activeNotifications = listOf(notificationHolder) + notificationData.activeNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddNotificationResult.newNotification(newNotificationData, notificationHolder)
        }
    }

    @Synchronized
    fun removeNotifications(
        account: Account,
        selector: (List<Reference>) -> List<Reference>
    ): RemoveNotificationsResult<Reference, Content>? {
        var notificationData = getNotificationData(account)
        if (notificationData.isEmpty()) return null

        val removeReferences = selector.invoke(notificationData.references)
        if (removeReferences.isEmpty()) return null

        val newNotificationHolders = mutableListOf<NotificationHolder<Content>>()
        val cancelNotificationIds = mutableListOf<Int>()

        val activeMessageReferences = notificationData.activeNotifications.map { it.content.reference }.toSet()
        val (removeActiveReferences, removeInactiveReferences) = removeReferences
            .partition { it in activeMessageReferences }

        if (removeInactiveReferences.isNotEmpty()) {

            val removeMessageReferenceSet = removeInactiveReferences.toSet()
            notificationData = notificationData.copy(
                inactiveNotifications = notificationData.inactiveNotifications
                    .filter { it.content.reference !in removeMessageReferenceSet }
            )
        }

        for (reference in removeActiveReferences) {
            val notificationHolder = notificationData.activeNotifications.first {
                it.content.reference == reference
            }

            if (notificationData.inactiveNotifications.isNotEmpty()) {
                val newNotificationHolder = notificationData.inactiveNotifications.first()
                    .toNotificationHolder(notificationHolder.notificationId)

                newNotificationHolders.add(newNotificationHolder)
                cancelNotificationIds.add(notificationHolder.notificationId)

                notificationData = notificationData.copy(
                    activeNotifications = notificationData.activeNotifications - notificationHolder +
                        newNotificationHolder,
                    inactiveNotifications = notificationData.inactiveNotifications.drop(1)
                )
            } else {
                cancelNotificationIds.add(notificationHolder.notificationId)

                notificationData = notificationData.copy(
                    activeNotifications = notificationData.activeNotifications - notificationHolder
                )
            }
        }

        notificationDataMap[account.uuid] = notificationData

        return if (newNotificationHolders.isEmpty() && cancelNotificationIds.isEmpty()) {
            null
        } else {
            RemoveNotificationsResult(
                notificationData = notificationData,
                notificationHolders = newNotificationHolders,
                cancelNotificationIds = cancelNotificationIds
            )
        }
    }

    @Synchronized
    fun clearNotifications(account: Account) {
        notificationDataMap.remove(account.uuid)
    }

    private fun getNotificationData(account: Account): NotificationData<Reference, Content> {
        return notificationDataMap[account.uuid] ?: NotificationData.create<Reference, Content>(
            account, notificationGroupType
        ).also { notificationData ->
            notificationDataMap[account.uuid] = notificationData
        }
    }

    private val NotificationData<Reference, Content>.isMaxNumberOfActiveNotificationsReached: Boolean
        get() = activeNotifications.size == MAX_NUMBER_OF_STACKED_NOTIFICATIONS

    private fun NotificationData<Reference, Content>.getNewNotificationId(): Int {
        val notificationIdsInUse = activeNotifications.map { it.notificationId }.toSet()
        for (index in 0 until MAX_NUMBER_OF_STACKED_NOTIFICATIONS) {
            val notificationId = NotificationIds.getSingleGroupedNotificationId(account, index, notificationGroupType)
            if (notificationId !in notificationIdsInUse) {
                return notificationId
            }
        }

        throw AssertionError("getNewNotificationId() called with no free notification ID")
    }

    private fun NotificationHolder<Content>.toInactiveNotificationHolder() = InactiveNotificationHolder(timestamp, content)

    private fun InactiveNotificationHolder<Content>.toNotificationHolder(notificationId: Int): NotificationHolder<Content> {
        return NotificationHolder(notificationId, timestamp, content)
    }

    private fun <T> List<T>.replace(old: T, new: T): List<T> {
        return map { element ->
            if (element === old) new else element
        }
    }
}
