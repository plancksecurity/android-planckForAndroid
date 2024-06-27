
package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.SwitchCompat;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.SettingsActivity;
import com.fsck.k9.activity.misc.ExtendedAsyncTask;
import com.fsck.k9.activity.misc.NonConfigurationInstance;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.PlanckUtils;
import com.fsck.k9.planck.ui.tools.KeyboardUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import security.planck.mdm.ConfigurationManager;
import security.planck.provisioning.ProvisioningScope;
import security.planck.ui.toolbar.ToolBarCustomizer;

@AndroidEntryPoint
public class AccountSetupNames extends K9Activity implements OnClickListener {
    public static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MANUAL_SETUP = "manualSetup";

    private EditText mDescription;

    private EditText mName;

    private Account mAccount;

    private Button mDoneButton;
    private SwitchCompat pepSyncAccount;
    private PlanckUIArtefactCache planckUIArtefactCache;
    private NonConfigurationInstance nonConfigurationInstance;

    @Inject
    ToolBarCustomizer toolBarCustomizer;

    @Inject
    ConfigurationManager configurationManager;

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        Object retain = null;
        if (nonConfigurationInstance != null && nonConfigurationInstance.retain()) {
            retain = nonConfigurationInstance;
        }
        return retain;
    }


    public static void actionSetNames(Context context, Account account, boolean manualSetup) {
        Intent i = new Intent(context, AccountSetupNames.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MANUAL_SETUP, manualSetup);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindViews(R.layout.account_setup_names);

        initializeToolbar(true, R.string.account_setup_names_title);
        toolBarCustomizer.setDefaultStatusBarColor();

        mDescription = (EditText)findViewById(R.id.account_description);
        mName = (EditText)findViewById(R.id.account_name);
        pepSyncAccount = findViewById(R.id.pep_enable_sync_account);
        pepSyncAccount.setVisibility(BuildConfig.IS_OFFICIAL ? View.GONE : View.VISIBLE);
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
        mAccount = Preferences.getPreferences(this).getAccountAllowingIncomplete(accountUuid);

        /*
         * Since this field is considered optional, we don't set this here. If
         * the user fills in a value we'll reset the current value, otherwise we
         * just leave the saved value alone.
         */
        // mDescription.setText(mAccount.getDescription());
        if (mAccount.getName() != null) {
            mName.setText(mAccount.getName());
        }
        if (getK9().isRunningOnWorkProfile()) {
            mDescription.setFocusable(false);
            mName.setFocusable(false);
            mDescription.setText(mAccount.getDescription());
        }
        if (!Utility.requiredFieldValid(mName)) {
            mDoneButton.setEnabled(false);
        }


        planckUIArtefactCache = PlanckUIArtefactCache.getInstance(getApplicationContext());
        planckUIArtefactCache.removeCredentialsInPreferences();

        restoreNonConfigurationInstance();
    }

    private void restoreNonConfigurationInstance() {
        nonConfigurationInstance = (NonConfigurationInstance) getLastCustomNonConfigurationInstance();
        if (nonConfigurationInstance != null) {
            nonConfigurationInstance.restore(this);
        }
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
        KeyboardUtils.hideKeyboard(AccountSetupNames.this);
        if (Utility.requiredFieldValid(mDescription)) {
            mAccount.setDescription(mDescription.getText().toString());
        }
        mAccount.setName(mName.getText().toString());
        mAccount.setPlanckSyncAccount(pepSyncAccount.isChecked());
        boolean isManualSetup = getIntent().getBooleanExtra(EXTRA_MANUAL_SETUP, false);
        PanckGenerateAccountKeysTask accountGenerationTask = new PanckGenerateAccountKeysTask(this, mAccount);
        launchGenerateAccountKeysTask(accountGenerationTask, isManualSetup);
    }

    private void loadConfigurations() {
        configurationManager.loadConfigurationsBlocking(new ProvisioningScope.SingleAccountSettings(mAccount.getEmail()));
    }

    @VisibleForTesting
    public void launchGenerateAccountKeysTask(PanckGenerateAccountKeysTask accountGenerationTask, boolean manualSetup) {
        nonConfigurationInstance = accountGenerationTask;
        accountGenerationTask.execute(manualSetup);
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.done:
            onNext();
            break;
        }
    }

    @VisibleForTesting
    public static class PanckGenerateAccountKeysTask extends ExtendedAsyncTask<Boolean, Void, Void> {
        Account account;

        @VisibleForTesting public AccountKeysGenerator accountKeysGenerator = new AccountKeysGenerator() {
            @Override
            public void generateAccountKeys() {
                PlanckUtils.pEpGenerateAccountKeys(mContext, account);
                K9.setServicesEnabled(mContext);
            }

            @Override
            public void onAccountKeysGenerationFinished() {
                if ((mProgressDialog != null) && mProgressDialog.isShowing()
                        && (mActivity != null) && !mActivity.isDestroyed()){
                    mProgressDialog.dismiss();
                }
                SettingsActivity.Companion.listAccountsOnStartup(mContext);
            }
        };

        protected PanckGenerateAccountKeysTask(Activity activity, Account account) {
            super(activity);
            this.account = account;
        }

        @Override
        public void showProgressDialog() {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage(mContext.getString(R.string.pep_account_setup_generating_keys));
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            accountKeysGenerator.onAccountKeysGenerationFinished();
        }

        @Override
        public Void doInBackground(Boolean... params) {
            account.setSetupState(Account.SetupState.READY);
            boolean manualSetup = params[0];
            if(manualSetup) {
                account.setOptionsOnInstall();
            }
            if (((K9) mContext.getApplicationContext()).isRunningOnWorkProfile()) {
                ((AccountSetupNames) mActivity).loadConfigurations();
            } else {
                account.save(Preferences.getPreferences(mActivity));
            }
            MessagingController.getInstance(mActivity).refreshRemoteSynchronous(account);
            accountKeysGenerator.generateAccountKeys();
            return null;
        }
    }

    interface AccountKeysGenerator {
        void generateAccountKeys();
        void onAccountKeysGenerationFinished();
    }
}
