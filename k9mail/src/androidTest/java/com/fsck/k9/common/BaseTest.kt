package com.fsck.k9.common

import android.app.Activity
import android.app.UiAutomation
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import com.fsck.k9.pEp.ui.activities.UtilsPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

open class BaseTest {

    lateinit var device: UiDevice
    lateinit var context: Context
    lateinit var resources: Resources
    private lateinit var uiAutomation: UiAutomation
    lateinit var testUtils: TestUtils

    private val messageListSize = IntArray(2)

    @get:Rule
    var mActivityRule = ActivityTestRule(SplashActivity::class.java)

    @Test
    fun emptyTest() {
    }

    @Before
    fun setUp() {
        Timber.e("New testcase starts here ======================>")

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = InstrumentationRegistry.getInstrumentation().targetContext
        resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        testUtils = TestUtils(device, InstrumentationRegistry.getInstrumentation())
        ViewMatchers.assertThat(context, CoreMatchers.notNullValue())
        ViewMatchers.assertThat(device, CoreMatchers.notNullValue())
        ViewMatchers.assertThat(resources, CoreMatchers.notNullValue())

        if (!device.isScreenOn) {
            // Just in case, starting up from scratch
            device.wakeUp()
            sleep(3000)
        }

        // Default is natural orientation
        device.setOrientationNatural()

        // Start from the home screen
        Intents.init()
        waitLauncher()
        waitAppLaunch()
        runBlocking {
            resetFocus()
        }
        Timber.e("Test Launch successful ================>")
    }

    private fun resetFocus() = runBlocking(Dispatchers.Main) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            Iterables.getOnlyElement(activities)?.let { activity ->
                val content = (activity.window.decorView.rootView as ViewGroup).getChildAt(0)
                val oldDefaultFocusHighlightEnabled = content.defaultFocusHighlightEnabled
                content.isFocusable = true
                content.defaultFocusHighlightEnabled = false
                content.requestFocus()
                content.clearFocus()
                content.defaultFocusHighlightEnabled = oldDefaultFocusHighlightEnabled
            }
        }
    }

    @After
    fun after() {
        Intents.release()
    }

    private fun waitAppLaunch() {
        device.wait(Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)), LAUNCH_TIMEOUT)
    }

    private fun waitLauncher() {
        val launcherPackage = device.launcherPackageName
        ViewMatchers.assertThat(launcherPackage, CoreMatchers.notNullValue())
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)
    }

    fun sleep(time: Int) {
        try {
            Thread.sleep(time.toLong())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun waitForIdle() = withContext(Dispatchers.IO) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        device.waitForIdle()
    }

    fun getCurrentActivity(): Activity? = runBlocking(Dispatchers.Main) {
        waitForIdle()
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        return@runBlocking Iterables.getOnlyElement(activities)
    }

    fun click(resourceId: Int) {
        Espresso.onView(ViewMatchers.withId(resourceId))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())
    }

    fun scrollAndClick(resourceId: Int) {
        Espresso.onView(ViewMatchers.withId(resourceId))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun longClick(resourceId: Int) {
        Espresso.onView(ViewMatchers.withId(resourceId)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(ViewActions.longClick())
    }

    fun addTextTo(resourceId: Int, text: String) {
        Espresso.onView(ViewMatchers.withId(resourceId)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(ViewActions.typeText(text))
    }

    fun setTextTo(resourceId: Int, text: String) {
        Espresso.onView(ViewMatchers.withId(resourceId)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(ViewActions.clearText(), ViewActions.typeText(text))
    }

    fun swipeScrollView() {
        Espresso.onView(Matchers.instanceOf(ScrollView::class.java)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(ViewActions.swipeUp())
    }

    fun swipeSettingsView() {
        Espresso.onView(Matchers.instanceOf(RecyclerView::class.java)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(ViewActions.swipeUp())
    }

    fun click(string: String) {
        Espresso.onView(ViewMatchers.withText(string)).check(ViewAssertions.matches(ViewMatchers.isDisplayed())).perform(ViewActions.click())
    }

    fun clickPopUpMenuItem(string: String) {
        Espresso.onView(ViewMatchers.withText(string)).inRoot(RootMatchers.isPlatformPopup()).perform(ViewActions.click())
    }

    fun clickClosedNavHamburger() {
        Espresso.onView(ViewMatchers.withContentDescription(R.string.navigation_drawer_open)).perform(ViewActions.click())
    }

    fun clickListItem(resourceId: Int, position: Int) {
        Espresso.onData(CoreMatchers.anything())
                .inAdapterView(ViewMatchers.withId(resourceId))
                .atPosition(position)
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())
    }

    fun clickSetting(stringResource: Int) {
        Espresso.onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(ViewMatchers.hasDescendant(ViewMatchers.withText(stringResource)), ViewActions.click()))
    }

    fun viewIsDisplayed(viewId: Int): Boolean {
        val isDisplayed = booleanArrayOf(true)
        Espresso.onView(ViewMatchers.withId(viewId))
                .withFailureHandler { _, _ -> isDisplayed[0] = false }
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        return isDisplayed[0]
    }

    fun getString(resourceId: Int): String = getCurrentActivity()?.resources?.getString(resourceId)
            ?: ""

    fun waitListView() {
        while (!viewIsDisplayed(R.id.fab_button_compose_message));
    }

    fun waitMessageView() {
        while (!viewIsDisplayed(R.id.openCloseButton));
    }

    fun waitMessageCompose() {
        while (!viewIsDisplayed(R.id.message_content));
    }

    fun getMessageListSize() {
        Espresso.onView(ViewMatchers.withId(R.id.message_list)).perform(UtilsPackage.saveSizeInInt(messageListSize, 0))
    }

    fun waitNewMessage() {
        var newEmail = false
        while (!newEmail) {
            try {
                Espresso.onView(ViewMatchers.withId(R.id.message_list)).perform(UtilsPackage.saveSizeInInt(messageListSize, 1))
                if (messageListSize[1] > messageListSize[0]) {
                    newEmail = true
                }
                sleep(500)
            } catch (ex: Exception) {
                Timber.i("Waiting for new message : $ex")
            }
        }
    }

    fun closeKeyboardWithDelay() {
        Espresso.closeSoftKeyboard()
        sleep(1000)
    }

    fun sendMessageToBot(botName: String) {
        val botEmail = "$botName@sq.pep.security"
        sendMessage(botEmail)
    }

    fun replyToSelfMessage() {
        clickListItem(R.id.message_list, 0)
        waitMessageView()
        click(R.id.openCloseButton)
        waitMessageCompose()
        runBlocking { waitForIdle() }
        Espresso.closeSoftKeyboard()
        click(R.id.send)
        runBlocking { waitForIdle() }
        Espresso.pressBack()
        runBlocking { waitForIdle() }
        sleep(3000)
    }

    fun sendNewMessageToSelf() {
        sendMessage(BuildConfig.PEP_TEST_EMAIL_ADDRESS)
    }

    private fun sendMessage(email: String) {
        click(R.id.fab_button_compose_message)
        waitMessageCompose()
        val message = TestUtils.BasicMessage(email, "reset", "", email)
        fillMessage(message)
        sleep(2000)
        click(R.id.send)
        sleep(3000)
    }

    private fun fillMessage(message: TestUtils.BasicMessage) {
        Espresso.closeSoftKeyboard()
        sleep(500)
        addTextTo(R.id.to, message.to)
        sleep(500)
        addTextTo(R.id.subject, message.subject)
        sleep(500)
        addTextTo(R.id.message_content, message.message)
    }

    fun allowPermissions() {
        Timber.e("allowPermissions")
        device.waitForIdle()
        try {
            val popUpMessage = By.clazz("android.widget.Button")
            var buttonExists = true
            Timber.e("while")
            while (buttonExists) {
                buttonExists = false
                device.findObjects(popUpMessage).forEach { obj ->
                    if (obj.resourceName != null && obj.resourceName == "com.android.permissioncontroller:id/permission_allow_button") {
                        buttonExists = true
                        obj.click()
                    }
                }
            }
        } catch (ex: Exception) {
            Timber.e("Cannot allow permissions")
        }
        do {
            allowPermissions(2)
            allowPermissions(1)
        } while (!UtilsPackage.viewIsDisplayed(R.id.action_continue) && !UtilsPackage.viewIsDisplayed(R.id.account_email))
    }

    private fun allowPermissions(index: Int) {
        while (true) {
            try {
                runBlocking { waitForIdle() }
                val allowPermissions = device.findObject(UiSelector()
                        .clickable(true)
                        .checkable(false)
                        .index(index))
                if (allowPermissions.exists()) {
                    allowPermissions.click()
                    runBlocking { waitForIdle() }
                } else {
                    Timber.e("There is no permissions dialog to interact with ")
                    return
                }
            } catch (ignoredException: Exception) {
                Timber.e(ignoredException, "Failed trying to allow permission")
            }
        }
    }

    companion object {
        private const val BASIC_SAMPLE_PACKAGE = "com.fsck.k9"
        private const val LAUNCH_TIMEOUT = 5000L
    }

}
