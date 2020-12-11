package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class GreenStatusEmailTest  {

    private UiDevice device;
    private TestUtils testUtils;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(device, instrumentation);
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.setupAccountIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test
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
        device.waitForIdle();
    }
}