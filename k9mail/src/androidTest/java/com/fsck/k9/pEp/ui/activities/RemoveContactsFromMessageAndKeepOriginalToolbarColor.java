package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.YellowStatusEmailFromBotTest.withBackgroundColor;
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
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.startActivity();
    }

    @Test
    public void assertRemoveTwoDifferentColorContactsAndKeepOriginalToolbarColor() {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount(false);
        messageFrom = testUtils.getTextFromTextViewThatContainsText("@");
        assertToolBarHasNoColorWhenUnkownReceiver(messageFrom);
        assertToolBarHasNoColorWhenUnkownReceiver("random@test.pep-security.net");
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.removeLastAccount();
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