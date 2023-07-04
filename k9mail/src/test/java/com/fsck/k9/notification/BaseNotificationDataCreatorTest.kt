package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.LockScreenNotificationVisibility
import com.fsck.k9.NotificationSetting
import com.fsck.k9.activity.MessageReference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"

class BaseNotificationDataCreatorTest {
    private var account = createAccount()
    private val notificationDataCreator = BaseNotificationDataCreator()

    @Test
    fun `account instance`() {
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.account).isSameInstanceAs(account)
    }

    @Test
    fun `account name from name property`() {
        doReturn("name").`when`(account).name
        doReturn("test@k9mail.example").`when`(account).email
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("name")
    }

    @Test
    fun `account name is blank`() {
        doReturn("").`when`(account).name
        doReturn("test@k9mail.example").`when`(account).email
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("test@k9mail.example")
    }

    @Test
    fun `account name is null`() {
        doReturn(null).`when`(account).name
        doReturn("test@k9mail.example").`when`(account).email
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.accountName).isEqualTo("test@k9mail.example")
    }

    @Test
    fun `group key`() {
        doReturn(42).`when`(account).accountNumber
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.groupKey).isEqualTo("newMailNotifications-42")
    }

    @Test
    fun `notification color`() {
        doReturn(0xFF0000).`when`(account).chipColor
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.color).isEqualTo(0xFF0000)
    }

    @Test
    fun `new messages count`() {
        val notificationData = createNewMailNotificationData(senders = listOf("irrelevant", "irrelevant"))

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.notificationsCount).isEqualTo(2)
    }

    @Test
    fun `do not display notification on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.NOTHING)
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.None)
    }

    @Test
    fun `display application name on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.APP_NAME)
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.AppName)
    }

    @Test
    fun `display new message count on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.MESSAGE_COUNT)
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.MessageCount)
    }

    @Test
    fun `display message sender names on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.SENDERS)
        val notificationData = createNewMailNotificationData(senders = listOf("Sender One", "Sender Two", "Sender Three"))

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isInstanceOf(LockScreenNotificationData.SenderNames::class.java)
        val senderNamesData = result.lockScreenNotificationData as LockScreenNotificationData.SenderNames
        assertThat(senderNamesData.senderNames).isEqualTo("Sender One, Sender Two, Sender Three")
    }

    @Test
    fun `display notification on lock screen`() {
        setLockScreenMode(LockScreenNotificationVisibility.EVERYTHING)
        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.lockScreenNotificationData).isEqualTo(LockScreenNotificationData.Public)
    }

    @Test
    fun ringtone() {
        val notificationSetting = NotificationSetting().apply {
            vibratePattern = 0
            vibrateTimes = 1
            ringtone = "content://ringtone/1"
        }
        doReturn(notificationSetting).`when`(account).notificationSetting

        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.ringtone).isEqualTo("content://ringtone/1")
    }

    @Test
    fun `vibration pattern`() {
        val notificationSetting = NotificationSetting().apply {
            setVibrate(true)
            vibratePattern = 3
            vibrateTimes = 2
            ringtone = "content://ringtone/1"
        }
        doReturn(notificationSetting).`when`(account).notificationSetting

        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        println(result.appearance.vibrationPattern?.map { it })

        assertThat(result.appearance.vibrationPattern).isEqualTo(
            LongArray(4).also {
                it[0] = 0L
                it[1] = 200L
                it[2] = 200L
                it[3] = 200L
            }
        )
    }

    @Test
    fun `led color`() {
        val notificationSetting = NotificationSetting().apply {
            vibratePattern = 0
            vibrateTimes = 1
            ledColor = 0xFF00FF00L.toInt()
            setLed(true)
        }
        doReturn(notificationSetting).`when`(account).notificationSetting

        val notificationData = createNewMailNotificationData()

        val result = notificationDataCreator.createBaseNotificationData(notificationData)

        assertThat(result.appearance.ledColor).isEqualTo(0xFF00FF00L.toInt())
    }

    private fun setLockScreenMode(mode: LockScreenNotificationVisibility) {
        K9.setLockScreenNotificationVisibility(mode)
    }

    private fun createNewMailNotificationData(senders: List<String> = emptyList()): NotificationData<MessageReference> {
        val activeNotifications = senders.mapIndexed { index, sender ->
            NotificationHolder(
                notificationId = index,
                timestamp = 0L,
                content = NotificationContent(
                    reference = mock<MessageReference>(),
                    sender = sender,
                    preview = "irrelevant",
                    summary = "irrelevant",
                    subject = "irrelevant"
                )
            )
        }
        return NotificationData(account, activeNotifications, inactiveNotifications = emptyList(), NotificationGroupType.NEW_MAIL)
    }

    private fun createAccount(): Account {
        val notificationSetting = NotificationSetting().apply {
            vibratePattern = 0
            vibrateTimes = 1
        }
        return mock {
            on { this.accountNumber } doReturn ACCOUNT_NUMBER
            on { name } doReturn ACCOUNT_NAME
            on { this.notificationSetting } doReturn notificationSetting
        }
    }
}
