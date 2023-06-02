package com.fsck.k9.planck.ui.activities

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.R
import com.fsck.k9.common.BaseAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val MESSAGE_SUBJECT = "subject"
private const val MESSAGE_BODY = "body"

@RunWith(AndroidJUnit4::class)
class ManyMailsTest : BaseAndroidTest() {

    @Before
    fun startpEpApp() {
        testUtils.setupAccountIfNeeded()
        testUtils.doWaitForResource(R.id.message_list)
    }

    @Test
    fun runTest() {
        sendManyMails()
    }

    private fun sendManyMails() {
        while (true) {
            sendMesssageToMyself()
            TestUtils.waitForIdle()
        }
    }

    private fun sendMesssageToMyself() {
        testUtils.composeMessageButton()
        fillMessage()
        testUtils.sendMessage()
    }

    private fun fillMessage() {
        val email = testUtils.getTextFromTextViewThatContainsText("@")
        testUtils.fillMessage(
            TestUtils.BasicMessage(
                email,
                MESSAGE_SUBJECT,
                MESSAGE_BODY,
                email
            ),
            false
        )
    }
}