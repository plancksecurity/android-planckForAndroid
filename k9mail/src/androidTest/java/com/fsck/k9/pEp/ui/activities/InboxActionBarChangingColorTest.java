package com.fsck.k9.pEp.ui.activities;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;
import com.fsck.k9.pEp.ui.tools.ThemeManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;

@RunWith(AndroidJUnit4.class)
public class InboxActionBarChangingColorTest extends BaseAndroidTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private String messageTo;

    @Before
    public void startpEpApp() {
        messageTo = System.currentTimeMillis() + "@" + HOST;
        testUtils.setupAccountIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void assertSelfMessageColor(){
        testUtils.getMessageListSize();
        composeMessageToMyself();
        TestUtils.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.sendMessage();
        TestUtils.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.pressBack();
        assertToolbarColor();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void assertBotMessageColor(){
        testUtils.getMessageListSize();
        composeMessage(messageTo);
        TestUtils.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingUndefined, false);
        testUtils.sendMessage();
        TestUtils.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingReliable, true);
        testUtils.pressBack();
        assertToolbarColor();
    }

    public void assertToolbarColor() {
        testUtils.checkToolbarColor(
                ThemeManager.isDarkTheme()
                        ? R.color.dark_theme_overlay_1
                        : R.color.pep_green
        );
    }

    private void composeMessage(String to) {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, to), false);
    }

    private void composeMessageToMyself() {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
    }
}
