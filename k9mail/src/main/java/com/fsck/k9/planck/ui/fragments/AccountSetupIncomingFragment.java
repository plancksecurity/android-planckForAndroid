package com.fsck.k9.planck.ui.fragments;

import static android.app.Activity.RESULT_OK;
import static com.fsck.k9.mail.store.imap.ImapStoreSettings.AUTODETECT_NAMESPACE_KEY;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
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
import com.fsck.k9.job.K9JobManager;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.imap.ImapStoreSettings;
import com.fsck.k9.mail.store.webdav.WebDavStoreSettings;
import com.fsck.k9.planck.ui.tools.AccountSetupNavigator;
import com.fsck.k9.planck.ui.tools.FeedbackTools;
import com.fsck.k9.view.ClientCertificateSpinner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class AccountSetupIncomingFragment extends PEpFragment {

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_ACTION = "action";
    private static final String STATE_SECURITY_TYPE_POSITION = "stateSecurityTypePosition";
    private static final String STATE_AUTH_TYPE_POSITION = "authTypePosition";
    private static final String GMAIL_AUTH_TOKEN_TYPE = "oauth2:https://mail.google.com/";

    @Inject PEpSettingsChecker pEpSettingsChecker;

    @Inject Preferences preferences;

    private ServerSettings.Type mStoreType;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private ClientCertificateSpinner mClientCertificateSpinner;
    private TextView mClientCertificateLabelView;
    private TextView mPasswordLabelView;
    private EditText mServerView;
    private EditText mPortView;
    private String mCurrentPortViewSetting;
    private Spinner mSecurityTypeView;
    private int mCurrentSecurityTypeViewPosition;
    private Spinner mAuthTypeView;
    private int mCurrentAuthTypeViewPosition;
    private CheckBox mImapAutoDetectNamespaceView;
    private EditText mImapPathPrefixView;
    private EditText mWebdavPathPrefixView;
    private EditText mWebdavAuthPathView;
    private EditText mWebdavMailboxPathView;
    private Button mNextButton;
    private Account mAccount;
    private CheckBox mCompressionMobile;
    private CheckBox mCompressionWifi;
    private CheckBox mCompressionOther;
    private CheckBox mSubscribedFoldersOnly;
    private AuthTypeAdapter mAuthTypeAdapter;
    private ConnectionSecurity[] mConnectionSecurityChoices = ConnectionSecurity.values();
    private View rootView;
    private AccountSetupNavigator accountSetupNavigator;
    private boolean editSettings;


    private final K9JobManager jobManager = K9.jobManager;

    public static AccountSetupIncomingFragment actionIncomingSettings(Account account) {
        AccountSetupIncomingFragment fragment = new AccountSetupIncomingFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, account.getUuid());
        fragment.setArguments(bundle);
        return fragment;
    }

    public static AccountSetupIncomingFragment actionEditIncomingSettings(Account account) {
        AccountSetupIncomingFragment fragment = new AccountSetupIncomingFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, account.getUuid());
        bundle.putString(EXTRA_ACTION, Intent.ACTION_EDIT);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static AccountSetupIncomingFragment actionEditIncomingSettings(String accountUuid) {
        AccountSetupIncomingFragment fragment = new AccountSetupIncomingFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, accountUuid);
        bundle.putString(EXTRA_ACTION, Intent.ACTION_EDIT);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ((AccountSetupBasics) requireActivity()).configurePasswordFlowScreen();
        setupPEpFragmentToolbar();
        rootView = inflater.inflate(R.layout.fragment_account_setup_incoming, container, false);

        ((K9Activity) getActivity()).initializeToolbar(true, R.string.account_setup_incoming_title);

        mUsernameView = (EditText) rootView.findViewById(R.id.account_username);
        mPasswordView = (EditText) rootView.findViewById(R.id.account_password);
        mClientCertificateSpinner = (ClientCertificateSpinner)rootView.findViewById(R.id.account_client_certificate_spinner);
        mClientCertificateLabelView = (TextView)rootView.findViewById(R.id.account_client_certificate_label);
        mPasswordLabelView = (TextView)rootView.findViewById(R.id.account_password_label);
        TextView serverLabelView = (TextView) rootView.findViewById(R.id.account_server_label);
        mServerView = (EditText)rootView.findViewById(R.id.account_server);
        mPortView = (EditText)rootView.findViewById(R.id.account_port);
        mSecurityTypeView = (Spinner)rootView.findViewById(R.id.account_security_type);
        mAuthTypeView = (Spinner)rootView.findViewById(R.id.account_auth_type);
        mImapAutoDetectNamespaceView = (CheckBox)rootView.findViewById(R.id.imap_autodetect_namespace);
        mImapPathPrefixView = (EditText)rootView.findViewById(R.id.imap_path_prefix);
        mWebdavPathPrefixView = (EditText)rootView.findViewById(R.id.webdav_path_prefix);
        mWebdavAuthPathView = (EditText)rootView.findViewById(R.id.webdav_auth_path);
        mWebdavMailboxPathView = (EditText)rootView.findViewById(R.id.webdav_mailbox_path);
        mNextButton = (Button)rootView.findViewById(R.id.next);
        mCompressionMobile = (CheckBox)rootView.findViewById(R.id.compression_mobile);
        mCompressionWifi = (CheckBox)rootView.findViewById(R.id.compression_wifi);
        mCompressionOther = (CheckBox)rootView.findViewById(R.id.compression_other);
        mSubscribedFoldersOnly = (CheckBox)rootView.findViewById(R.id.subscribed_folders_only);

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNext();
            }
        });

        mImapAutoDetectNamespaceView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mImapPathPrefixView.setEnabled(!isChecked);
                if (isChecked && mImapPathPrefixView.hasFocus()) {
                    mImapPathPrefixView.focusSearch(View.FOCUS_UP).requestFocus();
                } else if (!isChecked) {
                    mImapPathPrefixView.requestFocus();
                }
            }
        });

        mAuthTypeAdapter = AuthTypeAdapter.get(getActivity());
        mAuthTypeView.setAdapter(mAuthTypeAdapter);

        /*
         * Only allow digits in the port field.
         */
        mPortView.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        editSettings = Intent.ACTION_EDIT.equals(getArguments().getString(EXTRA_ACTION));

        String accountUuid = getArguments().getString(EXTRA_ACCOUNT);
        mAccount = getAccountFromPreferences(accountUuid);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = getAccountFromPreferences(accountUuid);
        }

        try {
            Log.i(K9.LOG_TAG, "Setting up based on settings: " + mAccount.getStoreUri());
            ServerSettings settings = RemoteStore.decodeStoreUri(mAccount.getStoreUri());

            if (savedInstanceState == null) {
                // The first item is selected if settings.authenticationType is null or is not in mAuthTypeAdapter
                mCurrentAuthTypeViewPosition = mAuthTypeAdapter.getAuthPosition(settings.authenticationType);
            } else {
                mCurrentAuthTypeViewPosition = savedInstanceState.getInt(STATE_AUTH_TYPE_POSITION);
            }
            mAuthTypeView.setSelection(mCurrentAuthTypeViewPosition, false);
            updateViewFromAuthType();

            if (settings.username != null) {
                mUsernameView.setText(settings.username);
            }

            if (settings.password != null) {
                mPasswordView.setText(settings.password);
            }

            if (settings.clientCertificateAlias != null) {
                mClientCertificateSpinner.setAlias(settings.clientCertificateAlias);
            }

            mStoreType = settings.type;
            if (ServerSettings.Type.POP3 == settings.type) {
                serverLabelView.setText(R.string.account_setup_incoming_pop_server_label);
                rootView.findViewById(R.id.imap_path_prefix_section).setVisibility(View.GONE);
                rootView.findViewById(R.id.webdav_advanced_header).setVisibility(View.GONE);
                rootView.findViewById(R.id.webdav_mailbox_alias_section).setVisibility(View.GONE);
                rootView.findViewById(R.id.webdav_owa_path_section).setVisibility(View.GONE);
                rootView.findViewById(R.id.webdav_auth_path_section).setVisibility(View.GONE);
                rootView.findViewById(R.id.compression_section).setVisibility(View.GONE);
                rootView.findViewById(R.id.compression_label).setVisibility(View.GONE);
                mSubscribedFoldersOnly.setVisibility(View.GONE);
            } else if (ServerSettings.Type.IMAP == settings.type) {
                serverLabelView.setText(R.string.account_setup_incoming_imap_server_label);

                ImapStoreSettings imapSettings = (ImapStoreSettings) settings;

                mImapAutoDetectNamespaceView.setChecked(imapSettings.autoDetectNamespace);
                if (imapSettings.pathPrefix != null) {
                    mImapPathPrefixView.setText(imapSettings.pathPrefix);
                }

                rootView.findViewById(R.id.webdav_advanced_header).setVisibility(View.GONE);
                rootView.findViewById(R.id.webdav_mailbox_alias_section).setVisibility(View.GONE);
                rootView.findViewById(R.id.webdav_owa_path_section).setVisibility(View.GONE);
                rootView.findViewById(R.id.webdav_auth_path_section).setVisibility(View.GONE);

                if (!editSettings) {
                    rootView.findViewById(R.id.imap_folder_setup_section).setVisibility(View.GONE);
                }
            } else if (ServerSettings.Type.WebDAV == settings.type) {
                serverLabelView.setText(R.string.account_setup_incoming_webdav_server_label);
                mConnectionSecurityChoices = new ConnectionSecurity[] {
                        ConnectionSecurity.NONE,
                        ConnectionSecurity.SSL_TLS_REQUIRED };

                // Hide the unnecessary fields
                rootView.findViewById(R.id.imap_path_prefix_section).setVisibility(View.GONE);
                rootView.findViewById(R.id.account_auth_type_label).setVisibility(View.GONE);
                rootView.findViewById(R.id.account_auth_type).setVisibility(View.GONE);
                rootView.findViewById(R.id.compression_section).setVisibility(View.GONE);
                rootView.findViewById(R.id.compression_label).setVisibility(View.GONE);
                mSubscribedFoldersOnly.setVisibility(View.GONE);

                WebDavStoreSettings webDavSettings = (WebDavStoreSettings) settings;

                if (webDavSettings.path != null) {
                    mWebdavPathPrefixView.setText(webDavSettings.path);
                }

                if (webDavSettings.authPath != null) {
                    mWebdavAuthPathView.setText(webDavSettings.authPath);
                }

                if (webDavSettings.mailboxPath != null) {
                    mWebdavMailboxPathView.setText(webDavSettings.mailboxPath);
                }
            } else {
                throw new Exception("Unknown account type: " + mAccount.getStoreUri());
            }

            if (!editSettings) {
                mAccount.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(settings.type));
            }

            // Note that mConnectionSecurityChoices is configured above based on server type
            ConnectionSecurityAdapter securityTypesAdapter =
                    ConnectionSecurityAdapter.get(getActivity(), mConnectionSecurityChoices);
            mSecurityTypeView.setAdapter(securityTypesAdapter);

            // Select currently configured security type
            if (savedInstanceState == null) {
                mCurrentSecurityTypeViewPosition = securityTypesAdapter.getConnectionSecurityPosition(settings.connectionSecurity);
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

            updateAuthPlainTextFromSecurityType(settings.connectionSecurity);

            mCompressionMobile.setChecked(mAccount.useCompression(NetworkType.MOBILE));
            mCompressionWifi.setChecked(mAccount.useCompression(NetworkType.WIFI));
            mCompressionOther.setChecked(mAccount.useCompression(NetworkType.OTHER));

            if (settings.host != null) {
                mServerView.setText(settings.host);
            }

            if (settings.port != -1) {
                mPortView.setText(String.format("%d", settings.port));
            } else {
                updatePortFromSecurityType();
            }
            mCurrentPortViewSetting = mPortView.getText().toString();

            mSubscribedFoldersOnly.setChecked(mAccount.subscribedFoldersOnly());
        } catch (Exception e) {
            failure(e);
        }

        initializeViewListeners();
        validateFields();
        if (editSettings) {
            mNextButton.setText(R.string.done_action);
        }
        return rootView;
    }

    private Account getAccountFromPreferences(String accountUuid) {
        return editSettings
                ? preferences.getAccount(accountUuid)
                : preferences.getAccountAllowingIncomplete(accountUuid);
    }

    @Override
    protected void inject() {
        getpEpComponent().inject(this);
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
            if (authType != AuthType.EXTERNAL_PLAIN) {
                mPasswordView.setVisibility(View.GONE);
                mPasswordLabelView.setVisibility(View.GONE);
            }
            mClientCertificateLabelView.setVisibility(View.VISIBLE);
            mClientCertificateSpinner.setVisibility(View.VISIBLE);
        } else if (isAuthTypeXOAuth2) {
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
            String toastText = getString(R.string.account_setup_incoming_invalid_setting_combo_notice,
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
            isAuthTypeExternal =authType.isExternalAuth();

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
                && !isAuthTypeExternal && !isAuthTypeXOAuth2
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

        mNextButton.setEnabled(Utility.domainFieldValid(mServerView)
                && hasValidPort
                && (hasValidPasswordSettings || hasValidExternalAuthSettings || hasValidXOAuth2Settings));
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
        mPortView.setText(String.valueOf(AccountCreator.getDefaultPort(securityType, mStoreType)));
        mPortView.addTextChangedListener(validationTextWatcher);
    }

    private void updateAuthPlainTextFromSecurityType(ConnectionSecurity securityType) {
        mAuthTypeAdapter.useInsecureText(securityType == ConnectionSecurity.NONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            goForward();
        }
    }

    private void checkSettings() {
        AccountSetupCheckSettings.actionCheckSettings(
                requireActivity(), mAccount, AccountSetupCheckSettings.CheckDirection.INCOMING, false, editSettings);
    }

    private void goForward() {
        if (editSettings) {
            if (getActivity() != null) {
                getActivity().finish();
            }
        } else {
            accountSetupNavigator.goForward(getFragmentManager(), mAccount);
        }
    }

    protected void onNext() {
        updateAccountSettings();
        checkSettings();
    }

    private void fail(Exception use) {
        Log.e(K9.LOG_TAG, "Failure", use);
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());
        FeedbackTools.showLongFeedback(rootView, toastText);
    }

    private void fetchAuthTokenForAccount(final String emailAddress) {
        new Exception().printStackTrace();
        AccountManager accountManager = AccountManager.get(getActivity());
        android.accounts.Account[] accounts = accountManager.getAccountsByType("com.google");
        for (android.accounts.Account account : accounts) {
            Log.w(K9.LOG_TAG, "Account: " + account.name);
            if(account.name.equals(emailAddress)) {
                accountManager.getAuthToken(account, GMAIL_AUTH_TOKEN_TYPE, null, getActivity(),
                        new AccountManagerCallback<Bundle>() {
                            @Override
                            public void run(AccountManagerFuture<Bundle> future) {
                                try {
                                    Bundle bundle = future.getResult();
                                    if(bundle.get(AccountManager.KEY_ACCOUNT_NAME).equals(emailAddress)) {
                                    }
                                } catch (Exception e) {
                                    failure(e);
                                }
                            }
                        }, null);
                return;
            }
        }
        failure(new Exception("Account doesn't exist"));

    }

    private void updateAccountSettings() {
        ConnectionSecurity connectionSecurity = getSelectedSecurity();
        String username = mUsernameView.getText().toString();
        String password = null;
        String clientCertificateAlias = null;
        AuthType authType = getSelectedAuthType();
        if (authType.isExternalAuth()) {
            clientCertificateAlias = mClientCertificateSpinner.getAlias();
        }
        String host = mServerView.getText().toString();
        int port = Integer.parseInt(mPortView.getText().toString());
        Map<String, String> extra = null;
        if (authType != AuthType.EXTERNAL) {
            password = mPasswordView.getText().toString();
        }
        if (ServerSettings.Type.IMAP == mStoreType) {
            extra = new HashMap<String, String>();
            extra.put(AUTODETECT_NAMESPACE_KEY,
                    Boolean.toString(mImapAutoDetectNamespaceView.isChecked()));
            extra.put(ImapStoreSettings.PATH_PREFIX_KEY,
                    mImapPathPrefixView.getText().toString());
        } else if (ServerSettings.Type.WebDAV == mStoreType) {
            extra = new HashMap<String, String>();
            extra.put(WebDavStoreSettings.PATH_KEY,
                    mWebdavPathPrefixView.getText().toString());
            extra.put(WebDavStoreSettings.AUTH_PATH_KEY,
                    mWebdavAuthPathView.getText().toString());
            extra.put(WebDavStoreSettings.MAILBOX_PATH_KEY,
                    mWebdavMailboxPathView.getText().toString());
        }
        mAccount.deleteCertificate(host, port, AccountSetupCheckSettings.CheckDirection.INCOMING);
        ServerSettings settings = new ServerSettings(mStoreType, host, port,
                connectionSecurity, authType, username, password, clientCertificateAlias, extra);
        mAccount.setStoreUri(RemoteStore.createStoreUri(settings));
        mAccount.setCompression(NetworkType.MOBILE, mCompressionMobile.isChecked());
        mAccount.setCompression(NetworkType.WIFI, mCompressionWifi.isChecked());
        mAccount.setCompression(NetworkType.OTHER, mCompressionOther.isChecked());
        if (!editSettings) {
            setOutgoingSettingsSameAsIncomingSettings();
        }
    }

    private void setOutgoingSettingsSameAsIncomingSettings() {
        /*
         * Set the username and password for the outgoing settings to the username and
         * password the user just set for incoming.
         */
        try {
            String username = mUsernameView.getText().toString();

            String password = null;
            String clientCertificateAlias = null;
            AuthType authType = getSelectedAuthType();
            if (AuthType.EXTERNAL == authType) {
                clientCertificateAlias = mClientCertificateSpinner.getAlias();
            } else if (AuthType.EXTERNAL_PLAIN == authType) {
                clientCertificateAlias = mClientCertificateSpinner.getAlias();
                password = mPasswordView.getText().toString();
            } else {
                password = mPasswordView.getText().toString();
            }

            URI oldUri = new URI(mAccount.getTransportUri());
            ServerSettings transportServer = new ServerSettings(ServerSettings.Type.SMTP, oldUri.getHost(), oldUri.getPort(),
                    ConnectionSecurity.SSL_TLS_REQUIRED, authType, username, password, clientCertificateAlias);
            String transportUri = Transport.createTransportUri(transportServer);
            mAccount.setTransportUri(transportUri);
        } catch (URISyntaxException use) {
            /*
             * If we can't set up the URL we just continue. It's only for
             * convenience.
             */
        }
    }

    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.next:
                    onNext();
                    break;
            }
        } catch (Exception e) {
            failure(e);
        }
    }

    private void failure(Exception use) {
        Log.e(K9.LOG_TAG, "Failure", use);
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());
        FeedbackTools.showLongFeedback(requireActivity().findViewById(android.R.id.content), toastText);
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
            /* unused */
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            /* unused */
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
        accountSetupNavigator.setCurrentStep(AccountSetupNavigator.Step.INCOMING, mAccount);
    }
}
