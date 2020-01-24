package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
public class RemoveContactsFromMessageAndKeepOriginalToolbarColor {

    private static final String APP_ID = BuildConfig.APPLICATION_ID;

    private TestUtils testUtils;
    private UiDevice device;
    private String messageFrom;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils = new TestUtils(device, instrumentation);
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test (timeout = TIMEOUT_TEST)
    public void assertRemoveTwoDifferentColorContactsAndKeepOriginalToolbarColor() {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount();
        messageFrom = testUtils.getTextFromTextViewThatContainsText("@");
        assertToolBarHasNoColorWhenUnkownReceiver(messageFrom);
        assertToolBarHasNoColorWhenUnkownReceiver("random@test.pep-security.net");
        testUtils.goBackAndRemoveAccount();
    }

    private void fillMessageWithOneKnownReceiverAndOneUnknown(String to, String subject, String message){
        testUtils.doWait("to");
        device.waitForIdle();
        device.findObject(By.res(APP_ID, "to")).longClick();
        device.waitForIdle();
        onView(withId(R.id.to)).perform(typeText(to), closeSoftKeyboard());
        device.findObject(By.res(APP_ID, "subject")).click();
        device.findObject(By.res(APP_ID, "subject")).setText(subject);
        onView(withId(R.id.to)).perform(typeText("unkown@user.is"), closeSoftKeyboard());
        device.findObject(By.res(APP_ID, "message_content")).click();
        device.findObject(By.res(APP_ID, "message_content")).setText(message);
        Espresso.closeSoftKeyboard();
    }

    private void assertToolBarHasNoColorWhenUnkownReceiver(String messageTo){
        testUtils.composeMessageButton();
        device.waitForIdle();
        fillMessageWithOneKnownReceiverAndOneUnknown(messageTo, "Subject", "Message");
        device.waitForIdle();
        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColor(R.color.pep_no_color)));
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        onView(withText(R.string.discard_action)).perform(click());
        onView(allOf(withId(R.id.toolbar))).check(matches(withBackgroundColor(R.color.pep_green)));
    }
}