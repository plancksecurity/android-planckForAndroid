package com.fsck.k9.planck.ui.activities;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.fsck.k9.planck.ui.activities.TestUtils.TIMEOUT_TEST;

@RunWith(AndroidJUnit4.class)
public class AttachFilesToEmailTest extends BaseAndroidTest {
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    @Before
    public void startpEpApp() {
        testUtils.setupAccountIfNeeded();
    }

    @Test (timeout = TIMEOUT_TEST)
    public void attachFilesToEmail() {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        fillComposeFields(testUtils.getTextFromTextViewThatContainsText("@"));
    }

    private void fillComposeFields(String messageTo) {
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT,  MESSAGE_BODY, messageTo), true);
        testUtils.sendMessage();
    }

}
