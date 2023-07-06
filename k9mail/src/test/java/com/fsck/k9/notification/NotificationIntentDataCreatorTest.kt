package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.RobolectricTest
import com.fsck.k9.activity.MessageReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import security.planck.notification.GroupMailInvite

private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"
private const val ACCOUNT_UUID = "1-2-3"
private const val FOLDER_NAME = "folder"
private const val UID1 = "UID1"
private const val UID2 = "UID2"
private const val SENDER = "sender"
private const val GROUP1 = "GROUP1"
private const val GROUP2 = "GROUP2"
private const val STEP = 10L

class NotificationIntentDataCreatorTest: RobolectricTest() {
    private val dataCreator = NotificationIntentDataCreator()
    private val account = createFakeAccount()
    private val message1 = createMessageReference(UID1)
    private val message2 = createMessageReference(UID2)
    private val groupMailInvite1 = createGroupInvite(GROUP1)
    private val groupMailInvite2 = createGroupInvite(GROUP2)

    @Test
    fun `getDismissMessageData is unique for each message`() {
        val uri1 = dataCreator.getDismissMessageData(message1)
        val uri2 = dataCreator.getDismissMessageData(message2)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getDismissAllMessagesData is unique each time`() {
        val uri1 = dataCreator.getDismissAllMessagesData(account)
        runBlocking { delay(STEP) }
        val uri2 = dataCreator.getDismissAllMessagesData(account)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getDismissGroupMailData is unique for each group event`() {
        val uri1 = dataCreator.getDismissGroupMailData(groupMailInvite1)
        val uri2 = dataCreator.getDismissGroupMailData(groupMailInvite2)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getDismissAllGroupMailData is unique each time`() {
        val uri1 = dataCreator.getDismissAllGroupMailData(account)
        runBlocking { delay(STEP) }
        val uri2 = dataCreator.getDismissAllGroupMailData(account)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getReplyMessageData is unique for each message`() {
        val uri1 = dataCreator.getReplyMessageData(message1)
        val uri2 = dataCreator.getReplyMessageData(message2)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getMarkMessageAsReadData is unique for each message`() {
        val uri1 = dataCreator.getMarkMessageAsReadData(message1)
        val uri2 = dataCreator.getMarkMessageAsReadData(message2)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getMarkAllMessagesAsReadData is unique each time`() {
        val uri1 = dataCreator.getMarkAllMessagesAsReadData(account)
        val uri2 = dataCreator.getMarkAllMessagesAsReadData(account)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getDeleteMessageData is unique for each message`() {
        val uri1 = dataCreator.getDeleteMessageData(message1)
        val uri2 = dataCreator.getDeleteMessageData(message2)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getDeleteMessageConfirmationData is unique for each message`() {
        val uri1 = dataCreator.getDeleteMessageConfirmationData(message1)
        val uri2 = dataCreator.getDeleteMessageConfirmationData(message2)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getDeleteAllMessagesData is unique each time`() {
        val uri1 = dataCreator.getDeleteAllMessagesData(account)
        runBlocking { delay(STEP) }
        val uri2 = dataCreator.getDeleteAllMessagesData(account)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getDeleteAllMessageConfirmationData is unique each time`() {
        val uri1 = dataCreator.getDeleteAllMessageConfirmationData()
        runBlocking { delay(STEP) }
        val uri2 = dataCreator.getDeleteAllMessageConfirmationData()

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getArchiveMessageData is unique for each message`() {
        val uri1 = dataCreator.getArchiveMessageData(message1)
        val uri2 = dataCreator.getArchiveMessageData(message2)

        assertNotEquals(uri1, uri2)
    }

    @Test
    fun `getArchiveAllMessagesData is unique each time`() {
        val uri1 = dataCreator.getArchiveAllMessagesData(account)
        runBlocking { delay(STEP) }
        val uri2 = dataCreator.getArchiveAllMessagesData(account)

        assertNotEquals(uri1, uri2)
    }

    private fun createFakeAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { name } doReturn ACCOUNT_NAME
            on { uuid } doReturn ACCOUNT_UUID
        }
    }

    private fun createMessageReference(uid: String): MessageReference {
        return MessageReference(ACCOUNT_UUID, FOLDER_NAME, uid, null)
    }

    private fun createGroupInvite(group: String): GroupMailInvite {
        return GroupMailInvite(group, SENDER, ACCOUNT_UUID)
    }
}