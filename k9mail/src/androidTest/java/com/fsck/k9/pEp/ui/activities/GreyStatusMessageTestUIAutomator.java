package com.fsck.k9.pEp.ui.activities;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;

@RunWith(AndroidJUnit4.class)
public class GreyStatusMessageTestUIAutomator extends BaseAndroidTest {

    private static final String EMAIL = "newemail@mail.es";

    @Before
    public void startpEpApp() {
        testUtils.setupAccountIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void greyStatusMessageTest() {
        prepareMessageCompose();
        testUtils.testStatusMailAndListMail(
                new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, "")
        );

        prepareMessageCompose();
        testUtils.testStatusMailAndListMail(
                new TestUtils.BasicMessage("","","", EMAIL),
                new TestUtils.BasicIdentity(Rating.pEpRatingUndefined, "")
        );

        prepareMessageCompose();
        testUtils.testStatusMailAndListMail(
                new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, "")
        );
    }

    private void prepareMessageCompose() {
        testUtils.composeMessageButton();
        testUtils.testStatusEmpty();
    }
}
