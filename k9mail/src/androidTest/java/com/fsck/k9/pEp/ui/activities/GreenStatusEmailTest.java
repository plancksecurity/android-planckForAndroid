package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GreenStatusEmailTest  {

    private UiDevice device;
    private TestUtils testUtils;
    private String messageFrom;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(device, instrumentation);
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
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