package com.fsck.k9.pEp.ui.activities;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pEp.jniadapter.Rating;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.fsck.k9.pEp.ui.activities.UtilsPackage.exists;

@RunWith(AndroidJUnit4.class)
public class StoreDraftEncryptedOnTrustedServersWhenNeverUnprotected {
    private UiDevice device;
    private TestUtils testUtils;
    private Resources resources;
    private String messageTo = "username@email.com";
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private Instrumentation instrumentation;
    private EspressoTestingIdlingResource espressoTestingIdlingResource;

    @Rule
    public IntentsTestRule<SplashActivity> splashActivityTestRule = new IntentsTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        espressoTestingIdlingResource = new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(espressoTestingIdlingResource.getIdlingResource());
        resources = InstrumentationRegistry.getTargetContext().getResources();
        testUtils = new TestUtils(device, instrumentation);
        testUtils.increaseTimeoutWait();
        testUtils.startActivity();
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(espressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void StoreDraftEncryptedOnTrustedServersWhenNeverUnprotected() {
        testUtils.createAccount(false);
        storeMessagesSecurely();
        testUtils.goBackToMessageListAndPressComposeMessageButton();
        device.waitForIdle();
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
        device.waitForIdle();
        testUtils.checkStatus(Rating.pEpRatingUnencrypted);
        testUtils.pressBack();
        testUtils.selectoFromMenu(R.string.is_always_secure);
        testUtils.goBackAndSaveAsDraft(splashActivityTestRule);
        clickFirstMessageFromDraft();
        openOptionsMenu();
        testUtils.assertsTextsOnScreenAreEqual(R.string.is_not_always_secure, R.string.is_not_always_secure);
        testUtils.goBackAndRemoveAccount(true);
    }

    private void clickFirstMessageFromDraft() {
        selectoFromMenu(R.string.account_settings_folders);
        device.waitForIdle();
        testUtils.selectFromScreen(R.string.special_mailbox_name_drafts);
        clickFirstMessage();
    }

    private void storeMessagesSecurely() {
        testUtils.openOptionsMenu();
        testUtils.selectFromScreen(R.string.preferences_action);
        testUtils.selectFromScreen(R.string.account_settings_action);
        testUtils.selectFromScreen(R.string.app_name);
        testUtils.checkBoxOnScreenChecked(R.string.pep_mistrust_server_and_store_mails_encrypted, false);
    }

    private void openOptionsMenu() {
        while (exists(onView(withId(R.id.toolbar)))){
            testUtils.openOptionsMenu();
            device.waitForIdle();
        }
    }

    private void clickFirstMessage() {
        Activity currentActivity = testUtils.getCurrentActivity();
        while (currentActivity == testUtils.getCurrentActivity()) {
            testUtils.clickFirstMessage();
            device.waitForIdle();
        }
    }

    void selectoFromMenu(int viewId){
        device.waitForIdle();
        testUtils.openOptionsMenu();
        testUtils.selectFromScreen(viewId);
    }
}