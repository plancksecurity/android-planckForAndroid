package com.fsck.k9.ui

import android.Manifest.permission.*
import android.app.Activity
import android.app.ActivityManager
import android.app.Instrumentation
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ScrollView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralLocation.*
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press.FINGER
import androidx.test.espresso.action.Swipe.FAST
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.MessageList
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.common.ChildViewAction.clickChildViewWithId
import com.fsck.k9.pEp.ui.activities.SplashActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.core.IsNot
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.File


open class BaseScreenshotTest {

    lateinit var device: UiDevice

    @get:Rule
    var splashRule =
            IntentsTestRule(SplashActivity::class.java, false, false)


    @get:Rule
    var permissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, READ_CONTACTS, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)


    /**
     * Wake up device and remove screenlock if needed
     * Set portrait orientation
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        Log.e("Test", "New testcase starts here ======================>")
        device = UiDevice.getInstance(getInstrumentation())
        assertThat(device, CoreMatchers.notNullValue())
        if (!device.isScreenOn) {
            // Just in case, starting up from scratch
            device.wakeUp()
            sleep(3000)
        }

        // Default is natural orientation
        device.setOrientationNatural()

        // Start from the home screen
        device.pressHome()
        try {
            waitLauncher()
            waitAppLaunch()
        } catch (e: Exception) {
            Timber.e("Launch failed ================>")
            Timber.e(e)
        }

        K9.setShallRequestPermissions(false)

        Timber.e("Test", "Launch successful ================>")
    }

    fun grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + getInstrumentation().context.packageName
                            + " android.permission.WRITE_EXTERNAL_STORAGE")
            getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + getInstrumentation().context.packageName
                            + " android.permission.READ_CONTACTS")
            getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant " + getInstrumentation().context.packageName
                            + " android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
            K9.setShallRequestPermissions(false)

            Thread(Runnable {
                val prefs = Preferences.getPreferences(getInstrumentation().context)
                val editor = prefs.storage.edit()
                K9.save(editor)
                editor.commit()
            }).start()
        }
    }

    @Test
    fun emptyTest() {
    }

    private fun waitAppLaunch() {
        device.wait(Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)), LAUNCH_TIMEOUT)
    }

    private fun waitLauncher() {
        val launcherPackage = device.launcherPackageName
        assertThat(launcherPackage, CoreMatchers.notNullValue())
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)
    }

    private fun launchApp() {
        // Launch the app
        val context = getInstrumentation().context
        val intent = context.packageManager.getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)
        // Clear out any previous instances
        context.startActivity(intent)
    }

    private fun clearAppData() {
        try {
            // clearing app data
            if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                (getInstrumentation().context.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            } else {
                val packageName = getApplicationContext<Context>().packageName
                val runtime = Runtime.getRuntime()
                runtime.exec("pm clear $packageName")
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        // Default is natural orientation (if a testcase fails)
        device.setOrientationNatural()
        device.pressHome()
        Timber.e("Testcase stops here <===========================")
    }

    fun openFirstScreen() {
        val intent = Intent()
        splashRule.launchActivity(intent)
        sleep(2000)
        getScreenShotCurrentActivity("splash")
        sleep(2000)
        runBlocking { waitForIdle() }
    }

    private fun getScreenShot(className: String, action: String) {
        sleep(500) // Wait for screen to change
        val imageDir = File(IMAGE_DIR)
        if (!imageDir.exists()) imageDir.mkdir()
        val index = testSet + "%2d".format(count++)
        device.takeScreenshot(File("$IMAGE_DIR$index $className ${action}.png"), 0.5f, 25)
        Timber.e("Screenshot #" + (count - 1))
    }

    fun sleep(time: Int) {
        try {
            Thread.sleep(time.toLong())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getScreenShotMessageList(action: String) = runBlocking(Dispatchers.Main) {
        waitForIdle()
        Timber.e("getScreenShotMessageList $action")
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        Iterables.getOnlyElement(activities)?.let { currentActivity ->
            if (currentActivity is MessageList) {
                val name = currentActivity::class.java.simpleName + " " + currentActivity.currentMessageView
                getScreenShot(name, action)

            }
        }
    }

    fun getScreenShotAccountSetup(action: String) = runBlocking(Dispatchers.Main) {
        waitForIdle()
        Timber.e("getScreenShotAccountSetup $action")
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        Iterables.getOnlyElement(activities)?.let { currentActivity ->
            if (currentActivity is AccountSetupBasics) {
                val name = currentActivity::class.java.simpleName + " " + currentActivity.visibleFragmentSimpleName
                getScreenShot(name, action)

            }
        }
    }

    @Throws(Throwable::class)
    fun getScreenShotCurrentActivity(action: String) = runBlocking(Dispatchers.Main) {
        waitForIdle()
        Timber.e("getScreenShotCurrentActivity $action")
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        Iterables.getOnlyElement(activities)?.let { currentActivity ->
            getScreenShot(currentActivity::class.java.simpleName, action)
        }
    }

    suspend fun waitForIdle() = withContext(Dispatchers.IO) {
        getInstrumentation().waitForIdleSync()
    }

    fun getCurrentActivity(): Activity? = runBlocking(Dispatchers.Main) {
        waitForIdle()
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        return@runBlocking Iterables.getOnlyElement(activities)
    }

    fun addTextTo(resourceId: Int, text: String) {
        onView(withId(resourceId)).check(matches(isDisplayed())).perform(typeText(text))
    }

    fun setTextTo(resourceId: Int, text: String) {
        onView(withId(resourceId)).check(matches(isDisplayed())).perform(clearText(), typeText(text))
    }

    fun click(resourceId: Int) {
        onView(withId(resourceId)).check(matches(isDisplayed())).perform(click())
    }

    fun swipeScrollView() {
        onView(instanceOf(ScrollView::class.java)).check(matches(isDisplayed())).perform(swipeUp())
    }

    fun swipeSettingsView() {
        onView(instanceOf(RecyclerView::class.java)).check(matches(isDisplayed())).perform(swipeUp())
    }

    fun click(string: String) {
        onView(withText(string)).check(matches(isDisplayed())).perform(click())
    }

    fun clickPopUpMenuItem(string: String) {
        onView(withText(string)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
    }

    fun longClick(resourceId: Int) {
        onView(withId(resourceId)).check(matches(isDisplayed())).perform(longClick())
    }

    fun swipeRight(view: Matcher<View>) {
        onView(view).check(matches(isDisplayed())).perform(swipeRight())
    }

    fun clickClosedNavHamburger() {
        onView(withContentDescription(R.string.navigation_drawer_open)).perform(click())
    }

    fun clickSetting(stringResource: Int) {
        onView(withId(androidx.preference.R.id.recycler_view))
                .perform(actionOnItem<ViewHolder>(hasDescendant(withText(stringResource)), click()))
    }

    fun clickSettingDialog(stringResource: Int, description: String) {
        onView(withId(androidx.preference.R.id.recycler_view))
                .perform(actionOnItem<ViewHolder>(hasDescendant(withText(stringResource)), click()))
        sleep(100)
        getScreenShotCurrentActivity(description)
        Espresso.pressBack()
    }

    fun clickListChildItem(listResource: Int, viewId: Int) {
        onView(withId(listResource))
                .perform(actionOnItemAtPosition<ViewHolder>(0, clickChildViewWithId(viewId)))
    }

    fun swipeListItem(listResource: Int, swipeDirection: String) {
        onData(CoreMatchers.anything())
                .inAdapterView(withId(listResource))
                .atPosition(0)
                .check(matches(isDisplayed()))
                .perform(if (swipeDirection == SWIPE_LEFT_ACTION)
                    GeneralSwipeAction(FAST, CENTER, CENTER_RIGHT, FINGER)
                else
                    GeneralSwipeAction(FAST, CENTER, CENTER_LEFT, FINGER)
                )
    }

    fun closeSwipeListItem(listResource: Int, swipeDirection: String) {
        onData(CoreMatchers.anything())
                .inAdapterView(withId(listResource))
                .atPosition(0)
                .check(matches(isDisplayed()))
                .perform(if (swipeDirection == SWIPE_LEFT_ACTION)
                    GeneralSwipeAction(FAST, CENTER, CENTER_LEFT, FINGER)
                else
                    GeneralSwipeAction(FAST, CENTER, CENTER_RIGHT, FINGER)
                )
    }

    fun expandSetting(stringResource: Int) {
        val string = getString(stringResource)
        onView(withId(androidx.preference.R.id.recycler_view))
                .perform(actionOnItem<ViewHolder>(hasDescendant(withSubstring(string)), click()))
    }

    fun clickListItem(resourceId: Int, position: Int) {
        onData(CoreMatchers.anything())
                .inAdapterView(withId(resourceId))
                .atPosition(position)
                .check(matches(isDisplayed()))
                .perform(click())

    }

    fun viewIsDisplayed(viewId: Int): Boolean {
        val isDisplayed = booleanArrayOf(true)
        onView(withId(viewId))
                .withFailureHandler { error, _ -> isDisplayed[0] = false }
                .check(matches(isDisplayed()))
        return isDisplayed[0]
    }

    fun getString(resourceId: Int): String = getCurrentActivity()?.resources?.getString(resourceId)
            ?: ""

    fun setTestSet(string: String) {
        testSet = string
        count = 0
    }

    fun startFileManagerStub(filename: String, extension: String) {
        val result: Instrumentation.ActivityResult = fileManagerResultStub(filename, extension)
        Intents.intending(IsNot.not(IntentMatchers.isInternal())).respondWith(result)
        runBlocking { waitForIdle() }
    }

    private fun fileManagerResultStub(fileName: String, extension: String): Instrumentation.ActivityResult {
        val resultData = Intent()
        val fileLocation = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName + extension)
        resultData.data = Uri.parse("file://$fileLocation")
        return Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
    }

    companion object {
        private const val BASIC_SAMPLE_PACKAGE = "com.fsck.k9"
        private const val LAUNCH_TIMEOUT = 5000L
        private const val IMAGE_DIR = "/sdcard/Screenshots/"
        const val SWIPE_LEFT_ACTION = "SWIPE_LEFT_ACTION"
        const val SWIPE_RIGHT_ACTION = "SWIPE_RIGHT_ACTION"
        private var count = 0
        private var testSet = "A"
    }

    private val AccountSetupBasics.visibleFragmentSimpleName: String
        get() {
            val fragmentManager: FragmentManager = this.supportFragmentManager
            val fragments = fragmentManager.fragments
            for (fragment in fragments) {
                if (fragment != null && fragment.isVisible) return fragment.javaClass.simpleName
            }
            return ""
        }

    private val MessageList.currentMessageView: String
        get() {
            val fragment = this.messageViewFragment
            return if (fragment != null && fragment.isVisible)
                fragment.javaClass.simpleName
            else "no visible fragment"
        }
}

