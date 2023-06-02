package com.fsck.k9.planck.ui.activities;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.common.BaseAndroidTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;



@RunWith(AndroidJUnit4.class)
public class ConfigureSeveralAccountsRemoveThemAndSetUpNewOne extends BaseAndroidTest {
    @Before
    public void startMainActivity() {
        testUtils.goToSettingsAndRemoveAllAccountsIfNeeded();
    }

    @Test(timeout = TestUtils.TIMEOUT_TEST)
    public void ConfigureAccountRemoveThemSetUpNewAccount() {
        int total = 8;
        for (int account = 0; account < total; account++) {
            testUtils.setupAccountAutomatically(false);
            testUtils.goToSettingsAndRemoveAllAccounts();
        }
    }

}
