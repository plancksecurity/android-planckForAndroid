package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.MockHelper.mockBuilder
import com.fsck.k9.RobolectricTest
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.planck.infrastructure.exceptions.AppDidntEncryptMessageException
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"
private const val OUTBOX_FOLDER = "outbox"
private const val DRAFTS_FOLDER = "drafts"
private const val MESSAGE_UID = "UID"
private const val ACCOUNT_UUID = "UUID"

class SendFailedNotificationControllerTest : RobolectricTest() {
    private val resourceProvider: NotificationResourceProvider = TestNotificationResourceProvider()
    private val notification = mock<Notification>()
    private val lockScreenNotification = mock<Notification>()
    private val builder = createFakeNotificationBuilder(notification)
    private val lockScreenNotificationBuilder = createFakeNotificationBuilder(lockScreenNotification)
    private val account = createFakeAccount()
    private val contentIntent = mock<PendingIntent>()
    private val notificationId = NotificationIds.getSendFailedNotificationId(account)
    private val notificationHelper = createFakeNotificationHelper(builder, lockScreenNotificationBuilder)
    private val controller = SendFailedNotificationController(
        notificationHelper = notificationHelper,
        actionBuilder = createActionBuilder(contentIntent),
        resourceProvider = resourceProvider
    )

    @Test
    fun testShowSendFailedNotification() {
        val exception = Exception()

        controller.showSendFailedNotification(account, exception)

        verify(notificationHelper).notify(account, notificationId, notification)
        verify(builder).setSmallIcon(resourceProvider.iconWarning)
        verify(builder).setTicker("Failed to send some messages")
        verify(builder).setContentTitle("Failed to send some messages")
        verify(builder).setContentText("Exception")
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Failed to send some messages")
        verify(lockScreenNotificationBuilder, never()).setContentText(any())
        verify(lockScreenNotificationBuilder, never()).setTicker(any())
    }

    @Test
    fun testShowAppCouldNotEncryptMessageNotification() {
        val exception = createFakeAppDidntEncryptException()

        controller.showSendFailedNotification(account, exception)

        verify(notificationHelper).notify(account, notificationId, notification)
        verify(builder).setSmallIcon(resourceProvider.iconWarning)
        verify(builder).setTicker("Could not encrypt title")
        verify(builder).setContentTitle("Could not encrypt title")
        verify(builder).setContentText("Could not encrypt text")
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Failed to send some messages")
        verify(lockScreenNotificationBuilder, never()).setContentText(any())
        verify(lockScreenNotificationBuilder, never()).setTicker(any())
    }

    @Test
    fun testClearSendFailedNotification() {
        controller.clearSendFailedNotification(account)

        verify(notificationHelper).cancel(notificationId)
    }

    private fun createFakeNotificationBuilder(notification: Notification): NotificationCompat.Builder {
        return mockBuilder {
            on { build() } doReturn notification
        }
    }

    private fun createFakeNotificationHelper(
        notificationBuilder: NotificationCompat.Builder,
        lockScreenNotificationBuilder: NotificationCompat.Builder
    ): NotificationHelper {
        return mock {
            on { createNotificationBuilder(any(), any()) }.doReturn(notificationBuilder, lockScreenNotificationBuilder)
        }
    }

    private fun createFakeAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { name } doReturn ACCOUNT_NAME
            on { outboxFolderName } doReturn OUTBOX_FOLDER
            on { draftsFolderName } doReturn DRAFTS_FOLDER
            on { uuid } doReturn ACCOUNT_UUID
        }
    }

    private fun createFakeAppDidntEncryptException(): AppDidntEncryptMessageException {
        val mimeMessage: MimeMessage = mock {
            on { uid } doReturn MESSAGE_UID
        }
        return mock {
            on { this.mimeMessage } doReturn mimeMessage
        }
    }

    private fun createActionBuilder(contentIntent: PendingIntent): NotificationActionCreator {
        return mock {
            on { createViewFolderPendingIntent(any(), anyString()) } doReturn contentIntent
            on { createMessageComposePendingIntent(any()) } doReturn contentIntent
        }
    }
}