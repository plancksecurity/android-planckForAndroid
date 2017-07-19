package com.fsck.k9.pEp.ui.fragments;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class AccountSetupBasicsFragmentTest {

    @Rule
    public ActivityTestRule<AccountSetupBasics> mActivityRule =
            new ActivityTestRule<>(AccountSetupBasics.class);

    @Test
    public void triggerIntentTest() {
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
    }

}