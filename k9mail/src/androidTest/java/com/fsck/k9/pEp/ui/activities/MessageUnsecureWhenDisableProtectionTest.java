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
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MessageUnsecureWhenDisableProtectionTest {
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
        launchTest();
        composeMessage();
        testUtils.checkStatus(Rating.pEpRatingTrusted);
        testUtils.pressBack();
        disableProtection();
        testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.sendMessage();
        testUtils.waitForNewMessage();
        testUtils.clickLastMessageReceived();
        uiDevice.waitForIdle();
        checkStatus();
        removeMessageListAndAccount();

    }

    private void launchTest(){
        testUtils.createAccount(false);
        testUtils.waitForNewMessage();
    }

    private void composeMessage(){
        testUtils.composeMessageButton();
        uiDevice.waitForIdle();
        messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        uiDevice.waitForIdle();
    }

    private void disableProtection(){
        testUtils.openOptionsMenu();
        testUtils.selectFromMenu(R.string.pep_force_unprotected);
        uiDevice.waitForIdle();
    }

    private void checkStatus(){
        onView(withId(R.id.tvPep)).perform(click());
        onView(withId(R.id.pEpTitle)).check(matches(withText(testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingUnencrypted.value))));
        uiDevice.waitForIdle();
    }

    private void removeMessageListAndAccount(){
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.pressBack();
        testUtils.removeMessagesFromList();
        testUtils.pressBack();
        testUtils.removeLastAccount();
    }
}
