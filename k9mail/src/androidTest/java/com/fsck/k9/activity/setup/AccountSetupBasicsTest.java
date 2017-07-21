package com.fsck.k9.activity.setup;

import android.content.Intent;
import android.support.test.espresso.ViewAssertion;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AccountSetupBasicsTest {

    @Rule
    public ActivityTestRule<AccountSetupBasics> mActivityRule =
            new ActivityTestRule<>(AccountSetupBasics.class);

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testManualSetup() {
        startActivity();
        setupEmailAndPassword();
        onView(withId(R.id.manual_setup)).perform(click());
        doWait();
        setupIncomingSettings();
        doWait();
        setupOutgoingSettings();
        doWait();
        accountSetupPEpOptions();
        doWait();
        accountSetupName();
    }

    @Test
    public void testAutomaticSetup() {
        startActivity();
        setupEmailAndPassword();
        onView(withId(R.id.next)).perform(click());
        doWait();
        accountSetupName();
    }

    private void doWait() {
        try {
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void accountSetupName() {
        onView(allOf(isAssignableFrom(TextView.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .check(matches(withText(R.string.account_setup_names_title)));

        onView(withId(R.id.account_name)).perform(replaceText("test"));
    }

    private void accountSetupPEpOptions() {
        onView(allOf(isAssignableFrom(TextView.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .check(matches(withText(R.string.account_settings_title_fmt)));

        onView(withId(R.id.next)).perform(click());
    }

    private void setupOutgoingSettings() {
        setupSettings(matches(withText(R.string.account_setup_outgoing_title)));
    }

    private void setupSettings(ViewAssertion matches) {
        onView(allOf(isAssignableFrom(TextView.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .check(matches);

        String server = BuildConfig.PEP_TEST_EMAIL_SERVER;

        onView(withId(R.id.account_server)).perform(replaceText(server));
        onView(withId(R.id.next)).perform(click());
    }

    private void setupIncomingSettings() {
        setupSettings(matches(withText(R.string.account_setup_incoming_title)));
    }

    private void setupEmailAndPassword() {
        onView(allOf(withId(R.id.next), withText(R.string.next_action))).check(matches(isDisplayed()));
        onView(allOf(isAssignableFrom(TextView.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .check(matches(withText(R.string.account_setup_basics_title)));

        String email = BuildConfig.PEP_TEST_EMAIL_ADDRESS;
        String pass = BuildConfig.PEP_TEST_EMAIL_PASSWORD;
        onView(withId(R.id.account_email)).perform(replaceText(email));
        onView(withId(R.id.account_password)).perform(replaceText(pass));
    }

    private void startActivity() {
        Intent intent = new Intent();
        mActivityRule.launchActivity(intent);
    }
}