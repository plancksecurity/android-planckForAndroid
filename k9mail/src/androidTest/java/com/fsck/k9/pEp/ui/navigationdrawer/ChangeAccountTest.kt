package com.fsck.k9.pEp.ui.navigationdrawer

import android.app.Activity
import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.MessageList
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import timber.log.Timber

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class ChangeAccountTest {

    @get:Rule
    var mActivityRule = IntentsTestRule(SplashActivity::class.java, false, false)


    lateinit var uiDevice: UiDevice
    lateinit var testUtils: TestUtils

    @Before
    fun before() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(uiDevice, InstrumentationRegistry.getInstrumentation())
        testUtils.startActivity()
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun stage1_clearAccounts() {
        uiDevice.waitForIdle()
        clearAccounts()
    }

    @Test
    fun stage2_clickAccountBubbleInfinite() {
        setupAccounts()

        while (true) {
            changeAccount()
        }
    }

    private fun clearAccounts() {
        val activity = getCurrentActivity()
        Timber.e("class is " + activity!!::class.java)

        if (activity is MessageList) {
            Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
            val settingText = activity.resources?.getString(R.string.action_settings) ?: ""
            onView(ViewMatchers.withText(settingText)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(click())
            val accountsSize = Preferences.getPreferences(activity).accounts.size
            repeat(accountsSize) {
                testUtils.goBackAndRemoveAccount()
            }
        }
    }

    private fun getCurrentActivity(): Activity? = runBlocking(Dispatchers.Main) {
        uiDevice.waitForIdle()
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        return@runBlocking Iterables.getOnlyElement(activities)
    }

    private fun changeAccount() {
        uiDevice.waitForIdle()
        testUtils.openHamburgerMenu()
        uiDevice.waitForIdle()
        sleep((1000..2000).random())
        onView(withId(R.id.first_account)).perform(click())
    }

    fun sleep(time: Int) {
        try {
            Thread.sleep(time.toLong())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun setupAccounts() {
        grantPermissions()
        passWelcomeScreen()
        testUtils.externalAppRespondWithFile(R.raw.account_folders1)
        importAccount()
        uiDevice.waitForIdle()
        testUtils.externalAppRespondWithFile(R.raw.account_folders2)
        clickAddAccountButton()
        importAccount()
    }

    private fun importAccount() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        uiDevice.waitForIdle()
        click(getString(R.string.settings_import))
        uiDevice.waitForIdle()
        click(getString(R.string.okay_action))
        uiDevice.waitForIdle()
        click(getString(R.string.okay_action))
        uiDevice.waitForIdle()
        addTextTo(R.id.incoming_server_password, BuildConfig.PEP_TEST_EMAIL_PASSWORD)
        uiDevice.waitForIdle()
        testUtils.clickAcceptButton()
        uiDevice.waitForIdle()
    }

    private fun clickAddAccountButton() {
        uiDevice.waitForIdle()
        testUtils.openHamburgerMenu()
        onView(withId(R.id.navFoldersAccountsButton)).perform(click())
        onView(withId(R.id.add_account_container)).perform(click())
        uiDevice.waitForIdle()
    }

    private fun passWelcomeScreen() {
        uiDevice.waitForIdle()
        onView(withId(R.id.skip)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(click())
        uiDevice.waitForIdle()
    }

    private fun grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getInstrumentation().context.packageName
                            + " android.permission.WRITE_EXTERNAL_STORAGE")
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getInstrumentation().context.packageName
                            + " android.permission.READ_CONTACTS")
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + InstrumentationRegistry.getInstrumentation().context.packageName
                            + " android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
            K9.setShallRequestPermissions(false)

            Thread {
                val prefs = Preferences.getPreferences(InstrumentationRegistry.getInstrumentation().context)
                val editor = prefs.storage.edit()
                K9.save(editor)
                editor.commit()
            }.start()
        }
    }


    private fun getString(resourceId: Int): String = getCurrentActivity()?.resources?.getString(resourceId)
            ?: ""

    private fun click(string: String) {
        onView(ViewMatchers.withText(string)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(click())
    }

    private fun addTextTo(resourceId: Int, text: String) {
        onView(withId(resourceId)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(ViewActions.typeText(text))
    }
}