package com.fsck.k9.planck.ui.activities;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;

@RunWith(AndroidJUnit4.class)
public class MessageUnsecureWhenDisableProtectionTest extends BaseAndroidTest {
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    @Before
    public void startActivity() {
        testUtils.setupAccountIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        composeMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.selectFromStatusPopupMenu(R.string.pep_force_unprotected);
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false, false, true);
        TestUtils.waitForIdle();
        testUtils.sendMessage();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        TestUtils.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingUndefined, false);
    }

    private void composeMessage() {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        String messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        TestUtils.waitForIdle();
    }
}
