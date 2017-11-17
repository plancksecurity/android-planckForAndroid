package com.fsck.k9.pEp.ui.activities;


import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.KeyEvent;

import com.fsck.k9.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GreyStatusEmailTest {

    private static final int TIME = 2000;
    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";
    private static final String EMAIL = "newemail@mail.es";
    private TestUtils testUtils;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void greyStatusEmailTest() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        testUtils.increaseTimeoutWait();
        accountConfiguration();
        testUtils.accountDescription(DESCRIPTION, USER_NAME);
        testUtils.accountListSelect(DESCRIPTION);
        testUtils.composseMessageButton();
        testStatusEmpty();
        testStatusMail(EMAIL, "Subject", "Message", Rating.pEpRatingUnencrypted.value);
        testStatusMail("", "", "", Rating.pEpRatingUndefined.value);
        testStatusMail(EMAIL, "", "", Rating.pEpRatingUnencrypted.value);
        testUtils.sendEmail();
        testUtils.removeAccount(DESCRIPTION);
    }

    private void testStatusEmpty(){
        checkStatus(Rating.pEpRatingUndefined.value);
    }

    private void testStatusMail(String to, String subject, String message, int status){
        testUtils.fillEmail(to, subject, message);
        doWait(TIME);
        checkStatus(status);
    }

    private void accountConfiguration(){
        onView(withId(R.id.skip)).perform(click());
        testUtils.newEmailAccount();
        //gmailAccount();
    }


    private void checkStatus(int status){
        onView(withId(R.id.pEp_indicator)).perform(click());
        onView(withId(R.id.pEpTitle)).check(matches(withText(getResourceString(R.array.pep_title, status))));
        Espresso.pressBack();
    }

    private void doWait(int timeMillis){
        try {
        Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getResourceString(int id, int n) {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        return targetContext.getResources().getStringArray(id)[n];
    }
}