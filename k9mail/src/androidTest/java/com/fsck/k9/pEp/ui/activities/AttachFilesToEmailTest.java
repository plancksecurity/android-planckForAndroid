package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;

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
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.getTextFromView;

@RunWith(AndroidJUnit4.class)
public class AttachFilesToEmailTest {

    private TestUtils testUtils;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils = new TestUtils(device, instrumentation);
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void attachFilesToEmail() {
        attachFilesToAccount(false);
    }

    private void attachFilesToAccount(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount();
        testUtils.composeMessageButton();
        String messageTo = getTextFromView(onView(withId(R.id.identity)));
        testUtils.fillMessage(new TestUtils.BasicMessage("", "Subject", "Message", messageTo), true);
        testUtils.sendMessage();
        testUtils.goBackAndRemoveAccount();
    }

}
