package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.NotificationSetting
import com.fsck.k9.TestClock
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.mailstore.LocalMessage
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

private const val ACCOUNT_UUID = "00000000-0000-4000-0000-000000000000"
private const val ACCOUNT_NUMBER = 23
private const val ACCOUNT_NAME = "Personal"
private const val ACCOUNT_EMAIL = "email@email.test"
private const val FOLDER_NAME = "folder"
private const val TIMESTAMP = 23L

class GroupedNotificationManagerTest {
    private val account = createAccount()
    private val notificationContentCreator = mock<NotificationContentCreator>()
    private val clock = TestClock(TIMESTAMP)
    private val manager = GroupedNotificationManager(
        notificationContentCreator,
        createNotificationRepository(NotificationGroupType.NEW_MAIL),
        createNotificationRepository(NotificationGroupType.GROUP_MAIL),
        BaseNotificationDataCreator(),
        SingleGroupedNotificationDataCreator(),
        SummaryGroupedNotificationDataCreator(SingleGroupedNotificationDataCreator()),
        clock
    )

    @Test
    fun `add first notification`() {
        val message = addMessageToNotificationContentCreator(
            sender = "sender",
            subject = "subject",
            preview = "preview",
            summary = "summary",
            messageUid = "msg-1"
        )

        val result = manager.addNewMailNotification(account, message, silent = false)

        assertNotNull(result)
        assertThat(result!!.singleNotificationData.first().content).isEqualTo(
            NewMailNotificationContent(
                reference = createMessageReference("msg-1"),
                sender = "sender",
                subject = "subject",
                preview = "preview",
                summary = "summary"
            )
        )
        assertThat(result.summaryNotificationData).isInstanceOf(SummarySingleNotificationData::class.java)
        val summaryNotificationData = result.summaryNotificationData as SummarySingleNotificationData<*,*>
        assertThat(summaryNotificationData.singleNotificationData.isSilent).isFalse()
    }

    @Test
    fun `add second notification`() {
        val messageOne = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Hi Bob",
            preview = "How are you?",
            summary = "Alice Hi Bob",
            messageUid = "msg-1"
        )
        val messageTwo = addMessageToNotificationContentCreator(
            sender = "Zoe",
            subject = "Meeting",
            preview = "We need to talk",
            summary = "Zoe Meeting",
            messageUid = "msg-2"
        )
        manager.addNewMailNotification(account, messageOne, silent = false)
        val timestamp = TIMESTAMP + 1000
        clock.time = timestamp

        val result = manager.addNewMailNotification(account, messageTwo, silent = false)

        assertNotNull(result)
        assertThat(result!!.singleNotificationData.first().content).isEqualTo(
            NewMailNotificationContent(
                reference = createMessageReference("msg-2"),
                sender = "Zoe",
                subject = "Meeting",
                preview = "We need to talk",
                summary = "Zoe Meeting"
            )
        )
        assertThat(result.baseNotificationData.notificationsCount).isEqualTo(2)
        assertThat(result.summaryNotificationData).isInstanceOf(SummaryInboxNotificationData::class.java)
        val summaryNotificationData = result.summaryNotificationData as SummaryInboxNotificationData
        assertThat(summaryNotificationData.content).isEqualTo(listOf("Zoe Meeting", "Alice Hi Bob"))
        assertThat(summaryNotificationData.references).isEqualTo(
            listOf(
                createMessageReference("msg-2"),
                createMessageReference("msg-1")
            )
        )
        assertThat(summaryNotificationData.nonVisibleNotificationsCount).isEqualTo(0)
        assertThat(summaryNotificationData.isSilent).isFalse()
    }

    @Test
    fun `add one more notification when already displaying the maximum number of notifications`() {
        addMaximumNumberOfNotifications()
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Another one",
            preview = "Are you tired of me yet?",
            summary = "Alice Another one",
            messageUid = "msg-x"
        )

        val result = manager.addNewMailNotification(account, message, silent = false)

        assertNotNull(result)
        val notificationId = NotificationIds.getSingleGroupedNotificationId(account, index = 0, NotificationGroupType.NEW_MAIL)
        assertThat(result!!.cancelNotificationIds).isEqualTo(listOf(notificationId))
        assertThat(result.singleNotificationData.first().notificationId).isEqualTo(notificationId)
    }

    @Test
    fun `remove notification when none was added before should return null`() {
        val result = manager.removeNewMailNotifications(account) {
            listOf(createMessageReference("any"))
        }

        assertThat(result).isNull()
    }

    @Test
    fun `remove notification with untracked notification ID should return null`() {
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Another one",
            preview = "Are you tired of me yet?",
            summary = "Alice Another one",
            messageUid = "msg-x"
        )
        manager.addNewMailNotification(account, message, silent = false)

        val result = manager.removeNewMailNotifications(account) {
            listOf(createMessageReference("untracked"))
        }

        assertThat(result).isNull()
    }

    @Test
    fun `remove last remaining notification`() {
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Hello",
            preview = "How are you?",
            summary = "Alice Hello",
            messageUid = "msg-1"
        )
        manager.addNewMailNotification(account, message, silent = false)

        val result = manager.removeNewMailNotifications(account) {
            listOf(createMessageReference("msg-1"))
        }

        assertNotNull(result)
        assertThat(result!!.cancelNotificationIds).containsExactly(
            NotificationIds.getSummaryGroupedNotificationId(account, NotificationGroupType.NEW_MAIL),
            NotificationIds.getSingleGroupedNotificationId(account, 0, NotificationGroupType.NEW_MAIL)
        )
        assertThat(result.singleNotificationData).isEmpty()
        assertThat(result.summaryNotificationData).isNull()
    }

    @Test
    fun `remove one of three notifications`() {
        val messageOne = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "One",
            preview = "preview",
            summary = "Alice One",
            messageUid = "msg-1"
        )
        manager.addNewMailNotification(account, messageOne, silent = false)
        val messageTwo = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Two",
            preview = "preview",
            summary = "Alice Two",
            messageUid = "msg-2"
        )
        val dataTwo = manager.addNewMailNotification(account, messageTwo, silent = true)
        assertNotNull(dataTwo)
        val notificationIdTwo = dataTwo!!.singleNotificationData.first().notificationId
        val messageThree = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Three",
            preview = "preview",
            summary = "Alice Three",
            messageUid = "msg-3"
        )
        manager.addNewMailNotification(account, messageThree, silent = true)

        val result = manager.removeNewMailNotifications(account) {
            listOf(createMessageReference("msg-2"))
        }

        assertNotNull(result)
        assertThat(result!!.cancelNotificationIds).isEqualTo(listOf(notificationIdTwo))
        assertThat(result.singleNotificationData).isEmpty()
        assertThat(result.baseNotificationData.notificationsCount).isEqualTo(2)
        assertThat(result.summaryNotificationData).isInstanceOf(SummaryInboxNotificationData::class.java)
        val summaryNotificationData = result.summaryNotificationData as SummaryInboxNotificationData<*>
        assertThat(summaryNotificationData.content).isEqualTo(listOf("Alice Three", "Alice One"))
        assertThat(summaryNotificationData.references).isEqualTo(
            listOf(
                createMessageReference("msg-3"),
                createMessageReference("msg-1")
            )
        )
    }

    @Test
    fun `remove notification when additional notifications are available`() {
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Another one",
            preview = "Are you tired of me yet?",
            summary = "Alice Another one",
            messageUid = "msg-restore"
        )
        manager.addNewMailNotification(account, message, silent = false)
        addMaximumNumberOfNotifications()

        val result = manager.removeNewMailNotifications(account) {
            listOf(createMessageReference("msg-1"))
        }

        assertNotNull(result)
        assertThat(result!!.cancelNotificationIds).hasSize(1)
        assertThat(result.baseNotificationData.notificationsCount)
            .isEqualTo(MAX_NUMBER_OF_STACKED_NOTIFICATIONS)

        val singleNotificationData = result.singleNotificationData.first()
        assertThat(singleNotificationData.notificationId).isEqualTo(result.cancelNotificationIds.first())
        assertThat(singleNotificationData.isSilent).isTrue()
        assertThat(singleNotificationData.content).isEqualTo(
            NewMailNotificationContent(
                reference = createMessageReference("msg-restore"),
                sender = "Alice",
                subject = "Another one",
                preview = "Are you tired of me yet?",
                summary = "Alice Another one"
            )
        )
    }

    private fun createAccount(): Account {
        val notificationSetting = NotificationSetting().apply {
            vibratePattern = 0
            vibrateTimes = 1
        }
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { uuid } doReturn ACCOUNT_UUID
            on { name } doReturn ACCOUNT_NAME
            on { email } doReturn ACCOUNT_EMAIL
            on { this.notificationSetting } doReturn notificationSetting
        }
    }

    private fun addMaximumNumberOfNotifications() {
        repeat(MAX_NUMBER_OF_STACKED_NOTIFICATIONS) { index ->
            val message = addMessageToNotificationContentCreator(
                sender = "sender",
                subject = "subject",
                preview = "preview",
                summary = "summary",
                messageUid = "msg-$index"
            )
            manager.addNewMailNotification(account, message, silent = true)
        }
    }

    private fun addMessageToNotificationContentCreator(
        sender: String,
        subject: String,
        preview: String,
        summary: String,
        messageUid: String
    ): LocalMessage {
        val message = mock<LocalMessage>()

        stubbing(notificationContentCreator) {
            on { createFromMessage(account, message) } doReturn
                NewMailNotificationContent(
                    reference = createMessageReference(messageUid),
                    sender = sender, subject = subject, preview = preview, summary = summary
                )
        }

        return message
    }

    private fun createMessageReference(messageUid: String): MessageReference {
        return MessageReference(ACCOUNT_UUID, FOLDER_NAME, messageUid, null)
    }

    private fun <Reference: NotificationReference, Content: NotificationContent<Reference>> createNotificationRepository(
        type: NotificationGroupType
    ): NotificationRepository<Reference, Content> {

        return NotificationRepository(
            NotificationDataStore(type)
        )
    }
}