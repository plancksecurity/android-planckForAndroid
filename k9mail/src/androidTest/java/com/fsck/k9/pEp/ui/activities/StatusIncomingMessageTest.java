package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import foundation.pEp.jniadapter.Rating;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withListSize;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withRecyclerView;


public class StatusIncomingMessageTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = System.currentTimeMillis() + "@" + HOST;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        testUtils = new TestUtils(device, instrumentation);
        testUtils.setupAccountIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void pEpStatusIncomingTrustedMessageShouldBeGreen() {
        acceptHandshakeWithPartner();
        assertPartnerIsGreenAndSendMessage();
        assertIncomingTrustedPartnerMessageIsGreen();
    }

    private void checkToolbarColor(int color) {
        boolean toolbarExists = false;
        while (!toolbarExists) {
            if (exists(onView(withId(R.id.toolbar)))) {
                onView(withId(R.id.toolbar)).check(matches(withBackgroundColor(color)));
                toolbarExists = true;
            }
        }
    }

    private void sendMessageToBot() {
        device.waitForIdle();
        testUtils.composeMessageButton();
        device.waitForIdle();

        fillMessage();

        testUtils.sendMessage();
    }

    private void acceptHandshakeWithPartner() {
        sendMessageToBot();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();

        onView(withId(R.id.securityStatusText)).check(matches(withText(
                testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingReliable.value))));

        testUtils.clickMessageStatus();

        checkToolbarColor(R.color.pep_yellow);
        onView(withId(R.id.my_recycler_view)).check(matches(withListSize(1)));
        onView(withRecyclerView(R.id.my_recycler_view).atPositionOnView(0, R.id.tvRatingStatus))
                .check(matches(withText(testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingReliable.value))));

        onView(withId(R.id.confirmHandshake)).perform(click());
        testUtils.goBackToMessageList();
    }

    private void assertPartnerIsGreenAndSendMessage() {
        device.waitForIdle();
        testUtils.composeMessageButton();
        device.waitForIdle();
        fillMessage();
        onView(withId(R.id.securityStatusText)).check(matches(withText(
                testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingTrustedAndAnonymized.value))));

        testUtils.sendMessage();
    }

    private void assertIncomingTrustedPartnerMessageIsGreen() {
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();

        testUtils.clickStatus();
        checkToolbarColor(R.color.pep_green);
        onView(withId(R.id.my_recycler_view)).check(matches(withListSize(1)));
        onView(withRecyclerView(R.id.my_recycler_view).atPositionOnView(0, R.id.tvRatingStatus))
                .check(matches(withText(testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingTrustedAndAnonymized.value))));
        testUtils.pressBack();
    }

    private void fillMessage() {
        onView(withId(R.id.to)).perform(typeText(messageTo), closeSoftKeyboard());
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(replaceText(MESSAGE_SUBJECT));
        device.waitForIdle();
        onView(withId(R.id.message_content)).perform(typeText(MESSAGE_BODY));
        device.waitForIdle();
    }
}
