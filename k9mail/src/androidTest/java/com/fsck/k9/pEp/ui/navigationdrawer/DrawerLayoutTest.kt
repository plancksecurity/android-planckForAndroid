package com.fsck.k9.pEp.ui.navigationdrawer

import androidx.test.espresso.Espresso.onView
import com.fsck.k9.R
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import org.hamcrest.core.IsNot.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DrawerLayoutTest {

    @get:Rule
    var mActivityRule = ActivityTestRule(SplashActivity::class.java)

    private lateinit var testUtils: TestUtils

    @Before
    fun before() {
        testUtils = TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()), InstrumentationRegistry.getInstrumentation())
    }


    @Test
    fun navDrawerIsNotEmpty() {
        testUtils.openHamburgerMenu()
        // click hamburger account
        onView(withId(R.id.menu_header)).check(matches(isDisplayed()))
        onView(withId(R.id.navigation_bar_folders_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.unified_inbox)).check(matches(isDisplayed()))
        onView(withId(R.id.all_messages_container)).check(matches(isDisplayed()))
        onView(withId(R.id.navigation_folders)).check(matches(isDisplayed()))
        onView(withId(R.id.navigation_folders)).check(matches(not(hasChildCount(0))))
    }

    fun clickAccountBall() {
        // click first account
        // check if drawer header changed to selected account
        // check if drawer accounts changed to other accounts
        // check if drawer folders changed to selected account
    }

    fun changeAccountFolders() {
        // click button to show accounts
        // check if accounts are visible
        // click button to show folders
        // check if folders are visible
    }

    fun clickFolders() {
        // click unified account
        // check if unified messages is showing in messageList
        // click all messages
        // check if all messages is showing in messageList
        // click inbox folder
        // check if inbox is showing in messageList
        // click <last folder> after inbox
        // check if <last folder> is showing in messageList
    }

    fun clickAccountInList() {
        // click first account in list
        // check if drawer folders are visible
        // check if drawer header changed to selected account
        // check if drawer accounts changed to other accounts
        // check if drawer folders changed to selected account
    }

    fun clickAddAccount() {
        // click add account button
        // check if add account opened
    }

    fun clickSettings() {
        // click settings button
        // check if settings opened
    }
}