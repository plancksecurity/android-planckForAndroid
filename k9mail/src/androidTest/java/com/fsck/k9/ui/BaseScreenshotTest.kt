package com.fsck.k9.ui

import android.app.Activity
import android.util.Log
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
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
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

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
            // Wait for launcher
            val launcherPackage = device.launcherPackageName
            Assert.assertThat(launcherPackage, CoreMatchers.notNullValue())
            device.wait(
                    Until.hasObject(By.pkg(launcherPackage).depth(0)),
                    LAUNCH_TIMEOUT.toLong()
            )

            // Launch the app
            val context = InstrumentationRegistry.getInstrumentation().context
            val intent = context.packageManager.getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)
            // Clear out any previous instances
            context.startActivity(intent)

            // Wait for the app to appear
            device.wait(
                    Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)),
                    LAUNCH_TIMEOUT.toLong()
            )
        } catch (e: Exception) {
            Log.e("Test", "Launch failed ================>")
            e.printStackTrace()
        }
        Log.e("Test", "Launch successful ================>")
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        // Default is natural orientation (if a testcase fails)
        device.setOrientationNatural()
        device.pressHome()
        Log.e("Test", "Testcase stops here <===========================")
    }

    @Test
    fun test() {

    }

    fun getScreenShot(className: String, action: String) {
        sleep(500) // Wait for screen to change
        val imageDir = File(IMAGE_DIR)
        if (!imageDir.exists()) imageDir.mkdir()
        device.takeScreenshot(File("$IMAGE_DIR${className}_${action}_${cnt++}.png"), 0.5f, 25)
        Log.e("Test", "Screenshot #" + (cnt - 1))
    }

    private fun sleep(time: Int) {
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

    suspend fun waitForIdle() = withContext(Dispatchers.IO){
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Throws(Throwable::class)
    fun getCurrentActivity(): Activity? = runBlocking(Dispatchers.Main) {
        waitForIdle()
        val activities: Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
        return@runBlocking Iterables.getOnlyElement(activities)
    }


    companion object {
        private const val BASIC_SAMPLE_PACKAGE = "com.yourown.app"
        private const val LAUNCH_TIMEOUT = 5000
        private const val IMAGE_DIR = "/sdcard/Screenshots/"
        private var cnt = 0
    }
}