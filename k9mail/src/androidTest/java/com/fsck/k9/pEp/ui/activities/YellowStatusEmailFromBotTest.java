package com.fsck.k9.pEp.ui.activities;

import android.app.Activity;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.internal.deps.guava.collect.Iterables;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.privacy.status.PEpTrustwords;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.core.internal.deps.guava.base.Preconditions.checkNotNull;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.TestUtils.TIMEOUT_TEST;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withBackgroundColor;
import static junit.framework.Assert.assertTrue;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class YellowStatusEmailFromBotTest {

    private UiDevice device;
    private TestUtils testUtils;
    private String messageTo = "";
    private static final String HOST = "test.pep-security.net";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private Instrumentation instrumentation;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(device, instrumentation);
        testUtils.increaseTimeoutWait();
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        testUtils.startActivity();
    }

    @Test (timeout = TIMEOUT_TEST)
    public void yellowStatusEmailFromBot (){
        sendMessageAndAssertYellowStatusMessage();
        twoStatusMessageYellowAndGray();

    }
    public void sendMessageAndAssertYellowStatusMessage() {
        testUtils.createAccount(false);
        testUtils.composeMessageButton();
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        testUtils.clickLastMessageReceived();
        testUtils.clickView(R.id.reply_message);
        onView(withId(R.id.subject)).perform(typeText(" "));
        clickMailStatus();
        testUtils.checkToolBarColor(R.color.pep_yellow);
        goBackToMessageList();
        testUtils.composeMessageButton();
        yellowStatusMessageTest();
    }

    public void twoStatusMessageYellowAndGray() {
        testUtils.composeMessageButton();
        fillComposeFields();
        onView(withId(R.id.subject)).perform(typeText(" "));
        clickMailStatus();
        testUtils.doWaitForResource(R.id.my_recycler_view);
        device.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(0)).check(matches(withBackgroundColor(R.color.pep_yellow)));
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(1)).check(matches(withBackgroundColor(R.color.pep_no_color)));
        testUtils.goBackAndRemoveAccount();
    }

    private void fillComposeFields() {
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        onView(withId(R.id.to)).perform(typeText("unknown@user.is"), closeSoftKeyboard());
        device.waitForIdle();
    }

    public static UtilsPackage.RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {

        return new UtilsPackage.RecyclerViewMatcher(recyclerViewId);
    }

    private void goBackToMessageList() {
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
        testUtils.doWaitForAlertDialog(splashActivityTestRule, R.string.save_or_discard_draft_message_dlg_title);
        testUtils.doWaitForObject("android.widget.Button");
        onView(withText(R.string.discard_action)).perform(click());
        device.waitForIdle();
        testUtils.pressBack();
        device.waitForIdle();
    }


    private void clickMailStatus() {
        testUtils.clickView(R.id.pEp_indicator);
    }

    private void yellowStatusMessageTest() {
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        onView(withId(R.id.pEp_indicator)).perform(click());
        device.waitForIdle();
        onView(withId(R.id.my_recycler_view)).check(doesNotExist());
        assertCurrentActivityIsInstanceOf(PEpTrustwords.class);

    }

    public void assertCurrentActivityIsInstanceOf(Class<? extends Activity> activityClass) {
        Activity currentActivity = testUtils.getCurrentActivity();
        checkNotNull(currentActivity);
        checkNotNull(activityClass);
        assertTrue(currentActivity.getClass().isAssignableFrom(activityClass));
    }
}
