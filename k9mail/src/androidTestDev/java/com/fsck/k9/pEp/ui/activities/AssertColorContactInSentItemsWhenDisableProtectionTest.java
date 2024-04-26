package com.fsck.k9.planck.ui.activities;

import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;

@RunWith(AndroidJUnit4.class)
public class AssertColorContactInSentItemsWhenDisableProtectionTest extends BaseAndroidTest {
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private Resources resources;

    @Before
    public void startActivity() {
        resources = ApplicationProvider.getApplicationContext().getResources();
        testUtils.setupAccountIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST * 2)
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        testUtils.getMessageListSize();
        composeMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.selectFromStatusPopupMenu(R.string.pep_force_unprotected);
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false, false, true);
        TestUtils.waitForIdle();
        testUtils.sendMessage();
        testUtils.waitForNewMessage();
        clickFirstMessageFromSentFolder();
        testUtils.assertMessageStatus(Rating.pEpRatingUndefined, false);
    }

    private void composeMessage() {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        String messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
    }

    private void clickFirstMessageFromSentFolder() {
        testUtils.goToSentFolder();
        testUtils.clickFirstMessage();
    }
}

