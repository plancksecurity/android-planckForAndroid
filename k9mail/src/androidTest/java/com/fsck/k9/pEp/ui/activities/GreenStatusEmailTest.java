package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import pEp.jniadapter.Rating;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GreenStatusEmailTest  {

    private UiDevice device;
    private TestUtils testUtils;
    private String messageFrom;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
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

    @Test (timeout = TIMEOUT_TEST)
    public void greenStatusMessage() {
        greenStatusMessageTest();
    }

    private void greenStatusMessageTest() {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount(false);
        testUtils.composeMessageButton();
        testUtils.testStatusEmpty();
        device.waitForIdle();
        messageFrom = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.testStatusMailAndListMail(new TestUtils.BasicMessage(messageFrom, "Subject", "Message", messageFrom) ,
                new TestUtils.BasicIdentity(Rating.pEpRatingTrusted, messageFrom));
        device.waitForIdle();
        testUtils.testStatusMail(new TestUtils.BasicMessage("","","", ""),
                new TestUtils.BasicIdentity(Rating.pEpRatingUndefined, ""));
        device.waitForIdle();
        testUtils.testStatusMailAndListMail(new TestUtils.BasicMessage(messageFrom, "Subject", "Message", messageFrom) ,
                new TestUtils.BasicIdentity(Rating.pEpRatingTrusted, messageFrom));
        testUtils.pressBack();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.save_or_discard_draft_message_dlg_title);
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
        testUtils.goBackAndRemoveAccount();
    }
}