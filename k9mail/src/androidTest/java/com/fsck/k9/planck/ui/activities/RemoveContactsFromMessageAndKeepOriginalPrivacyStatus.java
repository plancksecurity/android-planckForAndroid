package com.fsck.k9.planck.ui.activities;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.KeyEvent;


@RunWith(AndroidJUnit4.class)
public class RemoveContactsFromMessageAndKeepOriginalPrivacyStatus extends BaseAndroidTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private static final String UNKNOWN_ADDRESS = "unkown@user.is";

    @Before
    public void startpEpApp() {
        testUtils.setupAccountIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void assertRemoveTwoDifferentColorContactsAndKeepOriginalPrivacyStatus() {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        String messageFrom = testUtils.getTextFromTextViewThatContainsText("@");

        assertStatusWhenUnkownRecipient(messageFrom, false);
        assertStatusGoesBackToNormalOnRemovingRecipient(Rating.pEpRatingTrustedAndAnonymized, false);

        String botRecipient = System.currentTimeMillis() + "@" + HOST;
        testUtils.getMessageListSize();
        sendMessage(botRecipient);
        testUtils.waitForNewMessage();
        TestUtils.waitForIdle();


        testUtils.composeMessageButton();
        TestUtils.waitForIdle();

        assertStatusWhenUnkownRecipient(botRecipient, false);
        assertStatusGoesBackToNormalOnRemovingRecipient(Rating.pEpRatingReliable, true);
    }

    private void fillMessageWithOneKnownReceiverAndOneUnknown(String to, String subject, String message){
        testUtils.doWait("to");
        TestUtils.waitForIdle();
        onView(withId(R.id.subject)).perform(replaceText(subject));
        TestUtils.waitForIdle();
        onView(withId(R.id.message_content)).perform(typeText(message));
        TestUtils.waitForIdle();
        onView(withId(R.id.to)).perform(typeText(to +  "\n" + UNKNOWN_ADDRESS + "\n"), closeSoftKeyboard());
    }

    private void assertStatusWhenUnkownRecipient(String messageTo, boolean clickableExpected){
        fillMessageWithOneKnownReceiverAndOneUnknown(messageTo, "Subject", "Message");
        TestUtils.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingUndefined, clickableExpected);
        TestUtils.waitForIdle();
    }

    private void assertStatusGoesBackToNormalOnRemovingRecipient(Rating originalStatus, boolean clickableExpected) {
        deleteLastPartOfEmail();
        TestUtils.waitForIdle();
        testUtils.assertMessageStatus(originalStatus, clickableExpected);
        testUtils.goBackFromMessageCompose(false);
    }

    private void deleteLastPartOfEmail() {
        for (int i = 0; i <= UNKNOWN_ADDRESS.length(); i++) {
            TestUtils.waitForIdle();
            device.pressKeyCode(KeyEvent.KEYCODE_DEL);
            TestUtils.waitForIdle();
        }
    }

    private void sendMessage(String messageTo) {
        TestUtils.waitForIdle();
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        testUtils.sendMessage();
        TestUtils.waitForIdle();
    }
}