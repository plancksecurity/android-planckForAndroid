package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class GreyStatusMessageTestUIAutomator {

    private static final String EMAIL = "newemail@mail.es";

    private TestUtils testUtils;

    @Before
    public void startMainActivity() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()));
        testUtils.startActivity();
    }

    @Test
    public void greyStatusMessage(){
        greyStatusMessageTest(false);
    }

    public void greyStatusMessageTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount(isGmail);
        testUtils.composeMessageButton();
        testUtils.testStatusEmpty();
        testUtils.testStatusMail(new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                 new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, ""));
        testUtils.testStatusMail(new TestUtils.BasicMessage("", "", "", ""),
                 new TestUtils.BasicIdentity(Rating.pEpRatingUndefined, ""));
        testUtils.testStatusMail(new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, ""));
    }
}
