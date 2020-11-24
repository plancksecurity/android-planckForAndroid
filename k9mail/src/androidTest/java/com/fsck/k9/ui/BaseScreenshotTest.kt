package com.fsck.k9.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralLocation.*
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press.FINGER
import androidx.test.espresso.action.Swipe.FAST
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.fsck.k9.activity.MessageList
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.common.BaseTest
import com.fsck.k9.common.ChildViewAction.clickChildViewWithId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.hamcrest.core.IsNot
import org.junit.After
import timber.log.Timber
import java.io.File


open class BaseScreenshotTest : BaseTest() {
    @After
    @Throws(Exception::class)
    fun tearDown() {
        // Default is natural orientation (if a testcase fails)
        device.setOrientationNatural()
        device.pressHome()
        Timber.e("Testcase stops here <===========================")
    }

    fun openFirstScreen() {
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

    fun getScreenShotMessageList(action: String) {
        runBlocking { waitForIdle() }
        Timber.e("getScreenShotMessageList $action")
        getInstrumentation().runOnMainSync {
            run {
                val currentActivity: Activity? = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).elementAtOrNull(0)
                if (currentActivity != null && currentActivity is MessageList) {
                    val name = currentActivity::class.java.simpleName + " " + currentActivity.currentMessageView
                    getScreenShot(name, action)
                } else {
                    Timber.e("no currentActivity")
                }
            }
        }
    }

    fun getScreenShotAccountSetup(action: String) {
        runBlocking { waitForIdle() }
        Timber.e("getScreenShotAccountSetup $action")
        getInstrumentation().runOnMainSync {
            run {
                val currentActivity: Activity? = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).elementAtOrNull(0)
                if (currentActivity != null && currentActivity is AccountSetupBasics) {
                    val name = currentActivity::class.java.simpleName + " " + currentActivity.visibleFragmentSimpleName
                    getScreenShot(name, action)
                } else {
                    Timber.e("no currentActivity")
                }
            }
        }
    }

    @Throws(Throwable::class)
    fun getScreenShotCurrentActivity(action: String) {
        runBlocking { waitForIdle() }
        Timber.e("getScreenShotCurrentActivity $action")
        getInstrumentation().runOnMainSync {
            run {
                val currentActivity: Activity? = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).elementAtOrNull(0)
                if (currentActivity != null) {
                    getScreenShot(currentActivity::class.java.simpleName, action)
                } else {
                    Timber.e("no currentActivity")
                }
            }
        }
    }

    fun swipeRight(view: Matcher<View>) {
        onView(view).check(matches(isDisplayed())).perform(swipeRight())
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

