
package com.fsck.k9.activity.setup;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.ui.fragments.AccountSetupOutgoingFragment;

public class AccountSetupOutgoing extends K9Activity {
    private static final String EXTRA_ACCOUNT = "account";

    public static void actionEditOutgoingSettings(Context context, Account account) {
        context.startActivity(intentActionEditOutgoingSettings(context, account));
    }

    public static Intent intentActionEditOutgoingSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupOutgoing.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindViewsForLayout(R.layout.account_setup_outgoing);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        Account mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        if (savedInstanceState == null) {
            AccountSetupOutgoingFragment accountSetupIncomingFragment = AccountSetupOutgoingFragment.intentActionEditOutgoingSettings(mAccount);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.account_setup_container, accountSetupIncomingFragment).commit();
        }
    }

    @Override
    public void search(String query) {

    }

}
