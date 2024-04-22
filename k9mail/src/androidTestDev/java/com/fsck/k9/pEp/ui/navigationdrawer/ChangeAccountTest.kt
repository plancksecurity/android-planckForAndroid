package com.fsck.k9.planck.ui.navigationdrawer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.common.BaseAndroidTest
import com.fsck.k9.planck.ui.activities.TestUtils
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class ChangeAccountTest: BaseAndroidTest() {

    @Test
    fun stage1_clearAccounts() {
        testUtils.goToSettingsAndRemoveAllAccountsIfNeeded()
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST * 3)
    fun stage2_clickAccountBubble30Times() {
        setupAccounts()
        repeat(30) {
            changeAccount()
        }
        testUtils.goToSettingsAndRemoveAllAccounts()
    }

    private fun changeAccount() {
        TestUtils.waitForIdle()
        testUtils.openHamburgerMenu()
        TestUtils.waitForIdle()
        sleep((1000..2000).random())
        TestUtils.waitForIdle()
        onView(withId(R.id.first_account)).perform(click())
        TestUtils.waitForIdle()
    }

    fun sleep(time: Int) {
        try {
            Thread.sleep(time.toLong())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupAccounts() {
        testUtils.externalAppRespondWithFile(R.raw.account_folders1)
        importAccount()
        TestUtils.waitForIdle()
        testUtils.externalAppRespondWithFile(R.raw.account_folders2)
        clickAddAccountButton()
        importAccount()
    }

    private fun importAccount() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        TestUtils.waitForIdle()
        click(getString(R.string.settings_import))
        TestUtils.waitForIdle()
        click(getString(R.string.okay_action))
        TestUtils.waitForIdle()
        click(getString(R.string.okay_action))
        TestUtils.waitForIdle()
        addTextTo(R.id.incoming_server_password, BuildConfig.PLANCK_TEST_EMAIL_PASSWORD)
        TestUtils.waitForIdle()
        testUtils.clickAcceptButton()
        TestUtils.waitForIdle()
    }

    private fun clickAddAccountButton() {
        TestUtils.waitForIdle()
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.add_account_container)).perform(click())
        TestUtils.waitForIdle()
    }

    private fun getString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<Context>().resources.getString(resourceId)

    private fun click(string: String) {
        onView(ViewMatchers.withText(string)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(click())
    }

    private fun addTextTo(resourceId: Int, text: String) {
        onView(withId(resourceId)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.typeText(text))
    }
}