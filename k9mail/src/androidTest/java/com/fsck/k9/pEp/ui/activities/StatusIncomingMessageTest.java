package com.fsck.k9.pEp.ui.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withListSize;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withRecyclerView;

import android.app.Instrumentation;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import foundation.pEp.jniadapter.Rating;


public class StatusIncomingMessageTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice device;
    private TestUtils testUtils;
    private final String messageTo = System.currentTimeMillis() + "@" + HOST;

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

        testUtils.assertSecurityStatusText(Rating.pEpRatingReliable);

        testUtils.clickMessageStatus();

        checkToolbarColor(R.color.pep_yellow);
        onView(withId(R.id.my_recycler_view)).check(matches(withListSize(1)));
        onView(withRecyclerView(R.id.my_recycler_view).atPositionOnView(0, R.id.tvRatingStatus))
                .check(matches(withText(testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingReliable.value))));

        TestUtils.waitForIdle();
        testUtils.clickView(R.id.confirmHandshake);
        TestUtils.waitForIdle();
        testUtils.pressBack();
        testUtils.pressBack();
    }

    private void assertPartnerIsGreenAndSendMessage() {
        device.waitForIdle();
        testUtils.composeMessageButton();
        device.waitForIdle();
        fillMessage();
        testUtils.assertSecurityStatusText(Rating.pEpRatingTrustedAndAnonymized);

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
        testUtils.fillMessage(
                new TestUtils.BasicMessage(
                        BuildConfig.PEP_TEST_EMAIL_ADDRESS,
                        MESSAGE_SUBJECT,
                        MESSAGE_BODY,
                        messageTo
                ),
                false
        );
    }
}
