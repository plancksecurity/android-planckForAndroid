package com.fsck.k9.pEp.ui.activities;


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
public class GreyStatusMessageTest {

    private static final String EMAIL = "newemail@mail.es";
    private TestUtils testUtils;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()), InstrumentationRegistry.getInstrumentation());
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
    public void greyStatusEmail() {
        greyStatusEmailTest();
    }

    private void greyStatusEmailTest() {
        prepareMessageCompose();
        testUtils.testStatusMailAndListMail(new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, ""));

        prepareMessageCompose();
        testUtils.testStatusMailAndListMail(new TestUtils.BasicMessage("","","", ""),
                new TestUtils.BasicIdentity(Rating.pEpRatingUndefined, ""));

        prepareMessageCompose();
        testUtils.testStatusMailAndListMail(new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, ""));
    }

    private void prepareMessageCompose() {
        testUtils.composeMessageButton();
        testUtils.testStatusEmpty();
    }
}