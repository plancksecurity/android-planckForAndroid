package com.fsck.k9.planck.ui.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withListSize;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withRecyclerView;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;

import foundation.pEp.jniadapter.Rating;


public class StatusIncomingMessageTest extends BaseAndroidTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private final String messageTo = System.currentTimeMillis() + "@" + HOST;

    @Before
    public void startpEpApp() {
        testUtils.setupAccountIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void pEpStatusIncomingTrustedMessageShouldBeGreen() {
        testUtils.getMessageListSize();
        sendMessageToBot();
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();
        acceptHandshakeWithPartner();
        testUtils.pressBack();
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
        TestUtils.waitForIdle();
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();

        fillMessage();

        testUtils.sendMessage();
    }

    private void acceptHandshakeWithPartner() {
        testUtils.assertSecurityStatusText(Rating.pEpRatingReliable);

        testUtils.clickMessageStatus();

        checkToolbarColor(R.color.planck_yellow);
        onView(withId(R.id.my_recycler_view)).check(matches(withListSize(1)));
        onView(withRecyclerView(R.id.my_recycler_view).atPositionOnView(0, R.id.tvRatingStatus))
                .check(matches(withText(testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingReliable.value))));

        TestUtils.waitForIdle();
        testUtils.clickView(R.id.confirmHandshake);
        TestUtils.waitForIdle();
        testUtils.pressBack();
    }

    private void assertPartnerIsGreenAndSendMessage() {
        TestUtils.waitForIdle();
        testUtils.getMessageListSize();
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        fillMessage();
        testUtils.assertSecurityStatusText(
                Rating.pEpRatingTrustedAndAnonymized
        );

        testUtils.sendMessage();
    }

    private void assertIncomingTrustedPartnerMessageIsGreen() {
        testUtils.waitForNewMessage();
        testUtils.clickFirstMessage();

        testUtils.clickStatus();
        checkToolbarColor(R.color.planck_green);
        onView(withId(R.id.my_recycler_view)).check(matches(withListSize(1)));
        onView(withRecyclerView(R.id.my_recycler_view).atPositionOnView(0, R.id.tvRatingStatus))
                .check(matches(withText(testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingTrustedAndAnonymized.value))));
        testUtils.pressBack();
    }

    private void fillMessage() {
        testUtils.fillMessage(
                new TestUtils.BasicMessage(
                        BuildConfig.PLANCK_TEST_EMAIL_ADDRESS,
                        MESSAGE_SUBJECT,
                        MESSAGE_BODY,
                        messageTo
                ),
                false
        );
    }
}
