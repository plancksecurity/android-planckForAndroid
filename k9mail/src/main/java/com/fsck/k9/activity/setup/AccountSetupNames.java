
package com.fsck.k9.activity.setup;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpProviderFactory;
import com.fsck.k9.pEp.PEpUtils;

import org.pEp.jniadapter.Identity;

public class AccountSetupNames extends K9Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";

    private EditText mDescription;

    private EditText mName;

    private Account mAccount;

    private Button mDoneButton;

    public static void actionSetNames(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupNames.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindViewsForLayout(R.layout.account_setup_names);

        initializeToolbar(true, R.string.account_setup_names_title);
        setStatusBarPepColor(getResources().getColor(R.color.pep_green));

        mDescription = (EditText)findViewById(R.id.account_description);
        mName = (EditText)findViewById(R.id.account_name);
        mDoneButton = (Button)findViewById(R.id.done);
        mDoneButton.setOnClickListener(this);

        TextWatcher validationTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                validateFields();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        mName.addTextChangedListener(validationTextWatcher);

        mName.setKeyListener(TextKeyListener.getInstance(false, Capitalize.WORDS));

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        /*
         * Since this field is considered optional, we don't set this here. If
         * the user fills in a value we'll reset the current value, otherwise we
         * just leave the saved value alone.
         */
        // mDescription.setText(mAccount.getDescription());
        if (mAccount.getName() != null) {
            mName.setText(mAccount.getName());
        }
        if (!Utility.requiredFieldValid(mName)) {
            mDoneButton.setEnabled(false);
        }
    }

    @Override
    public void search(String query) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home: {
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void validateFields() {
        mDoneButton.setEnabled(Utility.requiredFieldValid(mName));
        Utility.setCompoundDrawablesAlpha(mDoneButton, mDoneButton.isEnabled() ? 255 : 128);
    }

    protected void onNext() {
        if (Utility.requiredFieldValid(mDescription)) {
            mAccount.setDescription(mDescription.getText().toString());
        }
        mAccount.setName(mName.getText().toString());
        mAccount.save(Preferences.getPreferences(this));
        new pEpGenerateAccountKeysTask().execute();
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.done:
            onNext();
            break;
        }
    }

    private void pEpGenerateAccountKeys() {
        PEpProvider pEp = PEpProviderFactory.createAndSetupProvider(getApplicationContext());
        Identity myIdentity = PEpUtils.createIdentity(new Address(mAccount.getEmail(), mAccount.getName()), getApplicationContext());
        pEp.myself(myIdentity);
        pEp.close();
    }

    private class pEpGenerateAccountKeysTask extends AsyncTask <Void, Void, Void> {
        ProgressDialog dialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(AccountSetupNames.this);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage(getString(R.string.pep_account_setup_generating_keys));
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dialog.dismiss();
            Accounts.listAccounts(AccountSetupNames.this);
            AccountSetupNames.this.finish();
        }

        @Override
        protected Void doInBackground(Void... params) {
            pEpGenerateAccountKeys();
            return null;
        }
    }
}
