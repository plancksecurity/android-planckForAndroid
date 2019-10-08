
package com.fsck.k9.activity.setup;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.misc.NonConfigurationInstance;
import com.fsck.k9.pEp.PEpImporterActivity;
import com.fsck.k9.pEp.ui.fragments.AccountSetupBasicsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupIncomingFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupOutgoingFragment;
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
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_EDIT_INCOMING = "extra_edit_incoming";
    private static final String EXTRA_EDIT_OUTGOING = "extra_edit_outgoing";
    private static final String EXTRA_BACK_OUTGOING = "extra_back_outgoing";
    private AccountSetupBasicsFragment accountSetupBasicsFragment;
    public boolean isManualSetupRequired;
    public boolean isEditingIncomingSettings;
    public boolean isEditingOutgoingSettings;
    public boolean isBackOutgoingSettings;
    private NonConfigurationInstance nonConfigurationInstance;
    @Inject AccountSetupNavigator accountSetupNavigator;
    private boolean isGoingBack = false;

    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        context.startActivity(i);
    }

    public static void actionEditIncomingSettings(Activity context, String accountUuid) {
        context.startActivity(intentActionEditIncomingSettings(context, accountUuid));
    }

    public static Intent intentActionEditIncomingSettings(Context context, String accountUuid) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        i.putExtra(EXTRA_EDIT_INCOMING, true);
        i.putExtra(EXTRA_ACCOUNT, accountUuid);
        return i;
    }

    public static void actionEditOutgoingSettings(Context context, String accountUuid) {
        context.startActivity(intentActionEditOutgoingSettings(context, accountUuid));
    }

    public static Intent intentActionEditOutgoingSettings(Context context, String accountUuid) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        i.putExtra(EXTRA_EDIT_OUTGOING, true);
        i.putExtra(EXTRA_ACCOUNT, accountUuid);
        return i;
    }

    public static void actionBackToOutgoingSettings(Context context, Account account) {
        context.startActivity(intentActionBackToOutgoingSettings(context, account));
    }

    public static Intent intentActionBackToOutgoingSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        i.putExtra(EXTRA_BACK_OUTGOING, true);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindViews(R.layout.account_setup_basics);
        isEditingIncomingSettings = getIntent().getBooleanExtra(EXTRA_EDIT_INCOMING, false);
        isEditingOutgoingSettings = getIntent().getBooleanExtra(EXTRA_EDIT_OUTGOING, false);
        isBackOutgoingSettings = getIntent().getBooleanExtra(EXTRA_BACK_OUTGOING, false);
        if (isEditingIncomingSettings) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.addToBackStack("AccountSetupIncomingFragment");
            String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
            ft.add(R.id.account_setup_container, AccountSetupIncomingFragment.actionEditIncomingSettings(accountUuid)).commit();
            accountSetupNavigator.setIsEditing(true);
        } else if (isEditingOutgoingSettings) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.addToBackStack("AccountSetupIncomingFragment");
            String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
            ft.add(R.id.account_setup_container, AccountSetupOutgoingFragment.intentActionEditOutgoingSettings(accountUuid)).commit();
            accountSetupNavigator.setIsEditing(true);
        } else if (isBackOutgoingSettings) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.addToBackStack("AccountSetupIncomingFragment");
            String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
            ft.add(R.id.account_setup_container, AccountSetupOutgoingFragment.intentBackToOutgoingSettings(accountUuid)).commit();
        }
        else if (savedInstanceState == null) {
            accountSetupBasicsFragment = new AccountSetupBasicsFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.addToBackStack("AccountSetupBasicsFragment");
            ft.add(R.id.account_setup_container, accountSetupBasicsFragment).commit();
        }
        askForBatteryOptimizationWhiteListing();

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
        ListImportContentsAsyncTask asyncTask = new ListImportContentsAsyncTask(this, uri,
                null, false, null);
        setNonConfigurationInstance(asyncTask);
        asyncTask.execute();
    }

    @Override
    public void setNonConfigurationInstance(NonConfigurationInstance inst) {
        nonConfigurationInstance = inst;
    }

    @Override
    protected void onImportFinished() {
        Accounts.Companion.listAccounts(this);
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
        AccountSetupIncomingFragment accountSetupIncomingFragment = (AccountSetupIncomingFragment)getSupportFragmentManager().findFragmentByTag("accountSetupIncomingFragment");
        if (accountSetupIncomingFragment != null && accountSetupIncomingFragment.isVisible()) {
            accountSetupIncomingFragment.onActivityResult(requestCode, resultCode, data);
        } else {
            if (accountSetupBasicsFragment != null && accountSetupBasicsFragment.isVisible()) {
                accountSetupBasicsFragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isGoingBack = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.import_settings:
                onSettingsImport();
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void goBack() {
        if (accountSetupNavigator.shouldDeleteAccount() && !isEditingIncomingSettings && !isEditingOutgoingSettings) {
            deleteAccount();
        }
        accountSetupNavigator.goBack(this, getSupportFragmentManager());
    }

    @Override
    public void onBackPressed() {
        isGoingBack = true;
        goBack();
    }

    public AccountSetupNavigator getAccountSetupNavigator() {
        return accountSetupNavigator;
    }

    @Override
    protected void onDestroy() {
        if (accountSetupNavigator.shouldDeleteAccount() && isManualSetupRequired && isGoingBack) {
            deleteAccount();
        }
        isGoingBack = false;
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
