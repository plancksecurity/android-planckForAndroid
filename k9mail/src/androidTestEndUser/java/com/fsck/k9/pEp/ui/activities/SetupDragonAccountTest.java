package com.fsck.k9.planck.ui.activities;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SetupDragonAccountTest extends BaseAndroidTest {
    @Before
    public void setUp() {
        testUtils.goToSettingsAndRemoveAllAccountsIfNeeded();
    }

    @After
    public void tearDown() {
        finishSetup();
        testUtils.goToSettingsAndRemoveAllAccounts();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void setupDragonAccountTest() {
        accountSetupBasics();
        incomingSettings();
        outgoingSettings();
    }

    private void outgoingSettings() {
        onView(withId(R.id.account_server)).perform(scrollTo(), replaceText(getServer()), closeSoftKeyboard());

        clickNext();
        testUtils.doWaitForAlertDialog(R.string.account_setup_failed_dlg_invalid_certificate_title);
        clickAccept();
    }

    private void incomingSettings() {
        onView(withId(R.id.account_server)).perform(scrollTo(), replaceText(getServer()), closeSoftKeyboard());
        onView(withId(R.id.account_username)).perform(scrollTo(), replaceText(getUsername()), closeSoftKeyboard());

        clickNext();
        testUtils.doWaitForAlertDialog(R.string.account_setup_failed_dlg_invalid_certificate_title);
        clickAccept();
    }

    private void accountSetupBasics() {
        onView(withId(R.id.account_email)).perform(scrollTo(), click());
        onView(withId(R.id.account_email)).perform(scrollTo(), replaceText(getEmail()), closeSoftKeyboard());
        onView(withId(R.id.account_password)).perform(scrollTo(), replaceText(getPassword()), closeSoftKeyboard());
        clickManualSetup();
    }

    private void clickManualSetup() {
        onView(withId(R.id.manual_setup)).perform(click());
    }

    private void clickAccept() {
        testUtils.clickAndroidDialogAccept();
    }

    private void clickNext() {
        onView(withId(R.id.next)).perform(click());
    }

    @NonNull
    private String getUsername() {
        return BuildConfig.PEP_DRAGON_USERNAME;
    }

    @NonNull
    private String getServer() {
        return BuildConfig.PEP_DRAGON_SERVER;
    }

    @NonNull
    private String getPassword() {
        return BuildConfig.PEP_DRAGON_PASSWORD;
    }

    @NonNull
    private String getEmail() {
        return BuildConfig.PEP_DRAGON_EMAIL_ADDRESS;
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    private void finishSetup() {
        testUtils.waitUntilViewDisplayed(R.id.next);
        clickNext();
        testUtils.waitUntilViewDisplayed(R.id.account_name);
        onView(withId(R.id.account_name)).perform(replaceText("test"));
        onView(withId(R.id.pep_enable_sync_account)).perform(click());

        onView(withId(R.id.done)).perform(click());
    }
}
