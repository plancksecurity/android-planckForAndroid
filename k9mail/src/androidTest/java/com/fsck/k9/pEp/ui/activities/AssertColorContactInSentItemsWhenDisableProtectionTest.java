package com.fsck.k9.pEp.ui.activities;

import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;

import com.fsck.k9.R;
import com.fsck.k9.pEp.EspressoTestingIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import foundation.pEp.jniadapter.Rating;

@RunWith(AndroidJUnit4.class)
public class AssertColorContactInSentItemsWhenDisableProtectionTest {
    private UiDevice device;
    private TestUtils testUtils;
    private static final String MESSAGE_SUBJECT = "Subject";
    private static final String MESSAGE_BODY = "Message";
    private Resources resources;

    @Rule
    public ActivityTestRule<SplashActivity> splashActivityTestRule = new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void startActivity() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        testUtils = new TestUtils(device, InstrumentationRegistry.getInstrumentation());
        new EspressoTestingIdlingResource();
        IdlingRegistry.getInstance().register(EspressoTestingIdlingResource.getIdlingResource());
        resources = ApplicationProvider.getApplicationContext().getResources();
        testUtils.setupAccountIfNeeded();
    }

    @After
    public void tearDown() {
        splashActivityTestRule.finishActivity();
        IdlingRegistry.getInstance().unregister(EspressoTestingIdlingResource.getIdlingResource());
    }

    @Test
    public void sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure() {
        composeMessage();
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false);
        testUtils.selectFromStatusPopupMenu(R.string.pep_force_unprotected);
        testUtils.assertMessageStatus(Rating.pEpRatingTrustedAndAnonymized, false, false);
        device.waitForIdle();
        testUtils.sendMessage();
        testUtils.waitForNewMessage();
        clickFirstMessageFromSentFolder();
        testUtils.assertMessageStatus(Rating.pEpRatingUndefined, false);
    }

    private void composeMessage() {
        testUtils.composeMessageButton();
        device.waitForIdle();
        String messageTo = testUtils.getTextFromTextViewThatContainsText("@");
        testUtils.fillMessage(new TestUtils.BasicMessage("", MESSAGE_SUBJECT, MESSAGE_BODY, messageTo), false);
    }

    private void clickFirstMessageFromSentFolder() {
        String folderName = resources.getString(R.string.special_mailbox_name_sent);
        testUtils.goToFolder(folderName);
        testUtils.clickFirstMessage();
    }
}

