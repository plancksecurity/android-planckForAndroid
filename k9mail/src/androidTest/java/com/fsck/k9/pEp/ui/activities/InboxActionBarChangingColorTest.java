package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;
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
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;

@RunWith(AndroidJUnit4.class)
public class InboxActionBarChangingColorTest {

    private static final String HOST = "test.pep-security.net";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private String selfMessage = "";

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = "random@test.pep-security.net";
    private Instrumentation instrumentation;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(device, instrumentation);
        testUtils.increaseTimeoutWait();
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        testUtils.startActivity();
    }

    @Test (timeout = TIMEOUT_TEST)
    public void assertActionBarColorIsNotChanging() {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount(false);
        assertSelfMessageColor();
        assertBotMessageColor();
        testUtils.goBackAndRemoveAccount();
    }

    private void assertSelfMessageColor(){
        device.waitForIdle();
        testUtils.composeMessageButton();
        selfMessage = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, selfMessage), false);
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickLastMessageReceived();
        testUtils.clickView(R.id.tvPep);
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
        testUtils.composeMessageButton();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickLastMessageReceived();
        onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(R.color.pep_yellow)));
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(R.color.pep_green)));
    }
}
