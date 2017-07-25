package com.fsck.k9.pEp.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.AccountSetupNames;
import com.fsck.k9.activity.setup.SpinnerOption;
import com.fsck.k9.mail.Store;
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator;

import timber.log.Timber;

/**
 * Created by arturo on 7/14/17.
 */

public class AccountSetupOptionsFragment extends PEpFragment {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private Spinner mCheckFrequencyView;

    private Spinner mDisplayCountView;


    private CheckBox mNotifyView;
    private CheckBox mNotifySyncView;
    private CheckBox mPushEnable;
    private CheckBox mUntrustedServer;

    private Account mAccount;
    private View rootView;
    private AccountSetupNavigator accountSetupNavigator;

    public static AccountSetupOptionsFragment actionOptions(Account account, boolean makeDefault) {
        AccountSetupOptionsFragment fragment = new AccountSetupOptionsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, account.getUuid());
        bundle.putBoolean(EXTRA_MAKE_DEFAULT, makeDefault);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.account_setup_options, container, false);

        ((K9Activity) getActivity()).initializeToolbar(true, R.string.account_settings_title_fmt);

        mCheckFrequencyView = (Spinner)rootView.findViewById(R.id.account_check_frequency);
        mDisplayCountView = (Spinner)rootView.findViewById(R.id.account_display_count);
        mNotifyView = (CheckBox)rootView.findViewById(R.id.account_notify);
        mNotifySyncView = (CheckBox)rootView.findViewById(R.id.account_notify_sync);
        mPushEnable = (CheckBox)rootView.findViewById(R.id.account_enable_push);

        mUntrustedServer = (CheckBox) rootView.findViewById(R.id.account_trust_server);
        rootView.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDone();
            }
        });

        SpinnerOption checkFrequencies[] = {
                new SpinnerOption(-1,
                        getString(R.string.account_setup_options_mail_check_frequency_never)),
                new SpinnerOption(1,
                        getString(R.string.account_setup_options_mail_check_frequency_1min)),
                new SpinnerOption(5,
                        getString(R.string.account_setup_options_mail_check_frequency_5min)),
                new SpinnerOption(10,
                        getString(R.string.account_setup_options_mail_check_frequency_10min)),
                new SpinnerOption(15,
                        getString(R.string.account_setup_options_mail_check_frequency_15min)),
                new SpinnerOption(30,
                        getString(R.string.account_setup_options_mail_check_frequency_30min)),
                new SpinnerOption(60,
                        getString(R.string.account_setup_options_mail_check_frequency_1hour)),
                new SpinnerOption(120,
                        getString(R.string.account_setup_options_mail_check_frequency_2hour)),
                new SpinnerOption(180,
                        getString(R.string.account_setup_options_mail_check_frequency_3hour)),
                new SpinnerOption(360,
                        getString(R.string.account_setup_options_mail_check_frequency_6hour)),
                new SpinnerOption(720,
                        getString(R.string.account_setup_options_mail_check_frequency_12hour)),
                new SpinnerOption(1440,
                        getString(R.string.account_setup_options_mail_check_frequency_24hour)),

        };

        ArrayAdapter<SpinnerOption> checkFrequenciesAdapter = new ArrayAdapter<SpinnerOption>(getActivity(),
                android.R.layout.simple_spinner_item, checkFrequencies);
        checkFrequenciesAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCheckFrequencyView.setAdapter(checkFrequenciesAdapter);

        SpinnerOption displayCounts[] = {
                new SpinnerOption(10, getString(R.string.account_setup_options_mail_display_count_10)),
                new SpinnerOption(25, getString(R.string.account_setup_options_mail_display_count_25)),
                new SpinnerOption(50, getString(R.string.account_setup_options_mail_display_count_50)),
                new SpinnerOption(100, getString(R.string.account_setup_options_mail_display_count_100)),
                new SpinnerOption(250, getString(R.string.account_setup_options_mail_display_count_250)),
                new SpinnerOption(500, getString(R.string.account_setup_options_mail_display_count_500)),
                new SpinnerOption(1000, getString(R.string.account_setup_options_mail_display_count_1000)),
        };

        ArrayAdapter<SpinnerOption> displayCountsAdapter = new ArrayAdapter<SpinnerOption>(getActivity(),
                android.R.layout.simple_spinner_item, displayCounts);
        displayCountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDisplayCountView.setAdapter(displayCountsAdapter);

        String accountUuid = getArguments().getString(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(getActivity()).getAccount(accountUuid);

        mNotifyView.setChecked(mAccount.isNotifyNewMail());
        mNotifySyncView.setChecked(mAccount.isShowOngoing());
        SpinnerOption.setSpinnerOptionValue(mCheckFrequencyView, mAccount
                .getAutomaticCheckIntervalMinutes());
        SpinnerOption.setSpinnerOptionValue(mDisplayCountView, mAccount
                .getDisplayCount());


        boolean isPushCapable = false;
        try {
            Store store = mAccount.getRemoteStore();
            isPushCapable = store.isPushCapable();
        } catch (Exception e) {
            Timber.e(e, "Could not get remote store");
        }


        if (!isPushCapable) {
            mPushEnable.setVisibility(View.GONE);
        } else {
            mPushEnable.setChecked(true);
        }
        return rootView;
    }

    @Override
    protected void inject() {
        getpEpComponent().inject(this);
    }

    private void onDone() {
        mAccount.setDescription(mAccount.getEmail());
        mAccount.setNotifyNewMail(mNotifyView.isChecked());
        mAccount.setShowOngoing(mNotifySyncView.isChecked());
        mAccount.setAutomaticCheckIntervalMinutes((Integer)((SpinnerOption)mCheckFrequencyView
                .getSelectedItem()).value);
        mAccount.setDisplayCount((Integer)((SpinnerOption)mDisplayCountView
                .getSelectedItem()).value);

        if (mPushEnable.isChecked()) {
            mAccount.setFolderPushMode(Account.FolderMode.FIRST_CLASS);
        } else {
            mAccount.setFolderPushMode(Account.FolderMode.NONE);
        }

        mAccount.setPEpStoreEncryptedOnServer(!mUntrustedServer.isChecked());

        mAccount.save(Preferences.getPreferences(getActivity()));
        if (mAccount.equals(Preferences.getPreferences(getActivity()).getDefaultAccount()) ||
                getArguments().getBoolean(EXTRA_MAKE_DEFAULT, false)) {
            Preferences.getPreferences(getActivity()).setDefaultAccount(mAccount);
        }
        K9.setServicesEnabled(getActivity());
        AccountSetupNames.actionSetNames(getActivity(), mAccount);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                onDone();
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        accountSetupNavigator = ((AccountSetupBasics) getActivity()).getAccountSetupNavigator();
        accountSetupNavigator.setCurrentStep(AccountSetupNavigator.Step.OPTIONS, mAccount);
    }
}
