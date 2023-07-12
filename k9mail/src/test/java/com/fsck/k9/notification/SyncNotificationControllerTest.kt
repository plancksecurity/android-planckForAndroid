package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.MockHelper.mockBuilder
import com.fsck.k9.RobolectricTest
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.notification.NotificationIds.getFetchingMailNotificationId
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"
private const val FOLDER_NAME = "Inbox"
private const val OUTBOX_FOLDER = "outbox"

class SyncNotificationControllerTest : RobolectricTest() {
    private val resourceProvider: NotificationResourceProvider = TestNotificationResourceProvider()
    private val notification = mock<Notification>()
    private val lockScreenNotification = mock<Notification>()
    private val builder = createFakeNotificationBuilder(notification)
    private val lockScreenNotificationBuilder = createFakeNotificationBuilder(lockScreenNotification)
    private val account = createFakeAccount()
    private val contentIntent = mock<PendingIntent>()
    private val notificationHelper = createFakeNotificationHelper(builder, lockScreenNotificationBuilder)
    private val controller = SyncNotificationController(
        notificationHelper = notificationHelper,
        actionBuilder = createActionBuilder(contentIntent),
        resourceProvider = resourceProvider
    )

    @Test
    fun testShowSendingNotification() {
        val notificationId = getFetchingMailNotificationId(account)

        controller.showSendingNotification(account)

        verify(notificationHelper).notify(account, notificationId, notification)
        verify(builder).setSmallIcon(resourceProvider.iconSendingMail)
        verify(builder).setTicker("Sending mail: $ACCOUNT_NAME")
        verify(builder).setContentTitle("Sending mail")
        verify(builder).setContentText(ACCOUNT_NAME)
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Sending mail")
        verify(lockScreenNotificationBuilder, never()).setContentText(any())
        verify(lockScreenNotificationBuilder, never()).setTicker(any())
    }

    @Test
    fun testClearSendingNotification() {
        val notificationId = getFetchingMailNotificationId(account)

        controller.clearSendingNotification(account)

        verify(notificationHelper).cancel(notificationId)
    }

    @Test
    fun testGetFetchingMailNotificationId() {
        val localFolder = createFakeLocalFolder()
        val notificationId = getFetchingMailNotificationId(account)

        controller.showFetchingMailNotification(account, localFolder)

        verify(notificationHelper).notify(account, notificationId, notification)
        verify(builder).setSmallIcon(resourceProvider.iconCheckingMail)
        verify(builder).setTicker("Checking mail: $ACCOUNT_NAME:$FOLDER_NAME")
        verify(builder).setContentTitle("Checking mail")
        verify(builder).setContentText("$ACCOUNT_NAME:$FOLDER_NAME")
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Checking mail")
        verify(lockScreenNotificationBuilder, never()).setContentText(any())
        verify(lockScreenNotificationBuilder, never()).setTicker(any())
    }

    @Test
    fun testClearSendFailedNotification() {
        val notificationId = getFetchingMailNotificationId(account)

        controller.clearFetchingMailNotification(account)

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
            on { name } doReturn ACCOUNT_NAME
            on { outboxFolderName } doReturn OUTBOX_FOLDER
        }
    }

    private fun createActionBuilder(contentIntent: PendingIntent): NotificationActionCreator {
        return mock {
            on { createViewFolderPendingIntent(eq(account), anyString()) } doReturn contentIntent
        }
    }

    private fun createFakeLocalFolder(): LocalFolder {
        return mock {
            on { name } doReturn FOLDER_NAME
        }
    }
}