package com.fsck.k9.pEp.ui.activities;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class GreenStatusEmailTest extends BaseAndroidTest {

    @Before
    public void startpEpApp() {
        testUtils.setupAccountIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST * 2)
    public void greenStatusMessage() {
        greenStatusMessageTest();
    }

    private void greenStatusMessageTest() {
        prepareMessageCompose();
        String messageFrom;
        messageFrom = testUtils.getTextFromTextViewThatContainsText("@");

        testUtils.testStatusMailAndListMail(
                new TestUtils.BasicMessage(messageFrom, "Subject", "Message", messageFrom) ,
                new TestUtils.BasicIdentity(Rating.pEpRatingTrustedAndAnonymized, messageFrom));

        prepareMessageCompose();
        testUtils.testStatusMailAndListMail(new TestUtils.BasicMessage("","","", ""),
                new TestUtils.BasicIdentity(Rating.pEpRatingUndefined, ""));

        prepareMessageCompose();
        messageFrom = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.testStatusMailAndListMail(
                new TestUtils.BasicMessage(messageFrom, "Subject", "Message", messageFrom) ,
                new TestUtils.BasicIdentity(Rating.pEpRatingTrustedAndAnonymized, messageFrom));
    }

    private void prepareMessageCompose() {
        testUtils.composeMessageButton();
        testUtils.testStatusEmpty();
        TestUtils.waitForIdle();
    }
}