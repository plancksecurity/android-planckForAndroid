package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

@RunWith(AndroidJUnit4.class)
public class SendMessageIsBlockedAfterChekingpEpStatus {
    private UiDevice uiDevice;
    private TestUtils testUtils;
    private String messageTo = "";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(uiDevice);
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @Test
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        //testUtils.createAccount(false);
        testUtils.getLastMessageReceived();
        testUtils.composeMessageButton();
        uiDevice.waitForIdle();
        messageTo = "random@email.is";
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        uiDevice.waitForIdle();
        testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.sendMessage();
        testUtils.getLastMessageReceived();
        testUtils.composeMessageButton();
        uiDevice.waitForIdle();
        messageTo = "random@email.is";
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        uiDevice.waitForIdle();
        testUtils.sendMessage();

        uiDevice.waitForIdle();
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.pressBack();
        //testUtils.removeMessagesFromList();
    }
}