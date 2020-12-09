package com.fsck.k9.pEp.ui.activities

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.pEp.EspressoTestingIdlingResource
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportSettingsAcceptTest {
    private lateinit var testUtils: TestUtils
    private lateinit var device: UiDevice
    private val accountListSize = IntArray(2)

    @get:Rule
    var splashActivityTestRule = IntentsTestRule(SplashActivity::class.java)

    @Before
    fun startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(device, InstrumentationRegistry.getInstrumentation())
        EspressoTestingIdlingResource()
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource())
        testUtils.externalAppRespondWithFile(R.raw.test)
        testUtils.setupAccountIfNeeded();
    }

    @After
    fun tearDown() {
        splashActivityTestRule.finishActivity()
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource())
    }

    @Test
    fun importSettingsWithAccountAccept() {
        testUtils.selectFromMenu(R.string.action_settings)
        Espresso.onView(ViewMatchers.withId(R.id.accounts_list)).perform(UtilsPackage.saveSizeInInt(accountListSize, 0))
        testUtils.selectFromMenu(R.string.import_export_action)
        testUtils.selectFromScreen(R.string.settings_import)
        testUtils.doWaitForAlertDialog(R.string.settings_import_selection)
        testUtils.clickAcceptButton()
        testUtils.doWaitForAlertDialog(R.string.settings_import_success_header)
        testUtils.clickAcceptButton()
        testUtils.doWaitForAlertDialog(R.string.settings_import_activate_account_header)
        Espresso.onView(ViewMatchers.withId(R.id.incoming_server_password)).perform(ViewActions.replaceText(BuildConfig.PEP_TEST_EMAIL_PASSWORD))
        testUtils.clickAcceptButton()
        device.waitForIdle()
        Espresso.onView(ViewMatchers.withId(R.id.accounts_list)).perform(UtilsPackage.saveSizeInInt(accountListSize, 1))

        checkLastAccountInSettings()
    }

    private fun checkLastAccountInSettings() {
        TestCase.assertEquals(accountListSize[0] + 1, accountListSize[1])
        val lastIndex = accountListSize[1] -1
        Espresso.onView(UtilsPackage.withRecyclerView(R.id.accounts_list).atPositionOnView(lastIndex, R.id.description))
                .check(ViewAssertions.matches(ViewMatchers.withSubstring("android04@peptest.ch")))
    }
}