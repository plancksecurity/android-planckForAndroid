
package com.fsck.k9.activity.setup;


import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PepPermissionActivity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.ui.fragments.AccountSetupBasicsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupIncomingFragment;

import java.util.List;

/**
 * Prompts the user for the email address and password.
 * Attempts to lookup default settings for the domain the user specified. If the
 * domain is known the settings are handed off to the AccountSetupCheckSettings
 * activity. If no settings are found the settings are handed off to the
 * AccountSetupAccountType activity.
 */
public class AccountSetupBasics extends PepPermissionActivity {

    private static final int ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 1;
    private static final int DIALOG_NO_FILE_MANAGER = 4;
    private AccountSetupBasicsFragment accountSetupBasicsFragment;
    private View.OnClickListener homeButtonListener;

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
            ft.add(R.id.account_setup_container, accountSetupBasicsFragment).commit();
        }
        PEpUtils.askForBatteryOptimizationWhiteListing(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.accout_setup_basic_option, menu);
        return true;
    }

    private void onImport() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> infos = packageManager.queryIntentActivities(i, 0);

        if (infos.size() > 0) {
            startActivityForResult(Intent.createChooser(i, null),
                    ACTIVITY_REQUEST_PICK_SETTINGS_FILE);
        } else {
            showDialog(DIALOG_NO_FILE_MANAGER);
        }
    }

    @Override
    public void search(String query) {

    }

    @Override
    protected void initializeInjector(ApplicationComponent applicationComponent) {

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
                if (homeButtonListener != null) {
                    homeButtonListener.onClick(item.getActionView());
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void setHomeButtonListener(View.OnClickListener onClickListener) {
        this.homeButtonListener = onClickListener;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (homeButtonListener != null) {
            homeButtonListener.onClick(getRootView());
        }
    }
}
