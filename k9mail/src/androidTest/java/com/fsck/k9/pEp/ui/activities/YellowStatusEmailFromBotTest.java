package com.fsck.k9.pEp.ui.activities;

import android.app.Activity;
import android.app.Instrumentation;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;
import com.fsck.k9.pEp.ui.privacy.status.PEpTrustwords;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import timber.log.Timber;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.core.internal.deps.guava.base.Preconditions.checkNotNull;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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
    private EspressoTestingIdlingResource espressoTestingIdlingResource;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startpEpApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        testUtils = new TestUtils(device, instrumentation);
        testUtils.increaseTimeoutWait();
        espressoTestingIdlingResource = new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(espressoTestingIdlingResource.getIdlingResource());
        messageTo = Long.toString(System.currentTimeMillis()) + "@" + HOST;
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(espressoTestingIdlingResource.getIdlingResource());
    }

    @Test (timeout = TIMEOUT_TEST)
    public void yellowStatusEmailFromBot (){
        sendMessageAndAssertYellowStatusMessage();
        testUtils.goBackToMessageCompose();
        twoStatusMessageYellowAndGray();

    }
    public void sendMessageAndAssertYellowStatusMessage() {
        testUtils.createAccount();
        testUtils.composeMessageButton();
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        onView(withId(R.id.subject)).perform(typeText(" "));
        testUtils.sendMessage();
        device.waitForIdle();
        testUtils.waitForNewMessage();
        //testUtils.clickLastMessageReceived();
        testUtils.clickView(R.id.reply_message);
        onView(withId(R.id.subject)).perform(typeText(" "));
        onView(withId(R.id.message_content)).perform(typeText(" "));
        device.waitForIdle();
        clickMailStatus();
        testUtils.checkToolBarColor(R.color.pep_yellow);
        device.pressBack();
        testUtils.goBackToMessageListAndPressComposeMessageButton();
        yellowStatusMessageTest();
    }

    public void twoStatusMessageYellowAndGray() {
        testUtils.goBackToMessageListAndPressComposeMessageButton();
        fillComposeFields();
        onView(withId(R.id.subject)).perform(typeText(" "));
        clickMailStatus();
        testUtils.doWaitForResource(R.id.my_recycler_view);
        device.waitForIdle();
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(0)).check(matches(withBackgroundColor(R.color.pep_yellow)));
        onView(withRecyclerView(R.id.my_recycler_view).atPosition(1)).check(matches(withBackgroundColor(R.color.pep_no_color)));
        testUtils.goBackAndRemoveAccount(true);
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

    public void goBackDiscardMessageAndRemoveAccount(){
        boolean accountRemoved = false;
        boolean messageDiscarded = false;
        while (!accountRemoved) {
            try {
                testUtils.removeLastAccount();
                accountRemoved = true;
            } catch (Exception ex) {
                device.pressBack();
                try {
                    if (!messageDiscarded) {
                        device.waitForIdle();
                        onView(withText(R.string.discard_action)).perform(click());
                        messageDiscarded = true;
                    }
                } catch (Exception e){
                    Timber.i("No dialog alert message");
                }
                Timber.i("View not found, pressBack to previous activity: " + ex);
            }
        }
    }

    private void clickMailStatus() {
        testUtils.doWaitForResource(R.id.pEp_indicator);
        testUtils.clickView(R.id.pEp_indicator);
    }

    private void yellowStatusMessageTest() {
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        onView(withId(R.id.pEp_indicator)).perform(click());
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
