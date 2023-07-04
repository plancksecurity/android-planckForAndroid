package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.NotificationQuickDelete
import com.fsck.k9.activity.MessageReference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import security.planck.notification.GroupMailInvite

private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"

class SingleGroupedNotificationDataCreatorTest {
    private val account = createAccount()
    private val notificationDataCreator = SingleGroupedNotificationDataCreator()

    @Test
    fun `base properties`() {
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 23,
            content = content,
            timestamp = 9000,
            addLockScreenNotification = true
        )

        assertThat(result.notificationId).isEqualTo(23)
        assertThat(result.isSilent).isTrue()
        assertThat(result.timestamp).isEqualTo(9000)
        assertThat(result.content).isEqualTo(content)
        assertThat(result.addLockScreenNotification).isTrue()
    }

    @Test
    fun `summary notification base properties`() {
        val content = createNewMailNotificationContent()
        val notificationData = createNewMailNotificationData(content)

        val result = notificationDataCreator.createSummarySingleNotificationData(
            timestamp = 9000,
            silent = false,
            data = notificationData
        )

        assertThat(result.singleNotificationData.notificationId).isEqualTo(
            NotificationIds.getSummaryGroupedNotificationId(account, NotificationGroupType.NEW_MAIL)
        )
        assertThat(result.singleNotificationData.isSilent).isFalse()
        assertThat(result.singleNotificationData.timestamp).isEqualTo(9000)
        assertThat(result.singleNotificationData.content).isEqualTo(content)
        assertThat(result.singleNotificationData.addLockScreenNotification).isFalse()
    }

    @Test
    fun `default actions`() {
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.actions).contains(NotificationAction.Reply)
        assertThat(result.actions).contains(NotificationAction.MarkAsRead)
        assertThat(result.wearActions).contains(WearNotificationAction.Reply)
        assertThat(result.wearActions).contains(WearNotificationAction.MarkAsRead)
    }

    @Test
    fun `always show delete action without confirmation`() {
        setDeleteAction(NotificationQuickDelete.ALWAYS)
        setConfirmDeleteFromNotification(false)
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.actions).contains(NotificationAction.Delete)
        assertThat(result.wearActions).contains(WearNotificationAction.Delete)
    }

    @Test
    fun `always show delete action with confirmation`() {
        setDeleteAction(NotificationQuickDelete.ALWAYS)
        setConfirmDeleteFromNotification(true)
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.actions).contains(NotificationAction.Delete)
        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Delete)
    }

    @Test
    fun `show delete action for single notification without confirmation`() {
        setDeleteAction(NotificationQuickDelete.FOR_SINGLE_MSG)
        setConfirmDeleteFromNotification(false)
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.actions).contains(NotificationAction.Delete)
        assertThat(result.wearActions).contains(WearNotificationAction.Delete)
    }

    @Test
    fun `show delete action for single notification with confirmation`() {
        setDeleteAction(NotificationQuickDelete.FOR_SINGLE_MSG)
        setConfirmDeleteFromNotification(true)
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.actions).contains(NotificationAction.Delete)
        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Delete)
    }

    @Test
    fun `never show delete action`() {
        setDeleteAction(NotificationQuickDelete.NEVER)
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.actions).doesNotContain(NotificationAction.Delete)
        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Delete)
    }

    @Test
    fun `archive action with archive folder`() {
        doReturn("Archive").`when`(account).archiveFolderName
        doReturn(true).`when`(account).hasArchiveFolder()
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.wearActions).contains(WearNotificationAction.Archive)
    }

    @Test
    fun `archive action without archive folder`() {
        doReturn(false).`when`(account).hasArchiveFolder()
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Archive)
    }

    @Test
    fun `spam action with spam folder and without spam confirmation`() {
        doReturn(true).`when`(account).hasSpamFolder()
        setConfirmSpam(false)
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.wearActions).contains(WearNotificationAction.Spam)
    }

    @Test
    fun `spam action with spam folder and with spam confirmation`() {
        doReturn(true).`when`(account).hasSpamFolder()
        setConfirmSpam(true)
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Spam)
    }

    @Test
    fun `spam action without spam folder and without spam confirmation`() {
        doReturn(false).`when`(account).hasSpamFolder()
        setConfirmSpam(false)
        val content = createNewMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.wearActions).doesNotContain(WearNotificationAction.Spam)
    }

    @Test
    fun `group mail notification has no actions`() {
        val content = createGroupMailNotificationContent()

        val result = notificationDataCreator.createSingleNotificationData(
            account = account,
            notificationId = 0,
            content = content,
            timestamp = 0,
            addLockScreenNotification = false
        )

        assertThat(result.actions).isEmpty()
        assertThat(result.wearActions).isEmpty()
    }

    private fun setDeleteAction(mode: NotificationQuickDelete) {
        K9.setNotificationQuickDeleteBehaviour(mode)
    }

    private fun setConfirmDeleteFromNotification(confirm: Boolean) {
        K9.setConfirmDeleteFromNotification(confirm)
    }

    private fun setConfirmSpam(confirm: Boolean) {
        K9.setConfirmSpam(confirm)
    }

    private fun createAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { name } doReturn ACCOUNT_NAME
        }
    }

    private fun createNewMailNotificationContent() = NotificationContent(
        reference = MessageReference("irrelevant", "folder", "irrelevant", null),
        sender = "irrelevant",
        subject = "irrelevant",
        preview = "irrelevant",
        summary = "irrelevant"
    )

    private fun createGroupMailNotificationContent() = NotificationContent(
        reference = GroupMailInvite("irrelevant", "irrelevant", "irrelevant"),
        sender = "irrelevant",
        subject = "irrelevant",
        summary = "irrelevant"
    )

    private fun createNewMailNotificationData(content: NotificationContent<MessageReference>): NotificationData<MessageReference> {
        return NotificationData(
            account,
            activeNotifications = listOf(
                NotificationHolder(
                    notificationId = 1,
                    timestamp = 0,
                    content = content
                )
            ),
            inactiveNotifications = emptyList(),
            notificationGroupType = NotificationGroupType.NEW_MAIL
        )
    }

    private fun createGroupMailNotificationData(content: NotificationContent<GroupMailInvite>): NotificationData<GroupMailInvite> {
        return NotificationData(
            account,
            activeNotifications = listOf(
                NotificationHolder(
                    notificationId = 1,
                    timestamp = 0,
                    content = content
                )
            ),
            inactiveNotifications = emptyList(),
            notificationGroupType = NotificationGroupType.GROUP_MAIL
        )
    }
}
