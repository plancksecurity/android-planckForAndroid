package com.fsck.k9.pEp.ui.fragments;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


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