
package com.fsck.k9.activity.setup;


import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.misc.NonConfigurationInstance;
import com.fsck.k9.pEp.PEpImporterActivity;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.fragments.AccountSetupBasicsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupIncomingFragment;
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator;

import javax.inject.Inject;

/**
 * Prompts the user for the email address and password.
 * Attempts to lookup default settings for the domain the user specified. If the
 * domain is known the settings are handed off to the AccountSetupCheckSettings
 * activity. If no settings are found the settings are handed off to the
 * AccountSetupAccountType activity.
 */
public class AccountSetupBasics extends PEpImporterActivity {

    private static final int ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 1;
    private static final int DIALOG_NO_FILE_MANAGER = 4;
    private AccountSetupBasicsFragment accountSetupBasicsFragment;
    public boolean isManualSetupRequired;
    @Inject AccountSetupNavigator accountSetupNavigator;
    private NonConfigurationInstance nonConfigurationInstance;

    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindViews(R.layout.account_setup_basics);
        if (savedInstanceState == null) {
            accountSetupBasicsFragment = new AccountSetupBasicsFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.addToBackStack("AccountSetupBasicsFragment");
            ft.add(R.id.account_setup_container, accountSetupBasicsFragment).commit();
        }
        PEpUtils.askForBatteryOptimizationWhiteListing(this);

        // Handle activity restarts because of a configuration change (e.g. rotating the screen)
        nonConfigurationInstance = (NonConfigurationInstance) getLastNonConfigurationInstance();
        if (nonConfigurationInstance != null) {
            nonConfigurationInstance.restore(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.account_setup_basic_option, menu);
        return true;
    }

    @Override
    protected void refresh() {
    }

    @Override
    public void onImport(Uri uri) {
        ListImportContentsAsyncTask asyncTask = new ListImportContentsAsyncTask(this, uri);
        setNonConfigurationInstance(asyncTask);
        asyncTask.execute();
    }

    @Override
    public void setNonConfigurationInstance(NonConfigurationInstance inst) {
        nonConfigurationInstance = inst;
    }

    @Override
    protected void onImportFinished() {
        Accounts.listAccounts(this);
    }

    @Override
    public void search(String query) {

    }

    @Override
    public void showPermissionGranted(String permissionName) {
        accountSetupBasicsFragment.contactsPermissionGranted();
    }

    @Override
    public void showPermissionDenied(String permissionName, boolean permanentlyDenied) {
        accountSetupBasicsFragment.contactsPermissionDenied();
    }

    @Override
    public void inject() {
        getpEpComponent().inject(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AccountSetupIncomingFragment accountSetupIncomingFragment = (AccountSetupIncomingFragment)getFragmentManager().findFragmentByTag("accountSetupIncomingFragment");
        if (accountSetupIncomingFragment != null && accountSetupIncomingFragment.isVisible()) {
            accountSetupIncomingFragment.onActivityResult(requestCode, resultCode, data);
        } else {
            accountSetupBasicsFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.import_settings:
                onImport();
                break;
            case android.R.id.home:
                goBack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void goBack() {
        if (accountSetupNavigator.shouldDeleteAccount()) {
            deleteAccount();
        }
        accountSetupNavigator.goBack(this, getFragmentManager());
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    public AccountSetupNavigator getAccountSetupNavigator() {
        return accountSetupNavigator;
    }

    @Override
    protected void onDestroy() {
        if (accountSetupNavigator.shouldDeleteAccount() && isManualSetupRequired) {
            deleteAccount();
        }
        super.onDestroy();
    }

    private void deleteAccount() {
        Preferences.getPreferences(getApplicationContext()).deleteAccount(accountSetupNavigator.getAccount());
    }

    public boolean isManualSetupRequired() {
        return isManualSetupRequired;
    }

    public void setManualSetupRequired(boolean manualSetupRequired) {
        isManualSetupRequired = manualSetupRequired;
    }
}
