package com.fsck.k9.pEp.ui.feedback

import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import org.junit.Test
import security.pEp.ui.feedback.FeedbackMessageInfo
import java.util.*


class FeedbackMessageInfoTest {
    val message: Message = mock()

    @Test
    fun `fromMessage converts all message fields to string with the expected format`() {
        stubMessage(
            "subject",
            arrayOf(getAdressStub("from@test.ts")),
            arrayOf(getAdressStub("to1@test.ts"), getAdressStub("to2@test.ts")),
            arrayOf(getAdressStub("cc1@test.ts"), getAdressStub("cc2@test.ts")),
            arrayOf(getAdressStub("bcc1@test.ts"), getAdressStub("bcc2@test.ts")),
            Date(16)
        )
        val feedbackMessageInfo = FeedbackMessageInfo.fromMessage(message)
        val info = feedbackMessageInfo
        assertEquals("subject", info.subject)
        assertEquals("from@test.ts", info.from)
        assertEquals("to1@test.ts, to2@test.ts", info.recipients.to)
        assertEquals("cc1@test.ts, cc2@test.ts", info.recipients.cc)
        assertEquals("bcc1@test.ts, bcc2@test.ts", info.recipients.bcc)
        assertEquals(Date(16).toString(), info.date)
    }

    @Suppress("SameParameterValue")
    private fun stubMessage(
        subject: String,
        from: Array<Address>,
        to: Array<Address>,
        cc: Array<Address>,
        bcc: Array<Address>,
        date: Date
    ) {
        doReturn(subject).`when`(message).subject
        doReturn(from).`when`(message).from
        doReturn(to).`when`(message).getRecipients(Message.RecipientType.TO)
        doReturn(cc).`when`(message).getRecipients(Message.RecipientType.CC)
        doReturn(bcc).`when`(message).getRecipients(Message.RecipientType.BCC)
        doReturn(date).`when`(message).sentDate
    }

    private fun getAdressStub(email: String): Address {
        val address: Address = mock()
        doReturn(email).`when`(address).address
        return address
    }
}