package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;

@RunWith(AndroidJUnit4.class)
public class InboxActionBarChangingColorTest {

    private static final String HOST = "test.pep-security.net";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = "random@test.pep-security.net";

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        testUtils = new TestUtils(device, instrumentation);
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.increaseTimeoutWait();
        messageTo = System.currentTimeMillis() + "@" + HOST;
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test (timeout = TIMEOUT_TEST)
    public void assertActionBarColorIsNotChanging() {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount();
        assertSelfMessageColor();
        assertBotMessageColor();
        testUtils.goBackAndRemoveAccount();
    }

    private void assertSelfMessageColor(){
        device.waitForIdle();
        testUtils.composeMessageButton();
        String selfMessage = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, selfMessage), false);
        onView(withId(R.id.subject)).perform(typeText(" "));
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        //testUtils.clickLastMessageReceived();
        testUtils.clickView(R.id.tvPep);
        testUtils.assertMessageStatus(Rating.pEpRatingTrusted.value);
        device.waitForIdle();
        testUtils.pressBack();
        checkToolbarColor(R.color.pep_green);
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        checkToolbarColor(R.color.pep_green);
    }

    private void checkToolbarColor(int color) {
        boolean toolbarExists = false;
        while (!toolbarExists) {
            if (exists(onView(withId(R.id.toolbar)))) {
                onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(color)));
                toolbarExists = true;
            }
        }
    }

    private void assertBotMessageColor(){
        testUtils.composeMessageButton();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        onView(withId(R.id.subject)).perform(typeText(" "));
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        //testUtils.clickLastMessageReceived();
        checkToolbarColor(R.color.pep_yellow);
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        checkToolbarColor(R.color.pep_green);
    }
}
