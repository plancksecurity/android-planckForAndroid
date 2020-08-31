package com.fsck.k9.ui

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.os.Build
import android.util.Log
import android.view.View
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.io.File


open class BaseScreenshotTest {
    lateinit var device: UiDevice

    /**
     * Wake up device and remove screenlock if needed
     * Set portrait orientation
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        Log.e("Test", "New testcase starts here ======================>")
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
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
            Timber.e( "Launch failed ================>")
            Timber.e(e)
        }
        Timber.e("Test", "Launch successful ================>")
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
        val context = InstrumentationRegistry.getInstrumentation().context
        val intent = context.packageManager.getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)
        // Clear out any previous instances
        context.startActivity(intent)
    }

    private fun clearAppData() {
        try {
            // clearing app data
            if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                (InstrumentationRegistry.getInstrumentation().context.getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
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
        Timber.e( "Testcase stops here <===========================")
    }

    @Test
    fun test() {

    }

    fun getScreenShot(className: String, action: String) {
        sleep(500) // Wait for screen to change
        val imageDir = File(IMAGE_DIR)
        if (!imageDir.exists()) imageDir.mkdir()
        device.takeScreenshot(File("$IMAGE_DIR${cnt++}_${className}_${action}.png"), 0.5f, 25)
        Timber.e( "Screenshot #" + (cnt - 1))
    }

    fun sleep(time: Int) {
        try {
            Thread.sleep(time.toLong())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Throwable::class)
    fun getScreenShotCurrentActivity(action: String) = runBlocking(Dispatchers.Main) {
        waitForIdle()
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        Iterables.getOnlyElement(activities)?.let { currentActivity ->
            getScreenShot(currentActivity::class.java.simpleName, action)
        }
    }

    suspend fun waitForIdle() = withContext(Dispatchers.IO) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Throws(Throwable::class)
    fun getCurrentActivity(): Activity? = runBlocking(Dispatchers.Main) {
        waitForIdle()
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        return@runBlocking Iterables.getOnlyElement(activities)
    }

    fun addTextTo(resourceId: Int, text: String) {
        Espresso.onView(ViewMatchers.withId(resourceId)).perform(ViewActions.typeText(text))
    }

    fun click(resourceId: Int) {
        Espresso.onView(ViewMatchers.withId(resourceId)).perform(click())
    }

    fun swipeRight(view: Matcher<View>) {
        Espresso.onView(view).perform(ViewActions.swipeRight())
    }

    companion object {
        private const val BASIC_SAMPLE_PACKAGE = "com.fsck.k9"
        private const val LAUNCH_TIMEOUT = 5000L
        private const val IMAGE_DIR = "/sdcard/Screenshots/"
        private var cnt = 0
    }
}