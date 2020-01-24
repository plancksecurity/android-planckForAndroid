package com.fsck.k9.pEp.ui.activities;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class ConfigureSeveralAccountsRemoveThemAndSetUpNewOne {

    private UiDevice device;
    private TestUtils testUtils;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void ConfigureAccountRemoveThemSetUpNewAccount() {
        int total = 14;
        for (int account = 0; account < total; account++) {
            testUtils.createAccount();
            testUtils.goBackAndRemoveAccount();
            device.waitForIdle();
        }
    }

}
