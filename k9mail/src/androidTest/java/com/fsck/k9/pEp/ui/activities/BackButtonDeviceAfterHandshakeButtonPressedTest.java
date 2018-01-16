package com.fsck.k9.pEp.ui.activities;


import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class BackButtonDeviceAfterHandshakeButtonPressedTest {

    private static final String HOST = "test.pep-security.net";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        testUtils.startActivity();
    }

    @Test
    public void backButtonDeviceAfterHandshakeButtonPressed() {
        testUtils.createAccount(false);
        sendMessages(3);
        device.waitForIdle();
        testUtils.clickLastMessageReceived();
        testUtils.assertMessageStatus(Rating.pEpRatingReliable.value);
        device.waitForIdle();
        onView(withId(R.id.handshake_button_text)).perform(click());
        device.waitForIdle();
        onView(withId(R.id.confirmTrustWords)).perform(click());
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.removeLastAccount();
    }

    public void sendMessages(int totalMessages) {
        device.waitForIdle();
        for (int message = 0; message < totalMessages; message++) {
            testUtils.getLastMessageReceived();
            testUtils.composeMessageButton();
            device.waitForIdle();
            testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_BODY, MESSAGE_SUBJECT, messageTo), false);
            testUtils.sendMessage();
            device.waitForIdle();
            testUtils.waitForMessageWithText("p≡p", "p≡pbot (" + messageTo + ")");
        }
    }
}
