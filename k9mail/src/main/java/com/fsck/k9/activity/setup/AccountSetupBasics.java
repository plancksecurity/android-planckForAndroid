
package com.fsck.k9.activity.setup;


import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.ui.fragments.AccountSetupBasicsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupIncomingFragment;

/**
 * Prompts the user for the email address and password.
 * Attempts to lookup default settings for the domain the user specified. If the
 * domain is known the settings are handed off to the AccountSetupCheckSettings
 * activity. If no settings are found the settings are handed off to the
 * AccountSetupAccountType activity.
 */
public class AccountSetupBasics extends K9Activity {

    private AccountSetupBasicsFragment accountSetupBasicsFragment;

    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_basics);
        if (savedInstanceState == null) {
            accountSetupBasicsFragment = new AccountSetupBasicsFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.account_setup_container, accountSetupBasicsFragment).commit();
        }
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
}
