package com.fsck.k9.pEp.ui.activities;

import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
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
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.withRecyclerView;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class StoreDraftEncryptedOnTrustedServersWhenNeverUnprotected {
    private UiDevice device;
    private TestUtils testUtils;
    private Resources resources;
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        resources = ApplicationProvider.getApplicationContext().getResources();
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        testUtils.setupAccountIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    private void assertTextInPopupMenu(boolean isAlwaysSecureAppears) {
        testUtils.waitUntilViewDisplayed(R.id.actionbar_message_view);
        onView(withId(R.id.actionbar_message_view)).perform(longClick());
        device.waitForIdle();
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

    @Test
    public void StoreDraftEncryptedOnTrustedServerWhenNeverUnprotected() {
        deactivateStoreMessagesSecurelyAndReturn();
        testUtils.composeMessageButton();
        String messageFrom = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage(messageFrom, MESSAGE_SUBJECT, MESSAGE_BODY, messageFrom), false);
        device.waitForIdle();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);

        assertTextInPopupMenu(true);

        testUtils.selectFromStatusPopupMenu(R.string.is_always_secure);
        assertTextInPopupMenu(false);

        testUtils.goBackFromMessageCompose(true);
        clickFirstMessageFromDraft();

        assertTextInPopupMenu(false);
    }

    private void clickFirstMessageFromDraft() {
        String folderName = resources.getString(R.string.special_mailbox_name_drafts);
        testUtils.goToFolder(folderName);
        testUtils.clickFirstMessage();
    }

    private void deactivateStoreMessagesSecurelyAndReturn() {
        testUtils.selectFromMenu(R.string.action_settings);
        onView(withRecyclerView(R.id.accounts_list).atPosition(0)).perform(click());
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.privacy_preferences);
        if(exists(onView(withText(R.string.account_settings_save_encrypted_summary_enabled)))) {
            testUtils.selectFromScreen(R.string.account_settings_save_encrypted_summary_enabled);
        }
        testUtils.pressBack();
        testUtils.pressBack();
        testUtils.pressBack();
    }
}