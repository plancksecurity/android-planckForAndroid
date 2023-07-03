package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.activity.MessageReference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import security.planck.notification.GroupMailInvite

private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"
private const val TIMESTAMP = 0L

class SummaryGroupedNotificationDataCreatorTest {
    private val account = createAccount()
    private val notificationDataCreator = SummaryGroupedNotificationDataCreator(
        SingleGroupedNotificationDataCreator()
    )

    @Test
    fun `single new message`() {
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        assertThat(result).isInstanceOf(SummarySingleNotificationData::class.java)
    }

    @Test
    fun `single notification during quiet time`() {
        setQuietTime(true)
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        val summaryNotificationData = result as SummarySingleNotificationData<*,*>
        assertThat(summaryNotificationData.singleNotificationData.isSilent).isTrue()
    }

    @Test
    fun `single notification with quiet time disabled`() {
        setQuietTime(false)
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        val summaryNotificationData = result as SummarySingleNotificationData<*,*>
        assertThat(summaryNotificationData.singleNotificationData.isSilent).isFalse()
    }

    @Test
    fun `inbox-style notification during quiet time`() {
        setQuietTime(true)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.isSilent).isTrue()
    }

    @Test
    fun `inbox-style notification with quiet time disabled`() {
        setQuietTime(false)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.isSilent).isFalse()
    }

    @Test
    fun `inbox-style base properties`() {
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.notificationId).isEqualTo(
            NotificationIds.getSummaryGroupedNotificationId(account, NotificationGroupType.NEW_MAIL)
        )
        assertThat(summaryNotificationData.isSilent).isTrue()
        assertThat(summaryNotificationData.timestamp).isEqualTo(TIMESTAMP)
    }

    @Test
    fun `default actions`() {
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).contains(SummaryNotificationAction.MarkAsRead)
        assertThat(summaryNotificationData.wearActions).contains(SummaryWearNotificationAction.MarkAsRead)
    }

    @Test
    fun `always show delete action without confirmation`() {
        setDeleteAction(K9.NotificationQuickDelete.ALWAYS)
        setConfirmDeleteFromNotification(false)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).contains(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).contains(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `always show delete action with confirmation`() {
        setDeleteAction(K9.NotificationQuickDelete.ALWAYS)
        setConfirmDeleteFromNotification(true)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).contains(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `show delete action for single notification without confirmation`() {
        setDeleteAction(K9.NotificationQuickDelete.FOR_SINGLE_MSG)
        setConfirmDeleteFromNotification(false)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).doesNotContain(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `never show delete action`() {
        setDeleteAction(K9.NotificationQuickDelete.NEVER)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).doesNotContain(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `archive action with archive folder`() {
        doReturn(true).`when`(account).hasArchiveFolder()
        doReturn("Archive").`when`(account).archiveFolderName
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.wearActions).contains(SummaryWearNotificationAction.Archive)
    }

    @Test
    fun `archive action without archive folder`() {
        doReturn(false).`when`(account).hasArchiveFolder()
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Archive)
    }

    @Test
    fun `group mail notification has no actions`() {
        val notificationData = createNotificationDataWithMultipleGroupMailEvents()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).isEmpty()
        assertThat(summaryNotificationData.wearActions).isEmpty()
    }

    private fun setQuietTime(quietTime: Boolean) {
        K9.setQuietTimeEnabled(quietTime)
        if (quietTime) {
            K9.setQuietTimeStarts("0:00")
            K9.setQuietTimeEnds("23:59")
        }
    }

    private fun setDeleteAction(mode: K9.NotificationQuickDelete) {
        K9.setNotificationQuickDeleteBehaviour(mode)
    }

    private fun setConfirmDeleteFromNotification(confirm: Boolean) {
        K9.setConfirmDeleteFromNotification(confirm)
    }

    private fun createAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { name } doReturn ACCOUNT_NAME
        }
    }

    private fun createNewMailNotificationContent() = NewMailNotificationContent(
        reference = MessageReference("irrelevant", "folder", "irrelevant", null),
        sender = "irrelevant",
        subject = "irrelevant",
        preview = "irrelevant",
        summary = "irrelevant"
    )

    private fun createNewMailNotificationData(
        contentList: List<NewMailNotificationContent> = listOf(createNewMailNotificationContent())
    ): NotificationData<MessageReference, NewMailNotificationContent> {
        val activeNotifications = contentList.mapIndexed { index, content ->
            NotificationHolder(notificationId = index, TIMESTAMP, content)
        }

        return NotificationData(
            account,
            activeNotifications,
            inactiveNotifications = emptyList(),
            notificationGroupType = NotificationGroupType.NEW_MAIL
        )
    }

    private fun createNotificationDataWithMultipleMessages(times: Int = 2): NotificationData<MessageReference, NewMailNotificationContent> {
        val contentList = buildList {
            repeat(times) {
                add(createNewMailNotificationContent())
            }
        }
        return createNewMailNotificationData(contentList)
    }

    private fun createNotificationDataWithMultipleGroupMailEvents(times: Int = 2): NotificationData<GroupMailInvite, GroupMailNotificationContent> {
        val contentList = buildList {
            repeat(times) {
                add(createGroupMailNotificationContent())
            }
        }
        return createGroupMailNotificationData(contentList)
    }

    private fun createGroupMailNotificationContent() = GroupMailNotificationContent(
        reference = GroupMailInvite("irrelevant", "irrelevant", "irrelevant"),
        sender = "irrelevant",
        subject = "irrelevant",
        summary = "irrelevant"
    )

    private fun createGroupMailNotificationData(
        contentList: List<GroupMailNotificationContent> = listOf(createGroupMailNotificationContent())
    ): NotificationData<GroupMailInvite, GroupMailNotificationContent> {
        val activeNotifications = contentList.mapIndexed { index, content ->
            NotificationHolder(notificationId = index, TIMESTAMP, content)
        }

        return NotificationData(
            account,
            activeNotifications,
            inactiveNotifications = emptyList(),
            notificationGroupType = NotificationGroupType.GROUP_MAIL
        )
    }
}