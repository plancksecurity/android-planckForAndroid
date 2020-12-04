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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


@RunWith(AndroidJUnit4.class)
public class RemoveContactsFromMessageAndKeepOriginalPrivacyStatus {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private static final String UNKNOWN_ADDRESS = "unkown@user.is";

    private TestUtils testUtils;
    private UiDevice device;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
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
    public void assertRemoveTwoDifferentColorContactsAndKeepOriginalPrivacyStatus() {
        testUtils.composeMessageButton();
        device.waitForIdle();
        String messageFrom = testUtils.getTextFromTextViewThatContainsText("@");

        assertStatusWhenUnkownRecipient(messageFrom, false);
        assertStatusGoesBackToNormalOnRemovingRecipient(Rating.pEpRatingTrustedAndAnonymized, false);

        String botRecipient = System.currentTimeMillis() + "@" + HOST;
        sendMessage(botRecipient);
        testUtils.waitForNewMessage();
        device.waitForIdle();


        testUtils.composeMessageButton();
        device.waitForIdle();

        assertStatusWhenUnkownRecipient(botRecipient, false);
        assertStatusGoesBackToNormalOnRemovingRecipient(Rating.pEpRatingReliable, true);
    }

    private void fillMessageWithOneKnownReceiverAndOneUnknown(String to, String subject, String message){
        testUtils.doWait("to");
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(replaceText(subject));
        device.waitForIdle();
        onView(withId(R.id.message_content)).perform(typeText(message));
        device.waitForIdle();
        onView(withId(R.id.to)).perform(typeText(to +  "\n" + UNKNOWN_ADDRESS + "\n"), closeSoftKeyboard());
    }

    private void assertStatusWhenUnkownRecipient(String messageTo, boolean clickableExpected){
        fillMessageWithOneKnownReceiverAndOneUnknown(messageTo, "Subject", "Message");
        device.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingUndefined, clickableExpected);
        device.waitForIdle();
    }

    private void assertStatusGoesBackToNormalOnRemovingRecipient(Rating originalStatus, boolean clickableExpected) {
        testUtils.removeTextFromTextView(R.id.to, UNKNOWN_ADDRESS + "\n");
        device.waitForIdle();
        testUtils.assertMessageStatus(originalStatus, clickableExpected);
        testUtils.goBackFromMessageCompose(false);
    }

    private void sendMessage(String messageTo) {
        testUtils.composeMessageButton();
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        testUtils.sendMessage();
        device.waitForIdle();
    }
}