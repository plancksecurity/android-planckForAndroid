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
import org.pEp.jniadapter.Rating;

import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;

@RunWith(AndroidJUnit4.class)
public class SendMessageIsBlockedAfterChekingpEpStatus {
    private UiDevice uiDevice;
    private TestUtils testUtils;
    private String messageTo = "";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private Instrumentation instrumentation;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(uiDevice, instrumentation);
        testUtils.increaseTimeoutWait();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test (timeout = TIMEOUT_TEST)
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        testUtils.createAccount(false);
        composeSelfMessage();
        testUtils.checkStatus(Rating.pEpRatingTrusted);
        testUtils.pressBack();
        disableProtection();
        onView(withId(R.id.subject)).perform(typeText(" "));
        testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        testUtils.pressBack();
        testUtils.sendMessage();
        uiDevice.waitForIdle();
        testUtils.waitForNewMessage();
        composeSelfMessage();
        testUtils.sendMessage();
        uiDevice.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.doWaitForResource(R.id.actionbar_title_first);
        onView(withId(R.id.actionbar_title_first)).check(matches(isDisplayed()));
        uiDevice.waitForIdle();
        testUtils.goBackAndRemoveAccount();
    }

    private void composeSelfMessage(){
        testUtils.composeMessageButton();
        uiDevice.waitForIdle();
        messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        uiDevice.waitForIdle();
    }

    private void disableProtection(){
        testUtils.openOptionsMenu();
        testUtils.selectFromScreen(R.string.pep_force_unprotected);
        instrumentation.waitForIdleSync();
        boolean toolbarClosed = false;
        while (!toolbarClosed){
            try{
                onView(withId(R.id.message_content)).perform(typeText(""));
                toolbarClosed = true;
            } catch (Exception ex){
                Timber.i("Toolbar is not closed yet");
            }
        }
        onView(withId(R.id.subject)).perform(click());
    }
}