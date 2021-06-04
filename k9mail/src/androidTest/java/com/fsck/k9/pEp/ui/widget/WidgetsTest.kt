package com.fsck.k9.pEp.ui.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetsTest {

    lateinit var uiDevice: UiDevice
    lateinit var testUtils: TestUtils

    @get:Rule
    var mActivityRule = ActivityTestRule(SplashActivity::class.java)

    @Before
    fun before() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(uiDevice, InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun openMessageFromWidget() {
        TestUtils.waitForIdle()
        uiDevice.pressBack()
        TestUtils.waitForIdle()
        val list = uiDevice.findObject(By.clazz("android.widget.ListView"))
        list.click()
        TestUtils.waitForIdle()
        uiDevice.pressBack()
        TestUtils.waitForIdle()
        testUtils.openHamburgerMenu()
    }
}