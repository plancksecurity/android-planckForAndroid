package com.fsck.k9.pEp.ui.activities;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.getTextFromView;

@RunWith(AndroidJUnit4.class)
public class AttachFilesToEmailTest {

    private TestUtils testUtils;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        testUtils.setupAccountIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void attachFilesToEmail() {
        attachFilesToAccount();
    }

    private void attachFilesToAccount() {
        testUtils.composeMessageButton();
        String messageTo = getTextFromView(onView(withId(R.id.accountName)));
        testUtils.fillMessage(new TestUtils.BasicMessage("", "Subject", "Message", messageTo), true);
        testUtils.sendMessage();
    }

}
