package com.fsck.k9.planck.ui.activities

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.fsck.k9.R
import com.fsck.k9.common.BaseAndroidTest
import com.fsck.k9.planck.ui.activities.TestUtils.BasicMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val MESSAGE_SUBJECT = "subject"
private const val MESSAGE_BODY = "body"

@RunWith(AndroidJUnit4::class)
class RemoveMessageThreadTest : BaseAndroidTest() {
    @get:Rule
    var permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        )

    @Before
    fun startpEpApp() {
        testUtils.setupAccountIfNeeded()
        testUtils.doWaitForResource(R.id.message_list)
    }

    @Test
    fun removeMessageThreadTest() {
        createMessageThread()
        removeMessageThread()

        createMessageThread()
        removeMessagesOneByOne()
    }

    private fun createMessageThread() {
        sendMesssageToMyself()
        replyToFirstMessage()
        testUtils.pressBack()
        waitForFirstMessageThread()
    }

    private fun waitForFirstMessageThread() {
        while (!firstMessageIsThread()) {
            TestUtils.waitForIdle()
        }
    }

    private fun firstMessageIsThread(): Boolean {
        var out = true
        onView(
            UtilsPackage.withRecyclerView(R.id.message_list)
                .atPositionOnView(0, R.id.message_unread_container, R.id.threadCount)
        ).withFailureHandler { _, _ ->
            out = false
        }.check(matches(withText("2")))
        return out
    }

    private fun sendMesssageToMyself() {
        testUtils.composeMessageButton()
        fillMessage()
        testUtils.sendMessage()
    }

    private fun replyToFirstMessage() {
        testUtils.clickFirstMessage()
        TestUtils.waitForIdle()
        testUtils.clickView(R.id.openCloseButton)
        TestUtils.waitForIdle()
        onView(withId(R.id.message_content)).perform(
            typeText(MESSAGE_BODY),
            closeSoftKeyboard()
        )
        testUtils.sendMessage()
    }

    private fun fillMessage() {
        val email = testUtils.getTextFromTextViewThatContainsText("@")
        testUtils.fillMessage(
            BasicMessage(
                email,
                MESSAGE_SUBJECT,
                MESSAGE_BODY,
                email
            ),
            false
        )
    }

    private fun removeMessageThread() {
        clickFirstMessage()
        TestUtils.waitForIdle()
        onView(UtilsPackage.withRecyclerView(R.id.message_list).atPosition(0))
            .perform(longClick())

        onView(UtilsPackage.withRecyclerView(R.id.message_list).atPosition(1))
            .perform(click())
        testUtils.clickView(R.id.delete)
    }

    private fun clickFirstMessage() {
        TestUtils.waitForIdle()
        onView(UtilsPackage.withRecyclerView(R.id.message_list).atPosition(0))
            .perform(scrollTo(), click())
    }

    private fun removeMessagesOneByOne() {
        clickFirstMessage()
        clickFirstMessage()
        testUtils.clickView(R.id.delete)
        testUtils.clickView(R.id.delete)
    }
}
