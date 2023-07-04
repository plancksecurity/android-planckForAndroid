package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.RobolectricTest
import com.fsck.k9.activity.MessageReference
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val ACCOUNT_UUID = "1-2-3"
private const val ACCOUNT_NUMBER = 23
private const val FOLDER_NAME = "folder"
private const val TIMESTAMP = 0L

class NotificationDataStoreTest : RobolectricTest() {
    private val account = createAccount()
    private val notificationDataStore = NotificationDataStore<MessageReference, NewMailNotificationContent>(NotificationGroupType.NEW_MAIL)

    @Test
    fun testAddNotificationContent() {
        val content = createNewMailNotificationContent("1")

        val result = notificationDataStore.addNotification(account, content, TIMESTAMP)

        assertNotNull(result)
        assertThat(result!!.shouldCancelNotification).isFalse()

        val holder = result.notificationHolder

        assertThat(holder).isNotNull()
        assertThat(holder.notificationId).isEqualTo(NotificationIds.getSingleGroupedNotificationId(account, 0, NotificationGroupType.NEW_MAIL))
        assertThat(holder.content).isEqualTo(content)
    }

    @Test
    fun testAddNotificationContentWithReplacingNotification() {
        notificationDataStore.addNotification(account, createNewMailNotificationContent("1"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("2"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("3"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("4"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("5"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("6"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("7"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("8"), TIMESTAMP)

        val result = notificationDataStore.addNotification(account, createNewMailNotificationContent("9"), TIMESTAMP)

        assertNotNull(result)
        assertThat(result!!.shouldCancelNotification).isTrue()
        assertThat(result.cancelNotificationId).isEqualTo(NotificationIds.getSingleGroupedNotificationId(account, 0, NotificationGroupType.NEW_MAIL))
    }

    @Test
    fun testRemoveNotificationForMessage() {
        val content = createNewMailNotificationContent("1")
        notificationDataStore.addNotification(account, content, TIMESTAMP)

        val result = notificationDataStore.removeNotifications(account) { listOf(content.reference) }

        assertNotNull(result)
        assertThat(result!!.cancelNotificationIds)
            .containsExactly(NotificationIds.getSingleGroupedNotificationId(account, 0, NotificationGroupType.NEW_MAIL))
        assertThat(result.notificationHolders).isEmpty()
    }

    @Test
    fun testRemoveNotificationForMessageWithRecreatingNotification() {
        notificationDataStore.addNotification(account, createNewMailNotificationContent("1"), TIMESTAMP)
        val content = createNewMailNotificationContent("2")
        notificationDataStore.addNotification(account, content, TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("3"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("4"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("5"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("6"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("7"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("8"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("9"), TIMESTAMP)
        val latestContent = createNewMailNotificationContent("10")
        notificationDataStore.addNotification(account, latestContent, TIMESTAMP)

        val result = notificationDataStore.removeNotifications(account) { listOf(latestContent.reference) }

        assertNotNull(result)
        assertThat(result!!.cancelNotificationIds)
            .containsExactly(NotificationIds.getSingleGroupedNotificationId(account, 1, NotificationGroupType.NEW_MAIL))
        assertThat(result.notificationHolders).hasSize(1)

        val holder = result.notificationHolders.first()
        assertThat(holder.notificationId).isEqualTo(NotificationIds.getSingleGroupedNotificationId(account, 1, NotificationGroupType.NEW_MAIL))
        assertThat(holder.content).isEqualTo(content)
    }

    @Test
    fun `remove multiple notifications`() {
        repeat(MAX_NUMBER_OF_STACKED_NOTIFICATIONS + 1) { index ->
            notificationDataStore.addNotification(account, createNewMailNotificationContent(index.toString()), TIMESTAMP)
        }

        val result = notificationDataStore.removeNotifications(account) { it.dropLast(1) }

        assertNotNull(result)
        assertThat(result!!.notificationData.notificationsCount).isEqualTo(1)
        assertThat(result.cancelNotificationIds).hasSize(MAX_NUMBER_OF_STACKED_NOTIFICATIONS)
    }

    @Test
    fun `remove all notifications`() {
        repeat(MAX_NUMBER_OF_STACKED_NOTIFICATIONS + 1) { index ->
            notificationDataStore.addNotification(account, createNewMailNotificationContent(index.toString()), TIMESTAMP)
        }

        val result = notificationDataStore.removeNotifications(account) { it }

        assertNotNull(result)
        assertThat(result!!.notificationData.notificationsCount).isEqualTo(0)
        assertThat(result.notificationHolders).hasSize(0)
    }

    @Test
    fun testRemoveDoesNotLeakNotificationIds() {
        for (i in 1..MAX_NUMBER_OF_STACKED_NOTIFICATIONS + 1) {
            val content = createNewMailNotificationContent(i.toString())
            notificationDataStore.addNotification(account, content, TIMESTAMP)
            notificationDataStore.removeNotifications(account) { listOf(content.reference) }
        }
    }

    @Test
    fun testNewMessagesCount() {
        val contentOne = createNewMailNotificationContent("1")
        val resultOne = notificationDataStore.addNotification(account, contentOne, TIMESTAMP)
        assertNotNull(resultOne)
        assertThat(resultOne!!.notificationData.notificationsCount).isEqualTo(1)

        val contentTwo = createNewMailNotificationContent("2")
        val resultTwo = notificationDataStore.addNotification(account, contentTwo, TIMESTAMP)
        assertNotNull(resultTwo)
        assertThat(resultTwo!!.notificationData.notificationsCount).isEqualTo(2)
    }

    @Test
    fun testIsSingleMessageNotification() {
        val resultOne = notificationDataStore.addNotification(account, createNewMailNotificationContent("1"), TIMESTAMP)
        assertNotNull(resultOne)
        assertThat(resultOne!!.notificationData.isSingleMessageNotification).isTrue()

        val resultTwo = notificationDataStore.addNotification(account, createNewMailNotificationContent("2"), TIMESTAMP)
        assertNotNull(resultTwo)
        assertThat(resultTwo!!.notificationData.isSingleMessageNotification).isFalse()
    }

    @Test
    fun testGetHolderForLatestNotification() {
        val content = createNewMailNotificationContent("1")
        val addResult = notificationDataStore.addNotification(account, content, TIMESTAMP)

        assertNotNull(addResult)
        assertThat(addResult!!.notificationData.activeNotifications.first()).isEqualTo(addResult.notificationHolder)
    }

    @Test
    fun `adding notification for message with active notification should update notification`() {
        val content1 = createNewMailNotificationContent("1")
        val content2 = createNewMailNotificationContent("1")

        val resultOne = notificationDataStore.addNotification(account, content1, TIMESTAMP)
        val resultTwo = notificationDataStore.addNotification(account, content2, TIMESTAMP)

        assertNotNull(resultOne)
        assertNotNull(resultTwo)
        assertThat(resultTwo!!.notificationData.activeNotifications).hasSize(1)
        assertThat(resultTwo.notificationData.activeNotifications.first().content).isSameInstanceAs(content2)
        with(resultTwo.notificationHolder) {
            assertThat(notificationId).isEqualTo(resultOne!!.notificationHolder.notificationId)
            assertThat(timestamp).isEqualTo(resultOne.notificationHolder.timestamp)
            assertThat(content).isSameInstanceAs(content2)
        }
        assertThat(resultTwo.shouldCancelNotification).isFalse()
    }

    @Test
    fun `adding notification for message with inactive notification should update notificationData`() {
        notificationDataStore.addNotification(account, createNewMailNotificationContent("1"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("2"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("3"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("4"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("5"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("6"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("7"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNewMailNotificationContent("8"), TIMESTAMP)
        val latestNotificationContent = createNewMailNotificationContent("9")
        notificationDataStore.addNotification(account, latestNotificationContent, TIMESTAMP)
        val content = createNewMailNotificationContent("1")

        val resultOne = notificationDataStore.addNotification(account, content, TIMESTAMP)

        assertThat(resultOne).isNull()

        val resultTwo = notificationDataStore.removeNotifications(account) {
            listOf(latestNotificationContent.reference)
        }

        assertNotNull(resultTwo)
        val notificationHolder = resultTwo!!.notificationData.activeNotifications.first { notificationHolder ->
            notificationHolder.content.reference == content.reference
        }
        assertThat(notificationHolder.content).isSameInstanceAs(content)
    }

    private fun createAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { uuid } doReturn ACCOUNT_UUID
        }
    }

    private fun createMessageReference(uid: String): MessageReference {
        return MessageReference(ACCOUNT_UUID, FOLDER_NAME, uid, null)
    }

    private fun createNewMailNotificationContent(uid: String): NewMailNotificationContent {
        val messageReference = createMessageReference(uid)
        return createNewMailNotificationContent(messageReference)
    }

    private fun createNewMailNotificationContent(messageReference: MessageReference): NewMailNotificationContent {
        return NewMailNotificationContent(
            reference = messageReference,
            sender = "irrelevant",
            subject = "irrelevant",
            preview = "irrelevant",
            summary = "irrelevant"
        )
    }
}