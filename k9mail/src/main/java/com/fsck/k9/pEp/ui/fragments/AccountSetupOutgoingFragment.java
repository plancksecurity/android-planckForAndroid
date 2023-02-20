package com.fsck.k9.pEp.ui.fragments;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.widget.ContentLoadingProgressBar;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.AuthTypeAdapter;
import com.fsck.k9.activity.setup.AuthTypeHolder;
import com.fsck.k9.activity.setup.ConnectionSecurityAdapter;
import com.fsck.k9.activity.setup.ConnectionSecurityHolder;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.view.ClientCertificateSpinner;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

public class AccountSetupOutgoingFragment extends PEpFragment {

    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";
    private static final String STATE_SECURITY_TYPE_POSITION = "stateSecurityTypePosition";
    private static final String STATE_AUTH_TYPE_POSITION = "authTypePosition";
    private static final String EXTRA_EDIT = "edit";

    private EditText mUsernameView;
    private EditText mPasswordView;
    private ClientCertificateSpinner mClientCertificateSpinner;
    private TextView mClientCertificateLabelView;
    private TextView mPasswordLabelView;
    private EditText mServerView;
    private EditText mPortView;
    private String mCurrentPortViewSetting;
    private CheckBox mRequireLoginView;
    private ViewGroup mRequireLoginSettingsView;
    private Spinner mSecurityTypeView;
    private int mCurrentSecurityTypeViewPosition;
    private Spinner mAuthTypeView;
    private int mCurrentAuthTypeViewPosition;
    private AuthTypeAdapter mAuthTypeAdapter;
    private Button mNextButton;
    private Account mAccount;
    private boolean mMakeDefault;

    private View rootView;
    private boolean mEdit;

    @Inject PEpSettingsChecker pEpSettingsChecker;
    @Inject Preferences preferences;
    
    private ContentLoadingProgressBar nextProgressBar;
    private AccountSetupNavigator accountSetupNavigator;

    public static AccountSetupOutgoingFragment actionOutgoingSettings(Account account, boolean makeDefault) {
        AccountSetupOutgoingFragment fragment = new AccountSetupOutgoingFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, account.getUuid());
        bundle.putBoolean(EXTRA_MAKE_DEFAULT, makeDefault);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static AccountSetupOutgoingFragment intentActionEditOutgoingSettings(Account account) {
        AccountSetupOutgoingFragment fragment = new AccountSetupOutgoingFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, account.getUuid());
        bundle.putBoolean(EXTRA_EDIT, true);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static AccountSetupOutgoingFragment intentActionEditOutgoingSettings(String accountUuid) {
        AccountSetupOutgoingFragment fragment = new AccountSetupOutgoingFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, accountUuid);
        bundle.putBoolean(EXTRA_EDIT, true);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setupPEpFragmentToolbar();
        rootView = inflater.inflate(R.layout.fragment_account_setup_outgoing, container, false);

        ((K9Activity) getActivity()).initializeToolbar(true, R.string.account_setup_outgoing_title);

        String accountUuid = getArguments().getString(EXTRA_ACCOUNT);
        mEdit = getArguments().getBoolean(EXTRA_EDIT);
        mAccount = getAccountFromPreferences(accountUuid);
        try {
            if (new URI(mAccount.getStoreUri()).getScheme().startsWith("webdav")) {
                mAccount.setTransportUri(mAccount.getStoreUri());
                checkSettings();
            }
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        mUsernameView = (EditText)rootView.findViewById(R.id.account_username);
        mPasswordView = (EditText)rootView.findViewById(R.id.account_password);
        mClientCertificateSpinner = (ClientCertificateSpinner)rootView.findViewById(R.id.account_client_certificate_spinner);
        mClientCertificateLabelView = (TextView)rootView.findViewById(R.id.account_client_certificate_label);
        mPasswordLabelView = (TextView)rootView.findViewById(R.id.account_password_label);
        mServerView = (EditText)rootView.findViewById(R.id.account_server);
        mPortView = (EditText)rootView.findViewById(R.id.account_port);
        mRequireLoginView = (CheckBox)rootView.findViewById(R.id.account_require_login);
        mRequireLoginSettingsView = (ViewGroup)rootView.findViewById(R.id.account_require_login_settings);
        mSecurityTypeView = (Spinner)rootView.findViewById(R.id.account_security_type);
        mAuthTypeView = (Spinner)rootView.findViewById(R.id.account_auth_type);
        mNextButton = (Button)rootView.findViewById(R.id.next);
        nextProgressBar = (ContentLoadingProgressBar) rootView.findViewById(R.id.next_progressbar);

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNext();
            }
        });

        mSecurityTypeView.setAdapter(ConnectionSecurityAdapter.get(getActivity()));

        mAuthTypeAdapter = AuthTypeAdapter.get(getActivity());
        mAuthTypeView.setAdapter(mAuthTypeAdapter);

        /*
         * Only allow digits in the port field.
         */
        mPortView.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        //FIXME: get Account object again?
        accountUuid = getArguments().getString(EXTRA_ACCOUNT);
        mAccount = getAccountFromPreferences(accountUuid);
        mMakeDefault = getArguments().getBoolean(EXTRA_MAKE_DEFAULT, false);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = getAccountFromPreferences(accountUuid);
        }

        try {
            ServerSettings settings = Transport.decodeTransportUri(mAccount.getTransportUri());

            updateAuthPlainTextFromSecurityType(settings.connectionSecurity);

            if (savedInstanceState == null) {
                // The first item is selected if settings.authenticationType is null or is not in mAuthTypeAdapter
                mCurrentAuthTypeViewPosition = mAuthTypeAdapter.getAuthPosition(settings.authenticationType);
            } else {
                mCurrentAuthTypeViewPosition = savedInstanceState.getInt(STATE_AUTH_TYPE_POSITION);
            }
            mAuthTypeView.setSelection(mCurrentAuthTypeViewPosition, false);
            updateViewFromAuthType();

            // Select currently configured security type
            if (savedInstanceState == null) {
                mCurrentSecurityTypeViewPosition = settings.connectionSecurity.ordinal();
            } else {

                /*
                 * Restore the spinner state now, before calling
                 * setOnItemSelectedListener(), thus avoiding a call to
                 * onItemSelected(). Then, when the system restores the state
                 * (again) in onRestoreInstanceState(), The system will see that
                 * the new state is the same as the current state (set here), so
                 * once again onItemSelected() will not be called.
                 */
                mCurrentSecurityTypeViewPosition = savedInstanceState.getInt(STATE_SECURITY_TYPE_POSITION);
            }
            mSecurityTypeView.setSelection(mCurrentSecurityTypeViewPosition, false);

            if (settings.username != null && !settings.username.isEmpty()) {
                mUsernameView.setText(settings.username);
                mRequireLoginView.setChecked(true);
                mRequireLoginSettingsView.setVisibility(View.VISIBLE);
            }

            if (settings.password != null) {
                mPasswordView.setText(settings.password);
            }

            if (settings.clientCertificateAlias != null) {
                mClientCertificateSpinner.setAlias(settings.clientCertificateAlias);
            }

            if (settings.host != null) {
                mServerView.setText(settings.host);
            }

            if (settings.port != -1) {
                mPortView.setText(String.format("%d", settings.port));
            } else {
                updatePortFromSecurityType();
            }
            mCurrentPortViewSetting = mPortView.getText().toString();
        } catch (Exception e) {
            /*
             * We should always be able to parse our own settings.
             */
            failure(e);
        }

        initializeViewListeners();
        validateFields();
        if (mEdit) {
            mNextButton.setText(R.string.done_action);
        }
        return rootView;
    }

    private Account getAccountFromPreferences(String accountUuid) {
         return mEdit
                ? preferences.getAccount(accountUuid)
                : preferences.getAccountAllowingIncomplete(accountUuid);
    }

    @Override
    protected void inject() {
        getpEpComponent().inject(this);
    }

    private void checkSettings() {
        AccountSetupCheckSettings.actionCheckSettings(
                requireActivity(), mAccount, AccountSetupCheckSettings.CheckDirection.OUTGOING);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mRequireLoginView.isChecked()) {
            mRequireLoginSettingsView.setVisibility(View.VISIBLE);
        } else {
            mRequireLoginSettingsView.setVisibility(View.GONE);
        }
    }

    /**
     * Called at the end of either {@code onCreate()} or
     * {@code onRestoreInstanceState()}, after the views have been initialized,
     * so that the listeners are not triggered during the view initialization.
     * This avoids needless calls to {@code validateFields()} which is called
     * immediately after this is called.
     */
    private void initializeViewListeners() {

        /*
         * Updates the port when the user changes the security type. This allows
         * us to show a reasonable default which the user can change.
         */
        mSecurityTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position,
                                       long id) {

                /*
                 * We keep our own record of the spinner state so we
                 * know for sure that onItemSelected() was called
                 * because of user input, not because of spinner
                 * state initialization. This assures that the port
                 * will not be replaced with a default value except
                 * on user input.
                 */
                if (mCurrentSecurityTypeViewPosition != position) {
                    updatePortFromSecurityType();

                    boolean isInsecure = (ConnectionSecurity.NONE == getSelectedSecurity());
                    boolean isAuthExternal = getSelectedAuthType().isExternalAuth();
                    boolean loginNotRequired = !mRequireLoginView.isChecked();

                    /*
                     * If the user selects ConnectionSecurity.NONE, a
                     * warning would normally pop up if the authentication
                     * is AuthType.EXTERNAL (i.e., using client
                     * certificates). But such a warning is irrelevant if
                     * login is not required. So to avoid such a warning
                     * (generated in validateFields()) under those
                     * conditions, we change the (irrelevant) authentication
                     * method to PLAIN.
                     */
                    if (isInsecure && isAuthExternal && loginNotRequired) {
                        AdapterView.OnItemSelectedListener onItemSelectedListener = mAuthTypeView.getOnItemSelectedListener();
                        mAuthTypeView.setOnItemSelectedListener(null);
                        mCurrentAuthTypeViewPosition = mAuthTypeAdapter.getAuthPosition(AuthType.PLAIN);
                        mAuthTypeView.setSelection(mCurrentAuthTypeViewPosition, false);
                        mAuthTypeView.setOnItemSelectedListener(onItemSelectedListener);
                        updateViewFromAuthType();
                    }

                    validateFields();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* unused */ }
        });

        mAuthTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position,
                                       long id) {
                if (mCurrentAuthTypeViewPosition == position) {
                    return;
                }

                updateViewFromAuthType();
                validateFields();
                AuthType selection = getSelectedAuthType();

                // Have the user select (or confirm) the client certificate
                if (selection.isExternalAuth()) {

                    // This may again invoke validateFields()
                    mClientCertificateSpinner.chooseCertificate();
                } else {
                    mPasswordView.requestFocus();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* unused */ }
        });

        mRequireLoginView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mRequireLoginSettingsView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                validateFields();
            }
        });
        mClientCertificateSpinner.setOnClientCertificateChangedListener(clientCertificateChangedListener);
        mUsernameView.addTextChangedListener(validationTextWatcher);
        mPasswordView.addTextChangedListener(validationTextWatcher);
        mServerView.addTextChangedListener(validationTextWatcher);
        mPortView.addTextChangedListener(validationTextWatcher);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mAccount != null) {
            outState.putString(EXTRA_ACCOUNT, mAccount.getUuid());
        }
        outState.putInt(STATE_SECURITY_TYPE_POSITION, mCurrentSecurityTypeViewPosition);
        outState.putInt(STATE_AUTH_TYPE_POSITION, mCurrentAuthTypeViewPosition);
    }

    /**
     * Shows/hides password field and client certificate spinner
     */
    private void updateViewFromAuthType() {
        AuthType authType = getSelectedAuthType();
        boolean isAuthTypeExternal = authType.isExternalAuth();
        boolean isAuthTypeXOAuth2 = (AuthType.XOAUTH2 == authType);

        if (isAuthTypeExternal) {

            // hide password fields, show client certificate fields
            if (AuthType.EXTERNAL_PLAIN != authType) {
                mPasswordView.setVisibility(View.GONE);
                mPasswordLabelView.setVisibility(View.GONE);
            }
            mClientCertificateLabelView.setVisibility(View.VISIBLE);
            mClientCertificateSpinner.setVisibility(View.VISIBLE);
        } else if (isAuthTypeXOAuth2) {
            // hide password fields, hide client certificate fields
            mPasswordView.setVisibility(View.GONE);
            mPasswordLabelView.setVisibility(View.GONE);
            mClientCertificateLabelView.setVisibility(View.GONE);
            mClientCertificateSpinner.setVisibility(View.GONE);
        } else {
            // show password fields, hide client certificate fields
            mPasswordView.setVisibility(View.VISIBLE);
            mPasswordLabelView.setVisibility(View.VISIBLE);
            mClientCertificateLabelView.setVisibility(View.GONE);
            mClientCertificateSpinner.setVisibility(View.GONE);
        }
    }

    /**
     * This is invoked only when the user makes changes to a widget, not when
     * widgets are changed programmatically.  (The logic is simpler when you know
     * that this is the last thing called after an input change.)
     */
    private void validateFields() {
        AuthType authType = getSelectedAuthType();
        boolean isAuthTypeExternal = authType.isExternalAuth();
        boolean isAuthTypeXOAuth2 = (AuthType.XOAUTH2 == authType);

        ConnectionSecurity connectionSecurity = getSelectedSecurity();
        boolean hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);

        if (isAuthTypeExternal && !hasConnectionSecurity) {

            // Notify user of an invalid combination of AuthType.EXTERNAL & ConnectionSecurity.NONE
            String toastText = getString(R.string.account_setup_outgoing_invalid_setting_combo_notice,
                    getString(R.string.account_setup_incoming_auth_type_label),
                    AuthType.EXTERNAL.toString(),
                    getString(R.string.account_setup_incoming_security_label),
                    ConnectionSecurity.NONE.toString());
            FeedbackTools.showLongFeedback(rootView, toastText);

            // Reset the views back to their previous settings without recursing through here again
            AdapterView.OnItemSelectedListener onItemSelectedListener = mAuthTypeView.getOnItemSelectedListener();
            mAuthTypeView.setOnItemSelectedListener(null);
            mAuthTypeView.setSelection(mCurrentAuthTypeViewPosition, false);
            mAuthTypeView.setOnItemSelectedListener(onItemSelectedListener);
            updateViewFromAuthType();

            onItemSelectedListener = mSecurityTypeView.getOnItemSelectedListener();
            mSecurityTypeView.setOnItemSelectedListener(null);
            mSecurityTypeView.setSelection(mCurrentSecurityTypeViewPosition, false);
            mSecurityTypeView.setOnItemSelectedListener(onItemSelectedListener);
            updateAuthPlainTextFromSecurityType(getSelectedSecurity());

            mPortView.removeTextChangedListener(validationTextWatcher);
            mPortView.setText(mCurrentPortViewSetting);
            mPortView.addTextChangedListener(validationTextWatcher);

            authType = getSelectedAuthType();
            isAuthTypeExternal = authType.isExternalAuth();

            connectionSecurity = getSelectedSecurity();
            hasConnectionSecurity = (connectionSecurity != ConnectionSecurity.NONE);
        } else {
            mCurrentAuthTypeViewPosition = mAuthTypeView.getSelectedItemPosition();
            mCurrentSecurityTypeViewPosition = mSecurityTypeView.getSelectedItemPosition();
            mCurrentPortViewSetting = mPortView.getText().toString();
        }

        boolean hasValidCertificateAlias = mClientCertificateSpinner.getAlias() != null;
        boolean hasValidUserName = Utility.requiredFieldValid(mUsernameView);

        boolean hasValidPasswordSettings = hasValidUserName
                && !isAuthTypeExternal
                && Utility.requiredFieldValid(mPasswordView);

        boolean hasValidExternalAuthSettings = hasValidUserName
                && isAuthTypeExternal
                && hasConnectionSecurity
                && hasValidCertificateAlias;

        if (authType == AuthType.EXTERNAL_PLAIN) {
            hasValidExternalAuthSettings = hasValidExternalAuthSettings
                    && Utility.requiredFieldValid(mPasswordView);
        }

        boolean hasValidXOAuth2Settings = hasValidUserName
                && isAuthTypeXOAuth2;

        boolean hasValidPort = validatePortEditText();

        mNextButton
                .setEnabled(Utility.domainFieldValid(mServerView)
                        && hasValidPort
                        && (!mRequireLoginView.isChecked()
                        || hasValidPasswordSettings
                        || hasValidExternalAuthSettings
                        || hasValidXOAuth2Settings));
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    private boolean validatePortEditText() {
        boolean isNotEmpty = Utility.requiredFieldValid(mPortView) ;
        boolean isWithinRange = isNotEmpty && Integer.parseInt(mPortView.getText().toString()) < 65354;
        if (!isNotEmpty) {
            mPortView.setError(getString(R.string.port_is_empty));
        }else if(!isWithinRange){
            mPortView.setError(getString(R.string.port_is_out_of_range));
        }
        return isWithinRange;
    }

    private void updatePortFromSecurityType() {
        ConnectionSecurity securityType = getSelectedSecurity();
        updateAuthPlainTextFromSecurityType(securityType);

        // Remove listener so as not to trigger validateFields() which is called
        // elsewhere as a result of user interaction.
        mPortView.removeTextChangedListener(validationTextWatcher);
        mPortView.setText(String.valueOf(AccountCreator.getDefaultPort(securityType, ServerSettings.Type.SMTP)));
        mPortView.addTextChangedListener(validationTextWatcher);
    }

    private void updateAuthPlainTextFromSecurityType(ConnectionSecurity securityType) {
        mAuthTypeAdapter.useInsecureText(securityType == ConnectionSecurity.NONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (mEdit) {
                mAccount.save(preferences);
                goForward();
            } else if (requestCode == AccountSetupCheckSettings.ACTIVITY_REQUEST_CODE) {
                goForward();
            }
        }
    }

    private void goForward() {
        if (mEdit) {
            if (getActivity() != null)  {
                getActivity().finish();
            }
        } else {
            accountSetupNavigator.goForward(getFragmentManager(), mAccount, false);
        }
    }

    protected void onNext() {
        ConnectionSecurity securityType = getSelectedSecurity();
        String uri;
        String username = null;
        String password = null;
        String clientCertificateAlias = null;
        AuthType authType = null;
        if (mRequireLoginView.isChecked()) {
            username = mUsernameView.getText().toString();
            authType = getSelectedAuthType();
            if (AuthType.EXTERNAL == authType) {
                clientCertificateAlias = mClientCertificateSpinner.getAlias();
            } else if (AuthType.EXTERNAL_PLAIN == authType){
                clientCertificateAlias = mClientCertificateSpinner.getAlias();
                password = mPasswordView.getText().toString();
            } else {
                password = mPasswordView.getText().toString();
            }
        }
        String newHost = mServerView.getText().toString();
        int newPort = Integer.parseInt(mPortView.getText().toString());
        ServerSettings server = new ServerSettings(ServerSettings.Type.SMTP, newHost, newPort, securityType, authType, username, password, clientCertificateAlias);
        uri = Transport.createTransportUri(server);
        mAccount.deleteCertificate(newHost, newPort, AccountSetupCheckSettings.CheckDirection.OUTGOING);
        mAccount.setTransportUri(uri);
        checkSettings();
    }

    private void failure(Exception use) {
        Log.e(K9.LOG_TAG, "Failure", use);
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        FeedbackTools.showLongFeedback(rootView, toastText);
    }

    /*
     * Calls validateFields() which enables or disables the Next button
     * based on the fields' validity.
     */
    TextWatcher validationTextWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            validateFields();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    ClientCertificateSpinner.OnClientCertificateChangedListener clientCertificateChangedListener = new ClientCertificateSpinner.OnClientCertificateChangedListener() {
        @Override
        public void onClientCertificateChanged(String alias) {
            validateFields();
        }
    };

    private AuthType getSelectedAuthType() {
        AuthTypeHolder holder = (AuthTypeHolder) mAuthTypeView.getSelectedItem();
        return holder.authType;
    }

    private ConnectionSecurity getSelectedSecurity() {
        ConnectionSecurityHolder holder = (ConnectionSecurityHolder) mSecurityTypeView.getSelectedItem();
        return holder.connectionSecurity;
    }

    @Override
    public void onResume() {
        super.onResume();
        accountSetupNavigator = ((AccountSetupBasics) getActivity()).getAccountSetupNavigator();
        accountSetupNavigator.setCurrentStep(AccountSetupNavigator.Step.OUTGOING, mAccount);
    }
}
