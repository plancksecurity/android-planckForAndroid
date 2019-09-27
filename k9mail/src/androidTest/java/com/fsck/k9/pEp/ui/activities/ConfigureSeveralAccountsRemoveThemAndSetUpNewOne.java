package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


@RunWith(AndroidJUnit4.class)
public class ConfigureSeveralAccountsRemoveThemAndSetUpNewOne {

    private UiDevice device;
    private TestUtils testUtils;
    private Instrumentation instrumentation;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(device, instrumentation);
        espressoTestingIdlingResource = new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(espressoTestingIdlingResource.getIdlingResource());
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(espressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void ConfigureAccountRemoveThemSetUpNewAccount() {
        int total = 14;
        for (int account = 0; account < total; account++) {
            testUtils.createAccount(false);
            testUtils.goBackAndRemoveAccount();
            device.waitForIdle();
        }
    }

}
