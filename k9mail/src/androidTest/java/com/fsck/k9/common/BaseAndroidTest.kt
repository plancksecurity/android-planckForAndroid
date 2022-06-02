package com.fsck.k9.common

import android.content.Intent
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.fsck.k9.pEp.EspressoTestingIdlingResource
import com.fsck.k9.pEp.ui.activities.SplashActivity
import com.fsck.k9.pEp.ui.activities.TestUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class BaseAndroidTest {
    protected lateinit var device: UiDevice
    protected lateinit var testUtils: TestUtils
    @JvmField
    protected val accountListSize = IntArray(2)

    @get:Rule
    var splashActivityTestRule = IntentsTestRule(
        SplashActivity::class.java, false, false)

    @Before
    fun baseBefore() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testUtils = TestUtils(device, InstrumentationRegistry.getInstrumentation())
        EspressoTestingIdlingResource()
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource())
        splashActivityTestRule.launchActivity(Intent())
        testUtils.skipTutorialAndAllowPermissionsIfNeeded()
    }

    @After
    fun baseAfter() {
        splashActivityTestRule.finishActivity()
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource())
    }
}