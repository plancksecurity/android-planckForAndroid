package com.fsck.k9.notification

import com.fsck.k9.Account
import security.planck.notification.GroupMailInvite

internal const val MAX_NUMBER_OF_GROUP_NOTIFICATIONS = 8

/**
 * Stores information about new message notifications for all accounts.
 *
 * We only use a limited number of system notifications per account (see [MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS]);
 * those are called active notifications. The rest are called inactive notifications. When an active notification is
 * removed, the latest inactive notification is promoted to an active notification.
 */
internal class GroupMailNotificationDataStore {
    private val notificationDataMap = mutableMapOf<String, NotificationDataGroupMail>()

    @Synchronized
    fun isAccountInitialized(account: Account): Boolean {
        return notificationDataMap[account.uuid] != null
    }

    @Synchronized
    fun initializeAccount(
        account: Account,
        activeNotifications: List<GroupMailNotificationHolder>,
        inactiveNotifications: List<InactiveGroupMailNotificationHolder>
    ): NotificationDataGroupMail {
        require(activeNotifications.size <= MAX_NUMBER_OF_GROUP_NOTIFICATIONS)

        return NotificationDataGroupMail(account, activeNotifications, inactiveNotifications).also { notificationData ->
            notificationDataMap[account.uuid] = notificationData
        }
    }

    @Synchronized
    fun addNotification(account: Account, content: GroupMailInvite, timestamp: Long): AddGroupMailNotificationResult? {
        val notificationData = getNotificationData(account)

        val activeNotification = notificationData.activeNotifications.firstOrNull { notificationHolder ->
            notificationHolder.content == content
        }
        val inactiveNotification = notificationData.inactiveNotifications.firstOrNull { inactiveNotificationHolder ->
            inactiveNotificationHolder.content == content
        }

        return if (activeNotification != null) {
            val newActiveNotification = activeNotification.copy(content = content)
            val notificationHolder = activeNotification.copy(
                content = content
            )

            val operations = emptyList<GroupMailNotificationStoreOperation>()

            val newActiveNotifications = notificationData.activeNotifications
                .replace(activeNotification, newActiveNotification)
            val newNotificationData = notificationData.copy(
                activeNotifications = newActiveNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddGroupMailNotificationResult.newNotification(newNotificationData, operations, notificationHolder)
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
            val notificationHolder = GroupMailNotificationHolder(notificationId, timestamp, content)

            val operations = listOf(
                GroupMailNotificationStoreOperation.ChangeToInactive(lastNotificationHolder.content),
                GroupMailNotificationStoreOperation.Add(content, notificationId, timestamp)
            )

            val newNotificationData = notificationData.copy(
                activeNotifications = listOf(notificationHolder) + notificationData.activeNotifications.dropLast(1),
                inactiveNotifications = listOf(inactiveNotificationHolder) + notificationData.inactiveNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddGroupMailNotificationResult.replaceNotification(newNotificationData, operations, notificationHolder)
        } else {
            val notificationId = notificationData.getNewNotificationId()
            val notificationHolder = GroupMailNotificationHolder(notificationId, timestamp, content)

            val operations = listOf(
                GroupMailNotificationStoreOperation.Add(content, notificationId, timestamp)
            )

            val newNotificationData = notificationData.copy(
                activeNotifications = listOf(notificationHolder) + notificationData.activeNotifications
            )
            notificationDataMap[account.uuid] = newNotificationData

            AddGroupMailNotificationResult.newNotification(newNotificationData, operations, notificationHolder)
        }
    }

    @Synchronized
    fun removeNotifications(
        account: Account,
        selector: (List<GroupMailInvite>) -> List<GroupMailInvite>
    ): RemoveGroupMailNotificationsResult? {
        var notificationData = getNotificationData(account)
        if (notificationData.isEmpty()) return null

        val removeReferences = selector.invoke(notificationData.notificationReferences)
        if (removeReferences.isEmpty()) return null

        val operations = mutableListOf<GroupMailNotificationStoreOperation>()
        val newNotificationHolders = mutableListOf<GroupMailNotificationHolder>()
        val cancelNotificationIds = mutableListOf<Int>()

        val activeReferences = notificationData.activeNotifications.map { it.content }.toSet()
        val (removeActiveReferences, removeInactiveReferences) = removeReferences
            .partition { it in activeReferences }

        if (removeInactiveReferences.isNotEmpty()) {
            val inactiveMessageReferences = notificationData.inactiveNotifications
                .map { it.content }.toSet()

            for (messageReference in removeInactiveReferences) {
                if (messageReference in inactiveMessageReferences) {
                    operations.add(GroupMailNotificationStoreOperation.Remove(messageReference))
                }
            }

            val removeReferenceSet = removeInactiveReferences.toSet()
            notificationData = notificationData.copy(
                inactiveNotifications = notificationData.inactiveNotifications
                    .filter { it.content !in removeReferenceSet }
            )
        }

        for (reference in removeActiveReferences) {
            val notificationHolder = notificationData.activeNotifications.first {
                it.content == reference
            }

            if (notificationData.inactiveNotifications.isNotEmpty()) {
                val newNotificationHolder = notificationData.inactiveNotifications.first()
                    .toNotificationHolder(notificationHolder.notificationId)

                newNotificationHolders.add(newNotificationHolder)
                cancelNotificationIds.add(notificationHolder.notificationId)

                operations.add(GroupMailNotificationStoreOperation.Remove(reference))
                operations.add(
                    GroupMailNotificationStoreOperation.ChangeToActive(
                        newNotificationHolder.content,
                        newNotificationHolder.notificationId
                    )
                )

                notificationData = notificationData.copy(
                    activeNotifications = notificationData.activeNotifications - notificationHolder +
                        newNotificationHolder,
                    inactiveNotifications = notificationData.inactiveNotifications.drop(1)
                )
            } else {
                cancelNotificationIds.add(notificationHolder.notificationId)

                operations.add(GroupMailNotificationStoreOperation.Remove(reference))

                notificationData = notificationData.copy(
                    activeNotifications = notificationData.activeNotifications - notificationHolder
                )
            }
        }

        notificationDataMap[account.uuid] = notificationData

        return if (operations.isEmpty()) {
            null
        } else {
            RemoveGroupMailNotificationsResult(
                notificationData = notificationData,
                notificationStoreOperations = operations,
                notificationHolders = newNotificationHolders,
                cancelNotificationIds = cancelNotificationIds
            )
        }
    }

    @Synchronized
    fun clearNotifications(account: Account) {
        notificationDataMap.remove(account.uuid)
    }

    private fun getNotificationData(account: Account): NotificationDataGroupMail {
        return notificationDataMap[account.uuid] ?: NotificationDataGroupMail.create(account).also { notificationData ->
            notificationDataMap[account.uuid] = notificationData
        }
    }

    private val NotificationDataGroupMail.isMaxNumberOfActiveNotificationsReached: Boolean
        get() = activeNotifications.size == MAX_NUMBER_OF_GROUP_NOTIFICATIONS

    private fun NotificationDataGroupMail.getNewNotificationId(): Int {
        val notificationIdsInUse = activeNotifications.map { it.notificationId }.toSet()
        for (index in 0 until MAX_NUMBER_OF_GROUP_NOTIFICATIONS) {
            val notificationId = NotificationIds.getSingleMessageNotificationId(account, index)
            if (notificationId !in notificationIdsInUse) {
                return notificationId
            }
        }

        throw AssertionError("getNewNotificationId() called with no free notification ID")
    }

    private fun GroupMailNotificationHolder.toInactiveNotificationHolder() = InactiveGroupMailNotificationHolder(timestamp, content)

    private fun InactiveGroupMailNotificationHolder.toNotificationHolder(notificationId: Int): GroupMailNotificationHolder {
        return GroupMailNotificationHolder(notificationId, timestamp, content)
    }

    private fun <T> List<T>.replace(old: T, new: T): List<T> {
        return map { element ->
            if (element === old) new else element
        }
    }
}
