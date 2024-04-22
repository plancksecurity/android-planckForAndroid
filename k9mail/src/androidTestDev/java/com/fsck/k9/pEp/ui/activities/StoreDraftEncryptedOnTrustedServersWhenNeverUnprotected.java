package com.fsck.k9.planck.ui.activities;

import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.fsck.k9.R;
import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.planck.ui.activities.UtilsPackage.withRecyclerView;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class StoreDraftEncryptedOnTrustedServersWhenNeverUnprotected extends BaseAndroidTest {
    private Resources resources;
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    @Before
    public void startActivity() {
        resources = ApplicationProvider.getApplicationContext().getResources();
        testUtils.setupAccountIfNeeded();
    }

    private void assertTextInPopupMenu(boolean isAlwaysSecureAppears) {
        testUtils.waitUntilViewDisplayed(R.id.actionbar_message_view);
        onView(withId(R.id.actionbar_message_view)).perform(longClick());
        TestUtils.waitForIdle();
        onViewOnPopupWindow(R.string.pep_force_unprotected).check(matches(isDisplayed()));
        if(isAlwaysSecureAppears) {
            onViewOnPopupWindow(R.string.is_always_secure).check(matches(isDisplayed()));
        }
        else {
            onViewOnPopupWindow(R.string.is_not_always_secure).check(matches(isDisplayed()));
        }

        testUtils.pressBack();
    }

    private ViewInteraction onViewOnPopupWindow(int stringId) {
        return onView(withText(stringId)).inRoot(testUtils.isPopupWindow());
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void StoreDraftEncryptedOnTrustedServerWhenNeverUnprotected() {
        deactivateStoreMessagesSecurelyAndReturn();
        testUtils.composeMessageButton();
        String messageFrom = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage(messageFrom, MESSAGE_SUBJECT, MESSAGE_BODY, messageFrom), false);
        TestUtils.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);

        assertTextInPopupMenu(true);

        testUtils.selectFromStatusPopupMenu(R.string.is_always_secure);
        assertTextInPopupMenu(false);

        testUtils.goBackFromMessageCompose(true);
        clickFirstMessageFromDraft();

        assertTextInPopupMenu(false);
    }

    private void clickFirstMessageFromDraft() {
        testUtils.goToDraftsFolder();
        testUtils.clickFirstMessage();
    }

    private void deactivateStoreMessagesSecurelyAndReturn() {
        testUtils.selectFromMenu(R.string.action_settings);
        onView(withRecyclerView(R.id.accounts_list).atPosition(0)).perform(click());
        TestUtils.waitForIdle();
        testUtils.selectFromScreen(R.string.privacy_preferences);
        if(exists(onView(withText(R.string.account_settings_save_encrypted_summary_enabled)))) {
            testUtils.selectFromScreen(R.string.account_settings_save_encrypted_summary_enabled);
        }
        testUtils.pressBack();
        testUtils.pressBack();
        testUtils.pressBack();
    }
}