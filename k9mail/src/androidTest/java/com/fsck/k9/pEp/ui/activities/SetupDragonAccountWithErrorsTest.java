package com.fsck.k9.pEp.ui.activities;


import android.support.annotation.NonNull;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static java.lang.Thread.sleep;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SetupDragonAccountWithErrorsTest {

    @Rule
    public ActivityTestRule<AccountSetupBasics> activityTestRule = new ActivityTestRule<>(AccountSetupBasics.class);

    @Test
    public void setupDragonAccountWithErrorsTest() {
        accountSetupBasics();
        incomingSettings();
        outgoingSettings();
    }

    private void outgoingSettings() {
        //you make a mistake before setting the correct configuration
        doWait();
        Espresso.pressBack();
        doWait();

        //set the correct configuration
        onView(withId(R.id.account_server)).perform(scrollTo(), replaceText(getServer()), closeSoftKeyboard());

        clickNext();
        clickAccept();
    }

    private void incomingSettings() {
        //you make a mistake before setting the correct configuration
        clickNext();

        doWait();
        Espresso.pressBack();
        doWait();

        //set the correct configuration
        onView(withId(R.id.account_server)).perform(scrollTo(), replaceText(getServer()), closeSoftKeyboard());
        onView(withId(R.id.account_username)).perform(scrollTo(), replaceText(getUsername()), closeSoftKeyboard());

        clickNext();
        clickAccept();
        clickNext();
    }

    private void accountSetupBasics() {
        onView(withId(R.id.account_email)).perform(scrollTo(), replaceText(getEmail()), closeSoftKeyboard());
        onView(withId(R.id.account_password)).perform(scrollTo(), replaceText(getPassword()), closeSoftKeyboard());

        clickNext();
    }

    private void clickAccept() {
        doWait();
        onView(withId(android.R.id.button1)).perform(click());
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

    private void doWait() {
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
