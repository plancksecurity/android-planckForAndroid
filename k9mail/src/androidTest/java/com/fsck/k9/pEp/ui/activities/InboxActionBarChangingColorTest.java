package com.fsck.k9.pEp.ui.activities;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;

@RunWith(AndroidJUnit4.class)
public class InboxActionBarChangingColorTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        messageTo = System.currentTimeMillis() + "@" + HOST;
        testUtils.setupAccountIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void assertSelfMessageColor(){
        composeMessageToMyself();
        device.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.pressBack();
        checkToolbarColor(R.color.pep_green);
    }

    @Test
    public void assertBotMessageColor(){
        composeMessage(messageTo);
        device.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingUndefined, false);
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingReliable, true);
        testUtils.pressBack();
        checkToolbarColor(R.color.pep_green);
    }

    private void checkToolbarColor(int color) {
        device.waitForIdle();
        boolean toolbarExists = false;
        while (!toolbarExists) {
            if (exists(onView(withId(R.id.toolbar)))) {
                onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(color)));
                toolbarExists = true;
            }
        }
    }

    private void composeMessage(String to) {
        testUtils.composeMessageButton();
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, to), false);
    }

    private void composeMessageToMyself() {
        testUtils.composeMessageButton();
        device.waitForIdle();
        messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
    }
}
