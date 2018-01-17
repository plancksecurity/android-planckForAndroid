package com.fsck.k9.pEp.ui.activities;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;


public class StatusIncomingMessageTest {

    private static final String HOST = "test.pep-security.net";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device);
        testUtils.increaseTimeoutWait();
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        testUtils.startActivity();
    }

    @Test
    public void pEpStatusIncomingTrustedMessageShouldBeGreen() {
        assertPartnerStatusIsTrusted();
        assertIncomingTrustedPartnerMessageIsGreen();
    }

    private void assertPartnerStatusIsTrusted() {
        testUtils.createAccount(false);
        testUtils.getLastMessageReceived();
        testUtils.composeMessageButton();
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForMessageWithText("p≡p", "p≡pbot (" + messageTo + ")");
        testUtils.clickLastMessageReceived();
        testUtils.assertMessageStatus(Rating.pEpRatingReliable.value);
        device.waitForIdle();
        onView(withId(R.id.handshake_button_text)).perform(click());
        onView(withId(R.id.confirmTrustWords)).perform(click());
        testUtils.pressBack();
        testUtils.assertMessageStatus(Rating.pEpRatingTrusted.value);
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.pressBack();
    }

    private void assertIncomingTrustedPartnerMessageIsGreen() {
        testUtils.composeMessageButton();
        fillMessage();
        device.waitForIdle();
        onView(withId(R.id.pEp_indicator)).perform(click());
        device.waitForIdle();
        testUtils.doWaitForResource(R.id.my_recycler_view);
        device.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(0)).check(matches(withBackgroundColor(R.color.pep_green)));
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.save_or_discard_draft_message_dlg_title);
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.removeLastAccount();
    }

    private void fillMessage() {
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        device.waitForIdle();
        onView(withId(R.id.subject)).perform(longClick(), closeSoftKeyboard());
        device.waitForIdle();
    }

    private static UtilsPackage.RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {

        return new UtilsPackage.RecyclerViewMatcher(recyclerViewId);
    }

}
