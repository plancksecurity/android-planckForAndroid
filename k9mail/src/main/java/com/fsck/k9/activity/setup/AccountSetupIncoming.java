
package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import timber.log.Timber;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.ui.fragments.AccountSetupIncomingFragment;

public class AccountSetupIncoming extends K9Activity {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private Account mAccount;

    public static void actionIncomingSettings(Activity context, Account account, boolean makeDefault) {
        Intent i = new Intent(context, AccountSetupIncoming.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }

    public static void actionEditIncomingSettings(Activity context, Account account) {
        context.startActivity(intentActionEditIncomingSettings(context, account));
    }

    public static Intent intentActionEditIncomingSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupIncoming.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindViews(R.layout.account_setup_incoming);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        if (savedInstanceState == null) {
            AccountSetupIncomingFragment accountSetupIncomingFragment = AccountSetupIncomingFragment.actionEditIncomingSettings(mAccount);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.account_setup_container, accountSetupIncomingFragment).commit();
        }
    }

    @Override
    public void search(String query) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
