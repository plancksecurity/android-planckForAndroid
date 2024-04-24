package com.fsck.k9.planck.ui.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withBackgroundColor;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withListSize;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withRecyclerView;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.K9;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;


@RunWith(AndroidJUnit4.class)
public class YellowStatusEmailFromBotTest extends BaseAndroidTest {

    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private static final String UNKNOWN_ADDRESS = "unkown@user.is";

    private final String botMail = System.currentTimeMillis() + "@" + HOST;

    @Before
    public void startpEpApp() {
        testUtils.setupAccountIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void yellowStatusEmailFromBot (){
        sendMessageAndAssertYellowStatusMessage();
        twoStatusMessageYellowAndGray();
    }

    private void sendMessageAndAssertYellowStatusMessage() {
        testUtils.getMessageListSize();
        sendMessage(botMail);
        testUtils.waitForNewMessage();
        TestUtils.waitForIdle();
        testUtils.clickFirstMessage();

        testUtils.assertMessageStatus(Rating.pEpRatingReliable, true);
        testUtils.pressBack();

        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        fillComposeFields(botMail);
        testUtils.assertMessageStatus(Rating.pEpRatingReliable, true);
        TestUtils.waitForIdle();
        testUtils.goBackFromMessageCompose(false);
    }

    private void twoStatusMessageYellowAndGray() {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();

        String ownAddress = testUtils.getTextFromTextViewThatContainsText("@");

        fillComposeFields(botMail + "\n" + UNKNOWN_ADDRESS + "\n" + ownAddress);
        TestUtils.waitForIdle();
        selectPrivacyStatusFromMenu();
        TestUtils.waitForIdle();

        checkToolbarColor(
                R.color.compose_unsecure_delivery_warning
        );
        onView(withId(R.id.my_recycler_view)).check(matches(withListSize(2)));

        onView(
                withRecyclerView(R.id.my_recycler_view)
                        .atPositionOnView(0, R.id.tvRatingStatus)
        ).check(matches(withText(
                testUtils.getResourceString(
                        R.array.pep_title, Rating.pEpRatingReliable.value
                )
        )));
        onView(
                withRecyclerView(R.id.my_recycler_view)
                        .atPositionOnView(1, R.id.tvRatingStatus)
        ).check(matches(withText(
                testUtils.getResourceString(
                        R.array.pep_title, Rating.pEpRatingCannotDecrypt.value
                )
        )));
    }

    private void sendMessage(String messageTo) {
        testUtils.composeMessageButton();
        TestUtils.waitForIdle();
        fillComposeFields(messageTo);
        testUtils.sendMessage();
        TestUtils.waitForIdle();
    }

    private void selectPrivacyStatusFromMenu() {
        testUtils.openOptionsMenu();
        TestUtils.waitForIdle();
        testUtils.selectFromScreen(R.string.pep_title_activity_privacy_status);
        TestUtils.waitForIdle();
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

    private void fillComposeFields(String messageTo) {
        testUtils.doWait("to");
        TestUtils.waitForIdle();
        onView(withId(R.id.subject)).perform(replaceText(MESSAGE_SUBJECT));
        TestUtils.waitForIdle();
        onView(withId(R.id.message_content)).perform(typeText(MESSAGE_BODY));
        TestUtils.waitForIdle();
        onView(withId(R.id.to))
                .perform(typeText(messageTo + "\n"), closeSoftKeyboard());
    }
}
