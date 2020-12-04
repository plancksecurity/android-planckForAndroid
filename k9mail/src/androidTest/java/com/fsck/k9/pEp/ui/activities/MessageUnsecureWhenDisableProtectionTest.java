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

import foundation.pEp.jniadapter.Rating;

@RunWith(AndroidJUnit4.class)
public class MessageUnsecureWhenDisableProtectionTest {
    private UiDevice uiDevice;
    private TestUtils testUtils;
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(uiDevice, InstrumentationRegistry.getInstrumentation());
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.setupAccountIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }


    @Test
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        composeMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.selectFromStatusPopupMenu(R.string.pep_force_unprotected);
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false, false);
        uiDevice.waitForIdle();
        testUtils.sendMessage();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        uiDevice.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingUndefined, false);
    }

    private void composeMessage() {
        testUtils.composeMessageButton();
        uiDevice.waitForIdle();
        String messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        uiDevice.waitForIdle();
    }
}
