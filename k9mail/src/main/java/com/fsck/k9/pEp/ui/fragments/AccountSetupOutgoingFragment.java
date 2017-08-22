package com.fsck.k9.pEp.ui.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.ContentLoadingProgressBar;
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

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.AccountSetupOptions;
import com.fsck.k9.activity.setup.AuthTypeAdapter;
import com.fsck.k9.activity.setup.AuthTypeHolder;
import com.fsck.k9.activity.setup.ConnectionSecurityAdapter;
import com.fsck.k9.activity.setup.ConnectionSecurityHolder;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.filter.Hex;
import com.fsck.k9.pEp.ui.infrastructure.exceptions.PEpCertificateException;
import com.fsck.k9.pEp.ui.infrastructure.exceptions.PEpSetupException;
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.view.ClientCertificateSpinner;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

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
        rootView = inflater.inflate(R.layout.fragment_account_setup_outgoing, container, false);

        ((K9Activity) getActivity()).initializeToolbar(true, R.string.account_setup_outgoing_title);
        ((K9Activity) getActivity()).setStatusBarPepColor(getResources().getColor(R.color.pep_green));

        String accountUuid = getArguments().getString(EXTRA_ACCOUNT);
        mEdit = getArguments().getBoolean(EXTRA_EDIT);
        mAccount = Preferences.getPreferences(getActivity()).getAccount(accountUuid);
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
        mAccount = Preferences.getPreferences(getActivity()).getAccount(accountUuid);
        mMakeDefault = getArguments().getBoolean(EXTRA_MAKE_DEFAULT, false);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = Preferences.getPreferences(getActivity()).getAccount(accountUuid);
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
        return rootView;
    }

    @Override
    protected void inject() {
        getpEpComponent().inject(this);
    }

    private void checkSettings() {
        pEpSettingsChecker.checkSettings(mAccount, AccountSetupCheckSettings.CheckDirection.OUTGOING, mMakeDefault, AccountSetupCheckSettingsFragment.OUTGOING,
                false,
                new PEpSettingsChecker.ResultCallback<PEpSettingsChecker.Redirection>() {
                    @Override
                    public void onError(PEpSetupException exception) {
                        handleErrorCheckingSettings(exception);
                    }

                    @Override
                    public void onLoaded(PEpSettingsChecker.Redirection redirection) {
                        goForward();
                    }
                });
    }

    private void enableViewGroup(boolean enable, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                enableViewGroup(enable, ((ViewGroup) child));
            } else {
                child.setEnabled(enable);
            }
        }

    }

    private void handleErrorCheckingSettings(PEpSetupException exception) {
        if (exception.isCertificateAcceptanceNeeded()) {
            handleCertificateValidationException(exception);
        } else {
            showErrorDialog(
                    exception.getTitleResource(),
                    exception.getMessage() == null ? "" : exception.getMessage());
        }
        nextProgressBar.hide();
        mNextButton.setVisibility(View.VISIBLE);
        enableViewGroup(true, (ViewGroup) rootView);
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
                    boolean isAuthExternal = (AuthType.EXTERNAL == getSelectedAuthType());
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
                if (AuthType.EXTERNAL == selection) {

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
        outState.putString(EXTRA_ACCOUNT, mAccount.getUuid());
        outState.putInt(STATE_SECURITY_TYPE_POSITION, mCurrentSecurityTypeViewPosition);
        outState.putInt(STATE_AUTH_TYPE_POSITION, mCurrentAuthTypeViewPosition);
    }

    /**
     * Shows/hides password field and client certificate spinner
     */
    private void updateViewFromAuthType() {
        AuthType authType = getSelectedAuthType();
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);
        boolean isAuthTypeXOAuth2 = (AuthType.XOAUTH2 == authType);

        if (isAuthTypeExternal) {

            // hide password fields, show client certificate fields
            mPasswordView.setVisibility(View.GONE);
            mPasswordLabelView.setVisibility(View.GONE);
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
        boolean isAuthTypeExternal = (AuthType.EXTERNAL == authType);
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
            isAuthTypeExternal = (AuthType.EXTERNAL == authType);

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

        boolean hasValidXOAuth2Settings = hasValidUserName
                && isAuthTypeXOAuth2;

        mNextButton
                .setEnabled(Utility.domainFieldValid(mServerView)
                        && Utility.requiredFieldValid(mPortView)
                        && (!mRequireLoginView.isChecked()
                        || hasValidPasswordSettings
                        || hasValidExternalAuthSettings
                        || hasValidXOAuth2Settings));
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
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
                mAccount.save(Preferences.getPreferences(getActivity()));
                goForward();
            } else {
                AccountSetupOptions.actionOptions(getActivity(), mAccount, mMakeDefault);
                goForward();
            }
        }
    }

    private void goForward() {
        if (mEdit) {
            getActivity().finish();
        }
        accountSetupNavigator.goForward(getFragmentManager(), mAccount, false);
    }

    protected void onNext() {
        nextProgressBar.show();
        mNextButton.setVisibility(View.GONE);
        rootView.setEnabled(false);
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

    private void handleCertificateValidationException(PEpSetupException cve) {
        PEpCertificateException certificateException = (PEpCertificateException) cve;
        Log.e(K9.LOG_TAG, "Error while testing settings (cve)", certificateException.getOriginalException());

        // Avoid NullPointerException in acceptKeyDialog()
        if (certificateException.hasCertChain()) {
            acceptKeyDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    certificateException.getOriginalException());
        } else {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_server_message_fmt,
                    errorMessageForCertificateException(certificateException.getOriginalException()));
        }
    }

    private void showErrorDialog(int stringResource, String message) {
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(stringResource))
                .setMessage(message)
                .show();
    }

    private void handleCertificateValidationException(CertificateValidationException cve) {
        Log.e(K9.LOG_TAG, "Error while testing settings (cve)", cve);

        X509Certificate[] chain = cve.getCertChain();
        // Avoid NullPointerException in acceptKeyDialog()
        if (chain != null) {
            acceptKeyDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    cve);
        } else {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_server_message_fmt,
                    errorMessageForCertificateException(cve));
        }
    }

    private String errorMessageForCertificateException(CertificateValidationException e) {
        switch (e.getReason()) {
            case Expired: return getString(R.string.client_certificate_expired, e.getAlias(), e.getMessage());
            case MissingCapability: return getString(R.string.auth_external_error);
            case RetrievalFailure: return getString(R.string.client_certificate_retrieval_failure, e.getAlias());
            case UseMessage: return e.getMessage();
            case Unknown:
            default: return "";
        }
    }

    private void acceptKeyDialog(final int msgResId, final CertificateValidationException ex) {
        Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
                String exMessage = "Unknown Error";

                if (ex != null) {
                    if (ex.getCause() != null) {
                        if (ex.getCause().getCause() != null) {
                            exMessage = ex.getCause().getCause().getMessage();

                        } else {
                            exMessage = ex.getCause().getMessage();
                        }
                    } else {
                        exMessage = ex.getMessage();
                    }
                }

                StringBuilder chainInfo = new StringBuilder(100);
                MessageDigest sha1 = null;
                try {
                    sha1 = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e) {
                    Log.e(K9.LOG_TAG, "Error while initializing MessageDigest", e);
                }

                final X509Certificate[] chain = ex.getCertChain();
                // We already know chain != null (tested before calling this method)
                for (int i = 0; i < chain.length; i++) {
                    // display certificate chain information
                    //TODO: localize this strings
                    chainInfo.append("Certificate chain[").append(i).append("]:\n");
                    chainInfo.append("Subject: ").append(chain[i].getSubjectDN().toString()).append("\n");

                    // display SubjectAltNames too
                    // (the user may be mislead into mistrusting a certificate
                    //  by a subjectDN not matching the server even though a
                    //  SubjectAltName matches)
                    try {
                        final Collection< List<? >> subjectAlternativeNames = chain[i].getSubjectAlternativeNames();
                        if (subjectAlternativeNames != null) {
                            // The list of SubjectAltNames may be very long
                            //TODO: localize this string
                            StringBuilder altNamesText = new StringBuilder();
                            altNamesText.append("Subject has ").append(subjectAlternativeNames.size()).append(" alternative names\n");

                            // we need these for matching
                            String storeURIHost = (Uri.parse(mAccount.getStoreUri())).getHost();
                            String transportURIHost = (Uri.parse(mAccount.getTransportUri())).getHost();

                            for (List<?> subjectAlternativeName : subjectAlternativeNames) {
                                Integer type = (Integer)subjectAlternativeName.get(0);
                                Object value = subjectAlternativeName.get(1);
                                String name;
                                switch (type.intValue()) {
                                    case 0:
                                        Log.w(K9.LOG_TAG, "SubjectAltName of type OtherName not supported.");
                                        continue;
                                    case 1: // RFC822Name
                                        name = (String)value;
                                        break;
                                    case 2:  // DNSName
                                        name = (String)value;
                                        break;
                                    case 3:
                                        Log.w(K9.LOG_TAG, "unsupported SubjectAltName of type x400Address");
                                        continue;
                                    case 4:
                                        Log.w(K9.LOG_TAG, "unsupported SubjectAltName of type directoryName");
                                        continue;
                                    case 5:
                                        Log.w(K9.LOG_TAG, "unsupported SubjectAltName of type ediPartyName");
                                        continue;
                                    case 6:  // Uri
                                        name = (String)value;
                                        break;
                                    case 7: // ip-address
                                        name = (String)value;
                                        break;
                                    default:
                                        Log.w(K9.LOG_TAG, "unsupported SubjectAltName of unknown type");
                                        continue;
                                }

                                // if some of the SubjectAltNames match the store or transport -host,
                                // display them
                                if (name.equalsIgnoreCase(storeURIHost) || name.equalsIgnoreCase(transportURIHost)) {
                                    //TODO: localize this string
                                    altNamesText.append("Subject(alt): ").append(name).append(",...\n");
                                } else if (name.startsWith("*.") && (
                                        storeURIHost.endsWith(name.substring(2)) ||
                                                transportURIHost.endsWith(name.substring(2)))) {
                                    //TODO: localize this string
                                    altNamesText.append("Subject(alt): ").append(name).append(",...\n");
                                }
                            }
                            chainInfo.append(altNamesText);
                        }
                    } catch (Exception e1) {
                        // don't fail just because of subjectAltNames
                        Log.w(K9.LOG_TAG, "cannot display SubjectAltNames in dialog", e1);
                    }

                    chainInfo.append("Issuer: ").append(chain[i].getIssuerDN().toString()).append("\n");
                    if (sha1 != null) {
                        sha1.reset();
                        try {
                            String sha1sum = Hex.encodeHex(sha1.digest(chain[i].getEncoded()));
                            chainInfo.append("Fingerprint (SHA-1): ").append(sha1sum).append("\n");
                        } catch (CertificateEncodingException e) {
                            Log.e(K9.LOG_TAG, "Error while encoding certificate", e);
                        }
                    }
                }

                // TODO: refactor with DialogFragment.
                // This is difficult because we need to pass through chain[0] for onClick()
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.account_setup_failed_dlg_invalid_certificate_title))
                        //.setMessage(getString(R.string.account_setup_failed_dlg_invalid_certificate)
                        .setMessage(getString(msgResId, exMessage)
                                + " " + chainInfo.toString()
                        )
                        .setCancelable(true)
                        .setPositiveButton(
                                getString(R.string.account_setup_failed_dlg_invalid_certificate_accept),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        acceptCertificate(chain[0]);
                                    }
                                })
                        .setNegativeButton(
                                getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        getFragmentManager().popBackStack();
                                    }
                                })
                        .show();
            }
        });
    }

    private void acceptCertificate(X509Certificate certificate) {
        try {
            mAccount.addCertificate(AccountSetupCheckSettings.CheckDirection.OUTGOING, certificate);
        } catch (CertificateException e) {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    e.getMessage() == null ? "" : e.getMessage());
        }
        goForward();
    }
}
