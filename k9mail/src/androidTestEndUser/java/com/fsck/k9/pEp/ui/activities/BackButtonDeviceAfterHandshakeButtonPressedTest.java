package com.fsck.k9.planck.ui.activities;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class BackButtonDeviceAfterHandshakeButtonPressedTest extends BaseAndroidTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private String messageTo;

    @Before
    public void startPlanckApp() {
        messageTo = System.currentTimeMillis() + "@" + HOST;
        testUtils.setupAccountIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void backButtonDeviceAfterHandshakeButtonPressed() {
        sendMessage();
        TestUtils.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        testUtils.clickStatus();
        TestUtils.waitForIdle();
        onView(withId(R.id.confirmHandshake)).perform(click());
        testUtils.pressBack();
    }

    private void sendMessage() {
        TestUtils.waitForIdle();
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_BODY, MESSAGE_SUBJECT, messageTo), false);
        testUtils.sendMessage();
        TestUtils.waitForIdle();
    }
}
