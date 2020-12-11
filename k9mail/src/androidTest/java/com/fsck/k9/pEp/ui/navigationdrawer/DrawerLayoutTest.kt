package com.fsck.k9.pEp.ui.navigationdrawer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.R
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.common.GetNavigationFolderTextAction
import com.fsck.k9.common.GetTextViewTextAction
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import com.schibsted.spain.barista.internal.matcher.HelperMatchers.atPosition
import org.hamcrest.core.IsNot.not
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/* Pre requirements
 *       Add 3 accounts that have different folders count
 *       ie. account1 -> 3 folders , account2 -> 4 folders, account3 -> 5 folders
 * */

@RunWith(AndroidJUnit4::class)
class DrawerLayoutTest {

    @get:Rule
    var mActivityRule = ActivityTestRule(SplashActivity::class.java)

    private lateinit var testUtils: TestUtils
    private lateinit var uiDevice: UiDevice

    @Before
    fun before() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(uiDevice, InstrumentationRegistry.getInstrumentation())
    }


    @Test
    fun navDrawerIsNotEmpty() {
        testUtils.openHamburgerMenu()
        onView(withId(R.id.menu_header)).check(matches(isDisplayed()))
        onView(withId(R.id.navigation_bar_folders_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.unified_inbox)).check(matches(isDisplayed()))
        onView(withId(R.id.all_messages_container)).check(matches(isDisplayed()))
        onView(withId(R.id.navigation_folders)).check(matches(isDisplayed()))
        onView(withId(R.id.navigation_folders)).check(matches(not(hasChildCount(0))))
    }

    @Test
    fun clickAccountBall() {
        testUtils.openHamburgerMenu()

        var email = getTextFromComponent(R.id.nav_header_email)
        var currentBallText = getTextFromComponent(R.id.nav_header_contact_text)
        var secondBallText = getTextFromComponent(R.id.second_account)
        var firstBallText = getTextFromComponent(R.id.first_account)
        var foldersSize = testUtils.getListSize(R.id.navigation_folders)

        onView(withId(R.id.second_account_container)).perform(click())
        uiDevice.waitForIdle()
        testUtils.openHamburgerMenu()
        uiDevice.waitForIdle()
        onView(withId(R.id.nav_header_email)).check(matches(not(withText(email))))
        onView(withId(R.id.nav_header_contact_text)).check(matches(withText(secondBallText)))
        onView(withId(R.id.second_account)).check(matches(withText(currentBallText)))
        onView(withId(R.id.first_account)).check(matches(withText(firstBallText)))
        assertTrue(foldersSize != testUtils.getListSize(R.id.navigation_folders))

        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        for (position in 0 until testUtils.getListSize(R.id.navigation_accounts)) {
            onView(withId(R.id.navigation_accounts))
                    .check(matches(atPosition(0, hasDescendant(withText(email)))))
        }
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())

        email = getTextFromComponent(R.id.nav_header_email)
        currentBallText = getTextFromComponent(R.id.nav_header_contact_text)
        secondBallText = getTextFromComponent(R.id.second_account)
        firstBallText = getTextFromComponent(R.id.first_account)
        foldersSize = testUtils.getListSize(R.id.navigation_folders)

        onView(withId(R.id.first_account_container)).perform(click())
        uiDevice.waitForIdle()
        testUtils.openHamburgerMenu()
        uiDevice.waitForIdle()
        onView(withId(R.id.nav_header_email)).check(matches(not(withText(email))))
        onView(withId(R.id.nav_header_contact_text)).check(matches(withText(firstBallText)))
        onView(withId(R.id.second_account)).check(matches(withText(currentBallText)))
        onView(withId(R.id.first_account)).check(matches(withText(secondBallText)))
        assertTrue(foldersSize != testUtils.getListSize(R.id.navigation_folders))

        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        for (position in 0 until testUtils.getListSize(R.id.navigation_accounts)) {
            onView(withId(R.id.navigation_accounts))
                    .check(matches(atPosition(0, hasDescendant(withText(email)))))
        }
    }

    private fun getTextFromComponent(resourceId: Int): String {
        val action = GetTextViewTextAction()
        onView(withId(resourceId)).perform(action)
        return action.text.toString()
    }

    @Test
    fun changeAccountFolders() {
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navigation_folders)).check(matches(isDisplayed()))

        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.navigation_accounts)).check(matches(isDisplayed()))

        onView(withId(R.id.menu_header)).perform(click())
        onView(withId(R.id.navigation_folders)).check(matches(isDisplayed()))
    }

    @Test
    fun clickFolders() {
        // click unified account
        testUtils.openHamburgerMenu()
        onView(withId(R.id.unified_inbox)).perform(click())
        uiDevice.waitForIdle()
        onView(withId(R.id.actionbar_title_first)).check(matches(withText(testUtils.getString(R.string.integrated_inbox_title))))
        // click all messages
        testUtils.openHamburgerMenu()
        onView(withId(R.id.all_messages_container)).perform(click())
        uiDevice.waitForIdle()
        onView(withId(R.id.actionbar_title_first)).check(matches(withText(testUtils.getString(R.string.search_all_messages_title))))
        // click inbox folder
        testUtils.openHamburgerMenu()
        val action = GetNavigationFolderTextAction()
        uiDevice.waitForIdle()
        onView(withId(R.id.navigation_folders))
                .check(matches(isDisplayed()))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, action),
                        actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        uiDevice.waitForIdle()
        onView(withId(R.id.actionbar_title_first)).check(matches(withText(action.text.toString())))
        // click last folder
        testUtils.openHamburgerMenu()
        uiDevice.waitForIdle()
        val size = testUtils.getListSize(R.id.navigation_folders)
        onView(withId(R.id.navigation_folders))
                .check(matches(isDisplayed()))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(size - 1, action),
                        actionOnItemAtPosition<RecyclerView.ViewHolder>(size - 1, click()))
        uiDevice.waitForIdle()
        onView(withId(R.id.actionbar_title_first)).check(matches(withText(action.text.toString())))
    }

    fun clickAccountInList() {
        // click first account in list
        // check if drawer folders are visible
        // check if drawer header changed to selected account
        // check if drawer accounts changed to other accounts
        // check if drawer folders changed to selected account
    }

    @Test
    fun clickAddAccount() {
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.add_account_container)).perform(click())
        uiDevice.waitForIdle()
        assertTrue(testUtils.currentActivity is AccountSetupBasics)
    }

    @Test
    fun clickSettings() {
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.configure_account_container)).perform(click())
        uiDevice.waitForIdle()
        assertTrue(testUtils.currentActivity is SettingsActivity)
    }
}