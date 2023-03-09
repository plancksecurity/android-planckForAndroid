package com.fsck.k9.pEp.ui.activities

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.fsck.k9.R
import com.fsck.k9.common.BaseAndroidTest
import org.junit.Before
import org.junit.Rule
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
    fun removeMessageThreadTest() {
        createMessageThread()
    }

    private fun createMessageThread() {
        repeat(200) {
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