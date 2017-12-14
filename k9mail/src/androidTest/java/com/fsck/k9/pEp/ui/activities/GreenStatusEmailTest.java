package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GreenStatusEmailTest  {

    private TestUtils testUtils;
    private String emailFrom;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startMainActivity() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        testUtils.startActivity();
    }

    @Test
    public void greenStatusEmail() {
        greenStatusEmailTest();
    }

    private void greenStatusEmailTest() {
        testUtils.increaseTimeoutWait();
        testUtils.composeMessageButton();
        testUtils.testStatusEmpty();
        testUtils.doWait();
        emailFrom = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.testStatusMailAndListMail(emailFrom, "Subject", "Message", Rating.pEpRatingTrusted.value, emailFrom);
        testUtils.doWait();
        testUtils.testStatusMail("", "", "", Rating.pEpRatingUndefined.value);
        testUtils.doWait();
        testUtils.testStatusMailAndListMail(emailFrom, "Subject", "Message", Rating.pEpRatingTrusted.value, emailFrom);
    }
}