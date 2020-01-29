package com.fsck.k9.pEp.ui.activities;



import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


import foundation.pEp.jniadapter.Rating;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;

@LargeTest
//@RunWith(AndroidJUnit4.class)
public class GreyStatusMessageTest {

    private static final String EMAIL = "newemail@mail.es";
    private TestUtils testUtils;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        testUtils = new TestUtils(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()), InstrumentationRegistry.getInstrumentation());
        espressoTestingIdlingResource = new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test(timeout = TIMEOUT_TEST)
    public void greyStatusEmail() {
        greyStatusEmailTest(false);
    }

    private void greyStatusEmailTest(boolean isGmail) {
        testUtils.increaseTimeoutWait();
        testUtils.createAccount();
        testUtils.composeMessageButton();
        testUtils.testStatusEmpty();
        testUtils.testStatusMail(new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, ""));
        testUtils.testStatusMail(new TestUtils.BasicMessage("", "", "", ""),
                new TestUtils.BasicIdentity(Rating.pEpRatingUndefined, ""));
        testUtils.testStatusMail(new TestUtils.BasicMessage("", "Subject", "Message", EMAIL),
                new TestUtils.BasicIdentity(Rating.pEpRatingUnencrypted, ""));
        testUtils.pressBack();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.save_or_discard_draft_message_dlg_title);
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
        testUtils.goBackAndRemoveAccount();
    }
}