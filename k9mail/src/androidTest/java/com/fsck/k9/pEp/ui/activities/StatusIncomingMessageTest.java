package com.fsck.k9.pEp.ui.activities;

import android.graphics.drawable.ColorDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.Checks;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.fsck.k9.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.pEp.jniadapter.Rating;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


public class StatusIncomingMessageTest {

    private static final String HOST = "test.pep-security.net";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    private UiDevice uiDevice;
    private TestUtils testUtils;
    private String messageTo;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(uiDevice);
        testUtils.increaseTimeoutWait();
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        testUtils.startActivity();
    }

    @Test
    public void pEpStatusIncomingTrustedMessageShouldBeGreen() {
        assertPartnerStatusIsTrusted();
        testUtils.pressBack();
        uiDevice.waitForIdle();
        testUtils.pressBack();
        uiDevice.waitForIdle();
        assertIncomingTrustedPartnerMessageIsGreen();
    }

    private void assertPartnerStatusIsTrusted() {
        testUtils.getLastMessageReceived();
        testUtils.composeMessageButton();
        uiDevice.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        testUtils.sendMessage();
        uiDevice.waitForIdle();
        testUtils.waitForMessageWithText("p≡p", "p≡pbot (" + messageTo + ")");
        testUtils.clickLastMessageReceived();
        testUtils.assertMessageStatus(Rating.pEpRatingReliable.value);
        uiDevice.waitForIdle();
        onView(withId(R.id.handshake_button_text)).perform(click());
        onView(withId(R.id.confirmTrustWords)).perform(click());
        testUtils.pressBack();
        testUtils.assertMessageStatus(Rating.pEpRatingTrusted.value);
    }

    private void assertIncomingTrustedPartnerMessageIsGreen() {
        testUtils.composeMessageButton();
        fillMessage();
        uiDevice.waitForIdle();
        onView(withId(R.id.pEp_indicator)).perform(click());
        uiDevice.waitForIdle();
        testUtils.doWaitForResource(R.id.my_recycler_view);
        uiDevice.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(0)).check(matches(withBackgroundColor(R.color.pep_green)));
    }

    private void fillMessage() {
        uiDevice.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        uiDevice.waitForIdle();
        onView(withId(R.id.subject)).perform(longClick(), closeSoftKeyboard());
        uiDevice.waitForIdle();
    }

    private static UtilsPackage.RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {

        return new UtilsPackage.RecyclerViewMatcher(recyclerViewId);
    }

    private static Matcher<View> withBackgroundColor(final int colorId) {
        Checks.checkNotNull(colorId);
        int colorFromResource = ContextCompat.getColor(getTargetContext(), colorId);
        return new BoundedMatcher<View, View>(View.class) {
            @Override
            public boolean matchesSafely(View view) {
                int backGroundColor = ((ColorDrawable) view.getBackground()).getColor();
                return colorFromResource == backGroundColor;
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }
}
