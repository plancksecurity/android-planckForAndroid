package com.fsck.k9.activity.setup;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.SettingsActivity;
import com.fsck.k9.activity.misc.NonConfigurationInstance;
import com.fsck.k9.planck.PlanckImporterActivity;
import com.fsck.k9.planck.ui.fragments.AccountSetupIncomingFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupOutgoingFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupSelectAuthFragment;
import com.fsck.k9.planck.ui.tools.AccountSetupNavigator;
import com.fsck.k9.planck.ui.tools.ThemeManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import security.planck.permissions.PermissionRequester;

/**
 * Prompts the user for the email address and password.
 * Attempts to lookup default settings for the domain the user specified. If the
 * domain is known the settings are handed off to the AccountSetupCheckSettings
 * activity. If no settings are found the settings are handed off to the
 * AccountSetupAccountType activity.
 */
@AndroidEntryPoint
public class AccountSetupBasics extends PlanckImporterActivity {

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_EDIT_INCOMING = "extra_edit_incoming";
    private static final String EXTRA_EDIT_OUTGOING = "extra_edit_outgoing";
    private boolean isManualSetupRequired;
    private boolean isEditingIncomingSettings;
    private boolean isEditingOutgoingSettings;
    @Inject
    AccountSetupNavigator accountSetupNavigator;
    private boolean isGoingBack = false;

    @Inject
    PermissionRequester permissionRequester;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindViews(R.layout.account_setup_basics);
        isEditingIncomingSettings = getIntent().getBooleanExtra(EXTRA_EDIT_INCOMING, false);
        isEditingOutgoingSettings = getIntent().getBooleanExtra(EXTRA_EDIT_OUTGOING, false);
        if (savedInstanceState == null) {
            if (isEditingIncomingSettings) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack("AccountSetupIncomingFragment");
                String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
                ft.replace(R.id.account_setup_container, AccountSetupIncomingFragment.actionEditIncomingSettings(accountUuid)).commit();
                accountSetupNavigator.setIsEditing(true);
            } else if (isEditingOutgoingSettings) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack("AccountSetupIncomingFragment");
                String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
                ft.replace(R.id.account_setup_container, AccountSetupOutgoingFragment.intentActionEditOutgoingSettings(accountUuid)).commit();
                accountSetupNavigator.setIsEditing(true);
            } else {
                AccountSetupSelectAuthFragment fragment = new AccountSetupSelectAuthFragment();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack("AccountSetupSelectAuthFragment");
                ft.replace(R.id.account_setup_container, fragment).commit();
            }
        }
        permissionRequester.requestBatteryOptimizationPermission();
    }

    public void configurePasswordFlowScreen() {
        getWindow().setBackgroundDrawableResource(
                ThemeManager.getAttributeResource(this, android.R.attr.windowBackground));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        showNavigationBar();
    }

    private void showNavigationBar() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.navigationBars());
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
        // NOP
    }

    @Override
    protected void onImportFinished() {
        SettingsActivity.Companion.actionBasicStart(this);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.account_setup_container);
        if(fragment != null && fragment.isAdded()) {
            fragment.onActivityResult(requestCode, resultCode, data);
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
        isGoingBack = false;
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
        // TODO: 07/07/2020 Review all Account deletion logic in AccountSetup workflow after we delay Account creation to AccountSetupNames.
    }

    public void setManualSetupRequired(boolean manualSetupRequired) {
        isManualSetupRequired = manualSetupRequired;
    }
}
