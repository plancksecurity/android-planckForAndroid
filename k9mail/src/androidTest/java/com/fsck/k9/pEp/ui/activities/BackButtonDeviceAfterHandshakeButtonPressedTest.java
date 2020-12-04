package com.fsck.k9.pEp.ui.activities;


import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class BackButtonDeviceAfterHandshakeButtonPressedTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        messageTo = System.currentTimeMillis() + "@" + HOST;
        testUtils.setupAccountIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void backButtonDeviceAfterHandshakeButtonPressed() {
        sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        testUtils.clickStatus();
        device.waitForIdle();
        onView(withId(R.id.confirmHandshake)).perform(click());
        testUtils.pressBack();
    }

    private void sendMessage() {
        device.waitForIdle();
        testUtils.composeMessageButton();
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_BODY, MESSAGE_SUBJECT, messageTo), false);
        testUtils.sendMessage();
        device.waitForIdle();
    }
}
