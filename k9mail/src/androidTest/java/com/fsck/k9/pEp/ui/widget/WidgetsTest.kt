package com.fsck.k9.pEp.ui.widget

import android.graphics.Point
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val WIDGETS: String = "WIDGETS"
const val TIMEOUT: Long = 5000L

@RunWith(AndroidJUnit4::class)
class WidgetsTest {

    private lateinit var uiDevice: UiDevice
    private lateinit var testUtils: TestUtils
    private lateinit var widgetName: String

    @get:Rule
    var mActivityRule = ActivityTestRule(SplashActivity::class.java)

    @Before
    fun setWidgetOnHome() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(uiDevice, InstrumentationRegistry.getInstrumentation())
        widgetName = mActivityRule.activity.getString(R.string.mail_list_widget_text)

        addMessageListWidget()
    }

    private fun addMessageListWidget() {
        val screenSize = Point(uiDevice.displayWidth, uiDevice.displayHeight)
        val screenCenter = Point(screenSize.x / 2, screenSize.y / 2)
        val showWidgets = Point(825, 1500)

        uiDevice.pressHome()

        uiDevice.wait(Until.hasObject(By.pkg(uiDevice.launcherPackageName).depth(0)), TIMEOUT)

        uiDevice.swipe(arrayOf(showWidgets, showWidgets), 150)

        findWidget(WIDGETS)?.click()

        val dimen = screenSize.y / 2

        var widget = findWidget(widgetName)
        while (widget == null) {
            uiDevice.swipe(screenCenter.x, dimen, screenCenter.x, 0, 150)
            widget = findWidget(widgetName)
        }
        val b = widget.visibleBounds
        val c = Point(b.left , b.bottom )
        val dest = Point(c.x , c.y )
        uiDevice.swipe(arrayOf(c, c, dest), 150)
    }

    private fun findWidget(withName: String): UiObject2? {
        return uiDevice.findObject(By.text(withName))
    }

    @Test
    fun openMessageFromWidget() {
        TestUtils.waitForIdle()
        uiDevice.pressBack()
        TestUtils.waitForIdle()
        val list = uiDevice.findObject(By.clazz("android.widget.ListView"))
        list.click()
        TestUtils.waitForIdle()
    }
}