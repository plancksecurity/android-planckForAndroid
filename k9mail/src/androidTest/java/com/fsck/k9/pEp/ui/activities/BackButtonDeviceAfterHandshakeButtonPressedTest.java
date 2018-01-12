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

    private UiDevice uiDevice;
    private TestUtils testUtils;
    private String messageTo;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(uiDevice);
        testUtils.increaseTimeoutWait();
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        testUtils.startActivity();
    }

    @Test
    public void backButtonDeviceAfterHandshakeButtonPressed() {
        sendMessages();
        uiDevice.waitForIdle();
        testUtils.clickLastMessageReceived();
        testUtils.assertMessageStatus(Rating.pEpRatingReliable.value);
        uiDevice.waitForIdle();
        onView(withId(R.id.handshake_button_text)).perform(click());
        uiDevice.waitForIdle();
        onView(withId(R.id.confirmTrustWords)).perform(click());
        testUtils.pressBack();
        testUtils.pressBack();
    }

    public void sendMessages() {
        uiDevice.waitForIdle();
        for (int messages = 0; messages < 3; messages++) {
            testUtils.getLastMessageReceived();
            testUtils.composeMessageButton();
            uiDevice.waitForIdle();
            testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_BODY, MESSAGE_SUBJECT, messageTo), false);
            testUtils.sendMessage();
            uiDevice.waitForIdle();
            testUtils.waitForMessageWithText("p≡p", "p≡pbot (" + messageTo + ")");
        }
    }
}
