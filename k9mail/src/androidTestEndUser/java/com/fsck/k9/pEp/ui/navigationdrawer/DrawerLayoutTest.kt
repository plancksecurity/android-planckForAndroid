package com.fsck.k9.planck.ui.navigationdrawer

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.R
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.common.GetNavigationAccountEmailAction
import com.fsck.k9.common.GetNavigationFolderTextAction
import com.fsck.k9.common.GetTextViewTextAction
import com.fsck.k9.planck.ui.activities.SplashActivity
import com.fsck.k9.planck.ui.activities.TestUtils
import com.schibsted.spain.barista.internal.matcher.HelperMatchers.atPosition
import org.hamcrest.CoreMatchers
import org.hamcrest.core.IsNot.not
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class DrawerLayoutTest : SetupDevTestAccounts() {

    @get:Rule
    var mActivityRule = IntentsTestRule(SplashActivity::class.java, false, false)

    @Before
    fun before() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(uiDevice, InstrumentationRegistry.getInstrumentation())
        mActivityRule.launchActivity(Intent())
        testUtils.skipTutorialAndAllowPermissionsIfNeeded()
    }

    @After
    fun tearDown() {
        mActivityRule.finishActivity()
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    fun stage1_clearAccounts() {
        TestUtils.waitForIdle()
        clearAccounts()
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    fun stage2_setupAccount() {
        TestUtils.waitForIdle()
        setupAccounts()
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    fun stage3_navDrawerIsNotEmpty() {
        testUtils.openHamburgerMenu()
        onView(withId(R.id.menu_header)).check(matches(isDisplayed()))
        onView(withId(R.id.navigation_bar_folders_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.unified_inbox)).check(matches(isDisplayed()))
        onView(withId(R.id.all_messages_container)).check(matches(isDisplayed()))
        onView(withId(R.id.navigation_folders)).check(matches(isDisplayed()))
        onView(withId(R.id.navigation_folders)).check(matches(not(hasChildCount(0))))
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    @Ignore("ignoring until P4A-1212 is done")
    fun stage3_clickAccountBall() {
        // TODO https://pep.foundation/jira/browse/P4A-1212
        testUtils.openHamburgerMenu()

        var email = getTextFromComponent(R.id.nav_header_email)
        var currentBallText = getTextFromComponent(R.id.nav_header_contact_text)
        var secondBallText = getTextFromComponent(R.id.second_account)
        var firstBallText = getTextFromComponent(R.id.first_account)
        var foldersSize = testUtils.getListSize(R.id.navigation_folders)

        onView(withId(R.id.second_account_container)).perform(click())
        TestUtils.waitForIdle()
        testUtils.openHamburgerMenu()
        TestUtils.waitForIdle()
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
        TestUtils.waitForIdle()
        testUtils.openHamburgerMenu()
        TestUtils.waitForIdle()
        onView(withId(R.id.nav_header_email)).check(matches(not(withText(email))))
        onView(withId(R.id.nav_header_contact_text)).check(matches(withText(firstBallText)))
        onView(withId(R.id.second_account)).check(matches(withText(currentBallText)))
        onView(withId(R.id.first_account)).check(matches(withText(secondBallText)))
        assertTrue(foldersSize != testUtils.getListSize(R.id.navigation_folders))

        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        for (position in 0 until testUtils.getListSize(R.id.navigation_accounts)) {
            onView(withId(R.id.navigation_accounts))
                    .check(matches(atPosition(position, hasDescendant(withText(email)))))
        }
    }

    private fun getTextFromComponent(resourceId: Int): String {
        val action = GetTextViewTextAction()
        onView(withId(resourceId)).perform(action)
        return action.text.toString()
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    fun stage3_changeAccountFolders() {
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navigation_folders)).check(matches(isDisplayed()))

        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.navigation_accounts)).check(matches(isDisplayed()))

        onView(withId(R.id.menu_header)).perform(click())
        onView(withId(R.id.navigation_folders)).check(matches(isDisplayed()))
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    fun stage3_clickFolders() {
        // click unified account
        testUtils.openHamburgerMenu()
        onView(withId(R.id.unified_inbox)).perform(click())
        TestUtils.waitForIdle()
        onView(withId(R.id.actionbar_title_first)).check(matches(withText(testUtils.getString(R.string.integrated_inbox_title))))
        // click all messages
        testUtils.openHamburgerMenu()
        onView(withId(R.id.all_messages_container)).perform(click())
        TestUtils.waitForIdle()
        onView(withId(R.id.actionbar_title_first)).check(matches(withText(testUtils.getString(R.string.search_all_messages_title))))
        // click inbox folder
        testUtils.openHamburgerMenu()
        val action = GetNavigationFolderTextAction()
        TestUtils.waitForIdle()
        onView(withId(R.id.navigation_folders))
                .check(matches(isDisplayed()))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, action),
                        actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        TestUtils.waitForIdle()
        onView(withId(R.id.actionbar_title_first)).check(matches(withText(action.text.toString())))
        // click last folder
        testUtils.openHamburgerMenu()
        TestUtils.waitForIdle()
        val size = testUtils.getListSize(R.id.navigation_folders)
        onView(withId(R.id.navigation_folders))
                .check(matches(isDisplayed()))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(size - 1, action),
                        actionOnItemAtPosition<RecyclerView.ViewHolder>(size - 1, click()))
        TestUtils.waitForIdle()
        onView(withId(R.id.actionbar_title_first)).check(matches(withText(action.text.toString())))

    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    fun stage3_clickAccountInList() {
        testUtils.openHamburgerMenu()

        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.navigation_accounts)).check(matches(isDisplayed()))

        val action = GetNavigationAccountEmailAction()

        onView(withId(R.id.navigation_accounts))
                .check(matches(isDisplayed()))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, action),
                        actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        val email = action.text.toString()

        testUtils.openHamburgerMenu()
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.navigation_accounts)).check(matches(isDisplayed()))

        TestUtils.waitForIdle()
        onView(withId(R.id.nav_header_email)).check(matches(withText(email)))
        onView(withId(R.id.navigation_accounts))
                .check(matches(CoreMatchers.anyOf(withChild(not(withText(email))), not(withText(email)))))
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    fun stage3_clickAddAccount() {
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.add_account_container)).perform(click())
        TestUtils.waitForIdle()
        assertTrue(TestUtils.getCurrentActivity() is AccountSetupBasics)
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    fun stage3_clickSettings() {
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.configure_account_container)).perform(click())
        TestUtils.waitForIdle()
        assertTrue(TestUtils.getCurrentActivity() is SettingsActivity)
    }
}