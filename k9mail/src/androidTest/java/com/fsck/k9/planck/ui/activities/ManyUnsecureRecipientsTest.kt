package com.fsck.k9.planck.ui.activities

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.R
import com.fsck.k9.common.BaseAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val NUMBER_OF_RECIPIENTS = 100

@RunWith(AndroidJUnit4::class)
class ManyUnsecureRecipientsTest : BaseAndroidTest() {

    @Before
    fun startpEpApp() {
        testUtils.setupAccountIfNeeded()
        testUtils.doWaitForResource(R.id.message_list)
    }

    @Test
    fun runTest() {
        fillMessageWithUnsecureRecipientsAndRemoveThem(
            "subject",
            "body",
            NUMBER_OF_RECIPIENTS
        )
    }

    private fun fillMessageWithUnsecureRecipientsAndRemoveThem(
        subject: String,
        message: String,
        recipients: Int,
    ) {
        testUtils.composeMessageButton()
        testUtils.doWait("to")
        TestUtils.waitForIdle()
        onView(withId(R.id.subject)).perform(replaceText(subject))
        TestUtils.waitForIdle()
        onView(withId(R.id.message_content)).perform(typeText(message))
        TestUtils.waitForIdle()
        onView(withId(R.id.to)).perform(typeText(
            (1..recipients).joinToString(" ", postfix = " ") { "t$it@t.ch" }
        ))

        while (true) {
            TestUtils.waitForIdle()
            onView(withId(R.id.message_content)).perform(scrollTo(), click()) // collapse recipients
            TestUtils.waitForIdle()
            onView(withId(R.id.to_label)).perform(scrollTo(), click()) // expand recipients
        }
    }
}