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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;

@RunWith(AndroidJUnit4.class)
public class InboxActionBarChangingColorTest {

    private static final String HOST = "test.pep-security.net";
    private static final String EMAIL = "juan@miau.xyz";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = "random@test.pep-security.net";

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        testUtils.startActivity();
    }

    @Test
    public void assertActionBarColorIsNotChanging() {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount(false);
        assertSelfMessageColor();
        assertBotMessageColor();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.removeLastAccount();
    }

    private void assertSelfMessageColor(){
        device.waitForIdle();
        testUtils.getLastMessageReceived();
        testUtils.composeMessageButton();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, EMAIL), false);
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForMessageWithText(MESSAGE_BODY, MESSAGE_SUBJECT);
        testUtils.clickLastMessageReceived();
        testUtils.assertMessageStatus(Rating.pEpRatingTrusted.value);
        device.waitForIdle();
        testUtils.pressBack();
        onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(R.color.pep_green)));
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(R.color.pep_green)));
    }

    private void assertBotMessageColor(){
        device.waitForIdle();
        testUtils.getLastMessageReceived();
        testUtils.composeMessageButton();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForMessageWithText("p≡p", "p≡pbot (" + messageTo + ")");
        testUtils.clickLastMessageReceived();
        testUtils.assertMessageStatus(Rating.pEpRatingReliable.value);
        device.waitForIdle();
        testUtils.pressBack();
        onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(R.color.pep_yellow)));
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(R.color.pep_green)));
    }
}
