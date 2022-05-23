package com.fsck.k9.pEp.ui.activities;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;
import com.fsck.k9.pEp.ui.tools.ThemeManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;

@RunWith(AndroidJUnit4.class)
public class InboxActionBarChangingColorTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private TestUtils testUtils;
    private String messageTo;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
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

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void assertSelfMessageColor(){
        testUtils.getMessageListSize();
        composeMessageToMyself();
        TestUtils.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.sendMessage();
        TestUtils.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.checkToolbarColor(R.color.pep_green);
        testUtils.pressBack();
        assertToolbarColor();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void assertBotMessageColor(){
        testUtils.getMessageListSize();
        composeMessage(messageTo);
        TestUtils.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingUndefined, false);
        testUtils.sendMessage();
        TestUtils.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingReliable, true);
        testUtils.checkToolbarColor(R.color.pep_yellow);
        testUtils.pressBack();
        assertToolbarColor();
    }

    public void assertToolbarColor() {
        testUtils.checkToolBarColor(
                ThemeManager.isDarkTheme()
                        ? R.color.dark_theme_overlay_1
                        : R.color.pep_green
        );
    }

    private void composeMessage(String to) {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, to), false);
    }

    private void composeMessageToMyself() {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
    }
}
