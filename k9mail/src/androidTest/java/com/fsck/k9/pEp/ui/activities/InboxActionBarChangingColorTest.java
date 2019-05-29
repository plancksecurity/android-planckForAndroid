package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;

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
    private EspressoTestingIdlingResource espressoTestingIdlingResource;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(device, instrumentation);
        espressoTestingIdlingResource = new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(espressoTestingIdlingResource.getIdlingResource());
        testUtils.increaseTimeoutWait();
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(espressoTestingIdlingResource.getIdlingResource());
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
        onView(withId(R.id.subject)).perform(typeText(" "));
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.waitForMessageAndClickIt();
        testUtils.clickView(R.id.tvPep);
        testUtils.assertMessageStatus(Rating.pEpRatingTrusted.value);
        device.waitForIdle();
        testUtils.pressBack();
        testUtils.checkToolbarColor(R.color.pep_green);
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.checkToolbarColor(R.color.pep_green);
    }

    private void assertBotMessageColor(){
        testUtils.composeMessageButton();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        onView(withId(R.id.subject)).perform(typeText(" "));
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.waitForMessageAndClickIt();
        testUtils.checkToolbarColor(R.color.pep_yellow);
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.checkToolbarColor(R.color.pep_green);
    }
}
