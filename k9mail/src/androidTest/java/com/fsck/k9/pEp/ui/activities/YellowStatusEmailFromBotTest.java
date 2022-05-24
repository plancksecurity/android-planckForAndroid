package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
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


@RunWith(AndroidJUnit4.class)
public class YellowStatusEmailFromBotTest {

    private TestUtils testUtils;
    private static final String HOST = "sq.pep.security";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private static final String UNKNOWN_ADDRESS = "unkown@user.is";

    private String botMail = System.currentTimeMillis() + "@" + HOST;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        UiDevice device = UiDevice.getInstance(instrumentation);
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

        checkToolbarColor(R.color.pep_no_color);
        onView(withId(R.id.my_recycler_view)).check(matches(withListSize(2)));

        onView(withRecyclerView(R.id.my_recycler_view).atPositionOnView(0, R.id.tvRatingStatus))
                .check(matches(withText(testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingReliable.value))));
        onView(withRecyclerView(R.id.my_recycler_view).atPositionOnView(1, R.id.tvRatingStatus))
                .check(matches(withText(testUtils.getResourceString(R.array.pep_title, Rating.pEpRatingCannotDecrypt.value))));
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
