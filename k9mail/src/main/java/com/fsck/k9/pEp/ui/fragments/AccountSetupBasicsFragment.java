package com.fsck.k9.pEp.ui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.ContentLoadingProgressBar;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.account.AndroidAccountOAuth2TokenStore;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.AccountSetupNames;
import com.fsck.k9.activity.setup.OAuthFlowActivity;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.filter.Hex;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.ui.infrastructure.exceptions.PEpCertificateException;
import com.fsck.k9.pEp.ui.infrastructure.exceptions.PEpSetupException;
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.pEp.ui.tools.SetupAccountType;
import com.fsck.k9.view.ClientCertificateSpinner;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.OnTextChanged;
import security.pEp.permissions.PermissionChecker;
import security.pEp.permissions.PermissionRequester;
import security.pEp.provisioning.AccountMailSettingsProvision;
import security.pEp.provisioning.ProvisioningSettings;
import security.pEp.provisioning.SimpleMailSettings;
import timber.log.Timber;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.fsck.k9.mail.ServerSettings.Type.IMAP;

public class AccountSetupBasicsFragment extends PEpFragment
        implements View.OnClickListener, TextWatcher, CompoundButton.OnCheckedChangeListener,
        ClientCertificateSpinner.OnClientCertificateChangedListener, AccountSetupBasics.AccountSetupSettingsCheckerFragment {
    private static final int ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 1;
    private final static String EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account";
    private final static int DIALOG_NOTE = 1;
    private final static String STATE_KEY_PROVIDER =
            "com.fsck.k9.AccountSetupBasics.provider";
    private final static String STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming";
    public static final String GMAIL = "gmail";
    private static final String ERROR_DIALOG_SHOWING_KEY = "errorDialogShowing";
    private static final String ERROR_DIALOG_TITLE = "errorDialogTitle";
    private static final String ERROR_DIALOG_MESSAGE = "errorDialogMessage";
    private static final String WAS_LOADING = "wasLoading";
    private static final int REQUEST_CODE_OAUTH = Activity.RESULT_FIRST_USER + 1;

    private EditText mEmailView;
    private EditText mPasswordView;
    private CheckBox mClientCertificateCheckBox;
    private ClientCertificateSpinner mClientCertificateSpinner;
    private CheckBox mOAuth2CheckBox;
    private Spinner mAccountSpinner;
    private Button mNextButton;
    private Button mManualSetupButton;
    private Account mAccount;
    private AccountSetupBasicsFragment.Provider mProvider;
    private AndroidAccountOAuth2TokenStore accountTokenStore;

    private EmailAddressValidator mEmailValidator = new EmailAddressValidator();

    private boolean mCheckedIncoming = false;
    private ContentLoadingProgressBar nextProgressBar;
    private View rootView;
    private AccountSetupNavigator accountSetupNavigator;
    private PePUIArtefactCache pEpUIArtefactCache;

    public boolean ismCheckedIncoming() {
        return mCheckedIncoming;
    }

    private AlertDialog errorDialog;
    private int errorDialogTitle;
    private String errorDialogMessage;
    private boolean errorDialogWasShowing;
    private boolean wasLoading;

    @Inject
    PEpSettingsChecker pEpSettingsChecker;
    @Inject
    SetupAccountType setupAccountType;
    @Inject
    PermissionChecker permissionChecker;
    @Inject
    PermissionRequester permissionRequester;
    @Inject
    ProvisioningSettings provisioningSettings;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setupPEpFragmentToolbar();
        rootView = inflater.inflate(R.layout.fragment_account_login, container, false);
        setupToolbar();
        mEmailView = rootView.findViewById(R.id.account_email);
        mPasswordView = rootView.findViewById(R.id.account_password);
        mClientCertificateCheckBox = rootView.findViewById(R.id.account_client_certificate);
        mClientCertificateSpinner = rootView.findViewById(R.id.account_client_certificate_spinner);
        mOAuth2CheckBox = rootView.findViewById(R.id.account_oauth2);
        mNextButton = rootView.findViewById(R.id.next);
        nextProgressBar = rootView.findViewById(R.id.next_progressbar);
        mManualSetupButton = rootView.findViewById(R.id.manual_setup);
        mNextButton.setOnClickListener(this);
        mManualSetupButton.setOnClickListener(this);
        mAccountSpinner = rootView.findViewById(R.id.account_spinner);

        initializeViewListeners();
        validateFields();

        pEpUIArtefactCache = PePUIArtefactCache.getInstance(getActivity().getApplicationContext());

        String email = pEpUIArtefactCache.getEmailInPreferences();
        String password = pEpUIArtefactCache.getPasswordInPreferences();
        if (email != null && password != null) {
            mEmailView.setText(email);
            mPasswordView.setText(password);
        } else if (BuildConfig.IS_ENTERPRISE) {
            mEmailView.setText(provisioningSettings.getEmail());
        }
        setHasOptionsMenu(!BuildConfig.IS_ENTERPRISE);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.account_setup_basic_option, menu);
    }

    private void setupToolbar() {
        ((AccountSetupBasics) getActivity()).initializeToolbar(!getActivity().isTaskRoot(), R.string.account_setup_basics_title);
    }

    private void initializeViewListeners() {
        mEmailView.addTextChangedListener(this);
        mPasswordView.addTextChangedListener(this);
        mClientCertificateCheckBox.setOnCheckedChangeListener(this);
        mClientCertificateSpinner.setOnClientCertificateChangedListener(this);
        mClientCertificateSpinner.setOnClientCertificateChangedListener(this);

        mOAuth2CheckBox.setOnCheckedChangeListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAccount != null) {
            outState.putString(EXTRA_ACCOUNT, mAccount.getUuid());
        }
        if (mProvider != null) {
            outState.putSerializable(STATE_KEY_PROVIDER, mProvider);
        }
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, mCheckedIncoming);
        saveErrorDialogState(outState);
        outState.putBoolean(WAS_LOADING, wasLoading);
    }

    private void saveErrorDialogState(Bundle outState) {
        outState.putBoolean(ERROR_DIALOG_SHOWING_KEY, errorDialogWasShowing);
        outState.putInt(ERROR_DIALOG_TITLE, errorDialogTitle);
        outState.putString(ERROR_DIALOG_MESSAGE, errorDialogMessage);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
                String accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
                mAccount = Preferences.getPreferences(getActivity()).getAccountAllowingIncomplete(accountUuid);
            }

            if (savedInstanceState.containsKey(STATE_KEY_PROVIDER)) {
                mProvider = (AccountSetupBasicsFragment.Provider) savedInstanceState.getSerializable(STATE_KEY_PROVIDER);
            }

            mCheckedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING);

            updateViewVisibility(mClientCertificateCheckBox.isChecked(), mOAuth2CheckBox.isChecked());
            restoreErrorDialogState(savedInstanceState);
            wasLoading = savedInstanceState.getBoolean(WAS_LOADING);
        }
    }

    private void restoreErrorDialogState(Bundle savedInstanceState) {
        errorDialogWasShowing = savedInstanceState.getBoolean(ERROR_DIALOG_SHOWING_KEY);
        errorDialogTitle = savedInstanceState.getInt(ERROR_DIALOG_TITLE);
        errorDialogMessage = savedInstanceState.getString(ERROR_DIALOG_MESSAGE);
    }

    public void afterTextChanged(Editable s) {
        validateFields();
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onClientCertificateChanged(String alias) {
        validateFields();
    }

    /**
     * Called when checking the client certificate CheckBox
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!permissionChecker.hasContactsPermission()) {
            if (isChecked) {
                permissionRequester.requestContactsPermission(rootView, new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        contactsPermissionGranted();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        contactsPermissionDenied();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        //NOP
                    }
                });
            }
        } else {
            updateViewVisibility(mClientCertificateCheckBox.isChecked(), mOAuth2CheckBox.isChecked());
            validateFields();

            // Have the user select (or confirm) the client certificate
            if (buttonView.equals(mClientCertificateCheckBox) && isChecked) {
                mClientCertificateSpinner.chooseCertificate();
            }
        }

    }

    private void updateViewVisibility(boolean usingCertificates, boolean usingXoauth) {
        if (usingCertificates) {
            mClientCertificateSpinner.setVisibility(View.VISIBLE);
            mOAuth2CheckBox.setEnabled(false);
        } else if (usingXoauth) {
            // hide username and password fields, show account spinner
            mEmailView.setVisibility(View.VISIBLE);
            mAccountSpinner.setVisibility(View.GONE);
            mPasswordView.setVisibility(View.GONE);
        } else {
            // show username & password fields, hide client certificate spinner
            mEmailView.setVisibility(View.VISIBLE);
            mAccountSpinner.setVisibility(View.GONE);
            mPasswordView.setVisibility(View.VISIBLE);
            mClientCertificateSpinner.setVisibility(View.GONE);
            mClientCertificateCheckBox.setEnabled(true);
            mOAuth2CheckBox.setEnabled(true);
        }
    }

    private void validateFields() {
        String clientCertificateAlias = mClientCertificateSpinner.getAlias();
        String email = mEmailView.getText().toString().trim();

        boolean clientCertificateChecked = mClientCertificateCheckBox.isChecked();
        boolean oauth2Checked = mOAuth2CheckBox.isChecked();
        boolean emailValid = Utility.requiredFieldValid(mEmailView) && mEmailValidator.isValidAddressOnly(email);
        boolean passwordValid = Utility.requiredFieldValid(mPasswordView);

        boolean oauth2 =
                oauth2Checked &&
                        emailValid;

        boolean certificateAuth =
                clientCertificateChecked && clientCertificateAlias != null && emailValid;

        boolean emailPasswordAuth =
                !oauth2Checked
                        && !certificateAuth
                        && !clientCertificateChecked
                        && emailValid
                        && passwordValid;

        boolean valid = oauth2 || certificateAuth || emailPasswordAuth;

        mNextButton.setEnabled(valid);
        mManualSetupButton.setEnabled(valid);
        /*
         * Dim the next button's icon to 50% if the button is disabled.
         * TODO this can probably be done with a stateful drawable. Check into it.
         * android:state_enabled
         */
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    @OnTextChanged(R.id.account_email)
    public void onEmailChanged() {
        validateFields();
    }

    @OnTextChanged(R.id.account_password)
    public void onPasswordChanged() {
        validateFields();
    }

    private String getOwnerName() {
        String name = null;
        if (BuildConfig.IS_ENTERPRISE) {
            if (provisioningSettings.getSenderName() != null) {
                name = provisioningSettings.getSenderName();
            }
        } else {
            try {
                name = getDefaultAccountName();
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Could not get default account name", e);
            }

            if (name == null) {
                name = "";
            }
        }
        return name;
    }

    private String getDefaultAccountName() {
        String name = null;
        Account account = Preferences.getPreferences(getActivity()).getDefaultAccount();
        if (account != null) {
            name = account.getName();
        }
        return name;
    }

    private Dialog onCreateDialog(int id) {
        if (id == DIALOG_NOTE) {
            if (mProvider != null && mProvider.note != null) {
                return new AlertDialog.Builder(getActivity())
                        .setMessage(mProvider.note)
                        .setPositiveButton(
                                getString(R.string.okay_action),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finishAutoSetup();
                                    }
                                })
                        .setNegativeButton(
                                getString(R.string.cancel_action),
                                null)
                        .create();
            }
        }
        return null;
    }

    private void finishAutoSetup() {
        boolean usingXOAuth2 = mOAuth2CheckBox.isChecked();

        String email;
        if (usingXOAuth2)
            email = mEmailView.getText().toString().trim();
        else
            email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString();
        String[] emailParts = splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];
        try {
            String userEnc = UrlEncodingHelper.encodeUtf8(user);
            String passwordEnc = UrlEncodingHelper.encodeUtf8(password);

            String incomingUsername = mProvider.incomingUsernameTemplate;
            incomingUsername = incomingUsername.replaceAll("\\$email", email);
            incomingUsername = incomingUsername.replaceAll("\\$user", userEnc);
            incomingUsername = incomingUsername.replaceAll("\\$domain", domain);

            URI incomingUriTemplate = mProvider.incomingUriTemplate;
            int port = RemoteStore.decodeStoreUri(incomingUriTemplate.toString()).port;
            String incomingUserInfo = incomingUsername + ":" + passwordEnc;
            if (usingXOAuth2)
                incomingUserInfo = AuthType.XOAUTH2 + ":" + incomingUserInfo;
            URI incomingUri = new URI(incomingUriTemplate.getScheme(), incomingUserInfo,
                    incomingUriTemplate.getHost(), port,
                    null, null, null);

            String outgoingUsername = mProvider.outgoingUsernameTemplate;

            URI outgoingUriTemplate = mProvider.outgoingUriTemplate;
            port = Transport.decodeTransportUri(outgoingUriTemplate.toString()).port;

            URI outgoingUri;
            if (outgoingUsername != null) {
                outgoingUsername = outgoingUsername.replaceAll("\\$email", email);
                outgoingUsername = outgoingUsername.replaceAll("\\$user", userEnc);
                outgoingUsername = outgoingUsername.replaceAll("\\$domain", domain);

                String outgoingUserInfo = outgoingUsername + ":" + passwordEnc;
                if (usingXOAuth2) {
                    outgoingUserInfo = outgoingUserInfo + ":" + AuthType.XOAUTH2;
                }
                outgoingUri = new URI(outgoingUriTemplate.getScheme(), outgoingUserInfo,
                        outgoingUriTemplate.getHost(), port, null,
                        null, null);

            } else {
                outgoingUri = new URI(outgoingUriTemplate.getScheme(),
                        null, outgoingUriTemplate.getHost(), port, null,
                        null, null);


            }
            initializeAccount();
            mAccount.setEmail(email);
            mAccount.setName(getOwnerName());
            if (BuildConfig.IS_ENTERPRISE && mAccount.getName() == null) {
                mAccount.setName(mAccount.getEmail());
            }
            if (BuildConfig.IS_ENTERPRISE) {
                mAccount.setDescription(
                        !Utility.isNullOrBlank(provisioningSettings.getAccountDescription())
                            ? provisioningSettings.getAccountDescription()
                            : mAccount.getEmail()
                );
            }
            mAccount.setStoreUri(incomingUri.toString());
            mAccount.setTransportUri(outgoingUri.toString());

            setupFolderNames(incomingUriTemplate.getHost().toLowerCase(Locale.US));

            ServerSettings incomingSettings = RemoteStore.decodeStoreUri(incomingUri.toString());
            ServerSettings outgoingSettings = Transport.decodeTransportUri(outgoingUri.toString());
            mAccount.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));

            saveCredentialsInPreferences();

            if (incomingSettings != null && outgoingSettings !=null &&
                    incomingSettings.authenticationType == AuthType.XOAUTH2 &&
                    outgoingSettings.authenticationType == AuthType.XOAUTH2
            ) {
                startOAuthFlow();
            } else {
                checkSettings();
            }

            // Check incoming here.  Then check outgoing in onActivityResult()

        } catch (URISyntaxException use) {
            /*
             * If there is some problem with the URI we give up and go on to
             * manual setup.
             */
            onManualSetup();
        }
    }

    private void startOAuthFlow() {
        Intent intent = OAuthFlowActivity.Companion.buildLaunchIntent(requireContext(), mAccount.getUuid());
        requireActivity().startActivityForResult(intent, REQUEST_CODE_OAUTH);
    }

    private void checkSettings() {
        checkSettings(AccountSetupCheckSettings.CheckDirection.INCOMING);
    }

    private void checkSettings(AccountSetupCheckSettings.CheckDirection direction) {
        AccountSetupBasics.BasicsSettingsCheckCallback basicsSettingsCheckCallback = new AccountSetupBasics.BasicsSettingsCheckCallback(this);
        ((AccountSetupBasics)requireActivity()).setBasicsFragmentSettingsCallback(basicsSettingsCheckCallback);
        pEpSettingsChecker.checkSettings(mAccount, direction, false, AccountSetupCheckSettingsFragment.LOGIN,
                false, basicsSettingsCheckCallback);
    }

    private void saveCredentialsInPreferences() {
        pEpUIArtefactCache.saveCredentialsInPreferences(mEmailView.getText().toString(), mPasswordView.getText().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
        accountSetupNavigator = ((AccountSetupBasics) getActivity()).getAccountSetupNavigator();
        accountSetupNavigator.setCurrentStep(AccountSetupNavigator.Step.BASICS, mAccount);
        accountTokenStore = K9.oAuth2TokenStore;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), R.layout.simple_spinner_item, accountTokenStore.getAccounts());
        mAccountSpinner.setAdapter(adapter);
        validateFields();
        restoreErrorDialogIfNeeded();
        restoreViewsEnabledState();
    }

    private void restoreViewsEnabledState() {
        mNextButton.setVisibility(wasLoading ? View.GONE : View.VISIBLE);
        mManualSetupButton.setEnabled(!wasLoading);
        enableViewGroup(!wasLoading, (ViewGroup)rootView);
        if(wasLoading) {
            nextProgressBar.setVisibility(View.VISIBLE);
            nextProgressBar.show();
            wasLoading = false;
        }
        else {
            nextProgressBar.hide();
        }
    }

    private void restoreErrorDialogIfNeeded() {
        if(errorDialogWasShowing) {
            showErrorDialog(errorDialogTitle, errorDialogMessage);
            errorDialogWasShowing = false;
        }
    }

    private void onNext() {
        nextProgressBar.show();
        mNextButton.setVisibility(View.GONE);
        accountSetupNavigator.setLoading(true);
        enableViewGroup(false, (ViewGroup) rootView);

        String email;
        if (mEmailView.getVisibility() == View.VISIBLE) {
            email = mEmailView.getText().toString().trim();
        } else {
            email = mAccountSpinner.getSelectedItem().toString();
        }
        if (isAValidAddress(email)) return;

        List<String> accounts = accountTokenStore.getAccounts();
        //if (accounts.contains(email)) {
        //    mOAuth2CheckBox.setChecked(true);
        //    mAccountSpinner.setSelection(accounts.indexOf(email));
        //    setup(email);
        //} else if (email.contains(GMAIL)) {
        //    new AlertDialog.Builder(getActivity())
        //            .setTitle(R.string.add_account_title)
        //            .setMessage(R.string.add_account_message)
        //            .setPositiveButton(getResources().getString(R.string.okay_action), new DialogInterface.OnClickListener() {
        //                @Override
        //                public void onClick(DialogInterface dialog, int which) {
        //                    accountSetupNavigator.createGmailAccount(getActivity());
        //                }
        //            })
        //            .setNegativeButton(getResources().getString(R.string.app_intro_skip_button), new DialogInterface.OnClickListener() {
        //                @Override
        //                public void onClick(DialogInterface dialog, int which) {
        //                    setup(email);
        //                }
        //            })
        //            .show();
        //} else {
        //    setup(email);
        //}
        setup(email);
    }

    private boolean isAValidAddress(String email) {
        return avoidAddingAlreadyExistingAccount(email) ||
                isEmailNull(email);
    }

    private boolean isEmailNull(String email) {
        if (email == null || email.isEmpty()) {
            resetView("You must enter an email address");
            return true;
        }
        return false;
    }

    private Provider getProvisionedProvider(String domain) {
        try {
            AccountMailSettingsProvision provisionSettings =
                    provisioningSettings.getProvisionedMailSettings();
            if (provisionSettings == null) {
                return null;
            }
            Provider provider = new Provider();
            provider.domain = domain;
            provider.id = "provisioned";
            provider.label = "enterprise provisioned provider";
            provider.incomingUriTemplate =
                    getServerUriTemplate(provisionSettings.getIncoming(), false);
            provider.outgoingUriTemplate = getServerUriTemplate(
                    provisionSettings.getOutgoing(),
                    true
            );
            provider.incomingUsernameTemplate = provisionSettings.getIncoming().getUserName();
            provider.outgoingUsernameTemplate = provisionSettings.getOutgoing().getUserName();
            return provider;
        } catch (Exception ex) {
            Timber.e(
                    ex,
                    "%s: Error while trying to parse provisioned provider.",
                    K9.LOG_TAG
            );
        }
        return null;
    }

    private URI getServerUriTemplate(
            SimpleMailSettings settings,
            boolean outgoing
    ) throws URISyntaxException {
        String uri = (outgoing ? "smtp" : "imap") +
                "+" +
                settings.getConnectionSecurityString() +
                "+" +
                "://" +
                settings.getServer() +
                ":" +
                settings.getPort();
        return new URI(uri);
    }

    private void setup(String email) {
        if (mClientCertificateCheckBox.isChecked()) {
            // Auto-setup doesn't support client certificates.
            onManualSetup();
            return;
        }
        String[] emailParts = splitEmail(email);
        String domain = emailParts[1];
        mProvider = findProviderForDomain(domain);
        if (mProvider == null) {
            /*
             * We don't have default settings for this account, start the manual
             * setup process.
             */
            onManualSetup();
            return;
        }
        Log.i(K9.LOG_TAG, "Provider found, using automatic set-up");
        if (mProvider.note != null) {
            onCreateDialog(DIALOG_NOTE);
        } else {
            finishAutoSetup();
        }
    }

    private boolean avoidAddingAlreadyExistingAccount(String email) {
        if (accountAlreadyExists(email)) {
            resetView(getString(R.string.account_already_exists));
            return true;
        }
        return false;
    }

    private void resetView(String feedback) {
        FeedbackTools.showLongFeedback(getView(), feedback);
        nextProgressBar.hide();
        mNextButton.setVisibility(View.VISIBLE);
        enableViewGroup(true, (ViewGroup) rootView);
    }

    @NonNull
    private boolean accountAlreadyExists(String email) {
        Preferences preferences = Preferences.getPreferences(getActivity());
        List<Account> accounts = preferences.getAccounts();
        for (Account account : accounts) {
            if (account.getEmail().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_PICK_SETTINGS_FILE
                && resultCode != RESULT_CANCELED) {
            ((AccountSetupBasics) getActivity()).onImport(data.getData());
        } else if (requestCode == REQUEST_CODE_OAUTH) {
            handleSignInResult(resultCode);
        } else {
            if (resultCode == RESULT_OK) {
                if (!mCheckedIncoming) {
                    //We've successfully checked incoming.  Now check outgoing.
                    mCheckedIncoming = true;
                    saveCredentialsInPreferences();
                    pEpSettingsChecker.checkSettings(mAccount, AccountSetupCheckSettings.CheckDirection.OUTGOING, false, AccountSetupCheckSettingsFragment.LOGIN,
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
                } else {
                    //We've successfully checked outgoing as well.
                    mAccount.setDescription(mAccount.getEmail());
                    mAccount.save(Preferences.getPreferences(getActivity()));
                    K9.setServicesEnabled(getActivity());
                    AccountSetupNames.actionSetNames(getActivity(), mAccount, false);
                    getActivity().finish();
                }
            }
        }
    }

    private void handleSignInResult(int resultCode) {
        if (resultCode != RESULT_OK) return;
        if (mAccount == null) {
            throw new IllegalStateException("Account instance missing");
        }
        checkSettings();
    }

    private void goForward() {
        try {
            setupAccountType.setupStoreAndSmtpTransport(mAccount, IMAP, "imap+ssl+");
            accountSetupNavigator.goForward(getFragmentManager(), mAccount, false);
        } catch (URISyntaxException e) {
            Timber.e(e);
        }
    }

    private void onManualSetup() {
        ((AccountSetupBasics) getActivity()).setManualSetupRequired(true);
        String email;
        if (mOAuth2CheckBox.isChecked()) {
            email = mAccountSpinner.getSelectedItem().toString();
        } else {
            email = mEmailView.getText().toString().trim();
        }

        if (isAValidAddress(email)) return;

        String[] emailParts = splitEmail(email);
        String user = email;
        String domain = emailParts[1];

        String password = null;
        String clientCertificateAlias = null;
        AuthType authenticationType;

        String imapHost = "mail." + domain;
        String smtpHost = "mail." + domain;

        if (mClientCertificateCheckBox.isChecked()) {
            if (mPasswordView.getText().toString().trim().isEmpty()) {
                authenticationType = AuthType.EXTERNAL;
            } else {
                authenticationType = AuthType.EXTERNAL_PLAIN;
                password = mPasswordView.getText().toString();
            }
            clientCertificateAlias = mClientCertificateSpinner.getAlias();
        } else if (mOAuth2CheckBox.isChecked()) {
            authenticationType = AuthType.XOAUTH2;
            imapHost = "imap.gmail.com";
            smtpHost = "smtp.gmail.com";
        } else {
            authenticationType = AuthType.PLAIN;
            password = mPasswordView.getText().toString();
        }

        initializeAccount();
        mAccount.setName(getOwnerName());
        mAccount.setEmail(email);

        // set default uris
        // NOTE: they will be changed again in AccountSetupAccountType!

        ServerSettings storeServer = new ServerSettings(ServerSettings.Type.IMAP, imapHost, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, authenticationType, user, password, clientCertificateAlias);
        ServerSettings transportServer = new ServerSettings(ServerSettings.Type.SMTP, smtpHost, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, authenticationType, user, password, clientCertificateAlias);
        String storeUri = RemoteStore.createStoreUri(storeServer);
        String transportUri = Transport.createTransportUri(transportServer);
        mAccount.setStoreUri(storeUri);
        mAccount.setTransportUri(transportUri);

        setupFolderNames(domain);

        saveCredentialsInPreferences();
        goForward();
    }

    private void initializeAccount() {
        if (mAccount == null || Preferences.getPreferences(getActivity()).getAccountAllowingIncomplete(mAccount.getUuid()) == null) {
            mAccount = Preferences.getPreferences(getActivity()).newAccount();
        }
    }

    private void setupFolderNames(String domain) {
        mAccount.setDraftsFolderName(getString(R.string.special_mailbox_name_drafts));
        mAccount.setTrashFolderName(getString(R.string.special_mailbox_name_trash));
        mAccount.setSentFolderName(getString(R.string.special_mailbox_name_sent));
        mAccount.setArchiveFolderName(getString(R.string.special_mailbox_name_archive));

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain.endsWith(".yahoo.com")) {
            mAccount.setSpamFolderName("Bulk Mail");
        } else {
            mAccount.setSpamFolderName(getString(R.string.special_mailbox_name_spam));
        }
    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                onNext();
                break;
            case R.id.manual_setup:
                onManualSetup();
                break;
        }
    }

    /**
     * Attempts to get the given attribute as a String resource first, and if it fails
     * returns the attribute as a simple String value.
     *
     * @param xml
     * @param name
     * @return
     */
    private String getXmlAttribute(XmlResourceParser xml, String name) {
        int resId = xml.getAttributeResourceValue(null, name, 0);
        if (resId == 0) {
            return xml.getAttributeValue(null, name);
        } else {
            return getString(resId);
        }
    }

    private AccountSetupBasicsFragment.Provider findProviderForDomain(String domain) {
        AccountSetupBasicsFragment.Provider provider = getProvisionedProvider(domain);
        if (provider != null) {
            return provider;
        }
        try {
            XmlResourceParser xml = getResources().getXml(R.xml.providers);
            int xmlEventType;
            provider = null;
            while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG
                        && "provider".equals(xml.getName())
                        && domain.equalsIgnoreCase(getXmlAttribute(xml, "domain"))) {
                    provider = new AccountSetupBasicsFragment.Provider();
                    provider.id = getXmlAttribute(xml, "id");
                    provider.label = getXmlAttribute(xml, "label");
                    provider.domain = getXmlAttribute(xml, "domain");
                    provider.note = getXmlAttribute(xml, "note");
                } else if (xmlEventType == XmlResourceParser.START_TAG
                        && "incoming".equals(xml.getName())
                        && provider != null) {
                    provider.incomingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.incomingUsernameTemplate = getXmlAttribute(xml, "username");
                } else if (xmlEventType == XmlResourceParser.START_TAG
                        && "outgoing".equals(xml.getName())
                        && provider != null) {
                    provider.outgoingUriTemplate = new URI(getXmlAttribute(xml, "uri"));
                    provider.outgoingUsernameTemplate = getXmlAttribute(xml, "username");
                } else if (xmlEventType == XmlResourceParser.END_TAG
                        && "provider".equals(xml.getName())
                        && provider != null) {
                    return provider;
                }
            }
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Error while trying to load provider settings.", e);
        }
        return null;
    }

    private String[] splitEmail(String email) {
        String[] retParts = new String[2];
        String[] emailParts = email.split("@");
        retParts[0] = (emailParts.length > 0) ? emailParts[0] : "";
        retParts[1] = (emailParts.length > 1) ? emailParts[1] : "";
        return retParts;
    }

    @Override
    protected void inject() {
        getpEpComponent().inject(this);
    }

    public void contactsPermissionDenied() {
        mOAuth2CheckBox.setChecked(false);
    }

    public void contactsPermissionGranted() {
        updateViewVisibility(mClientCertificateCheckBox.isChecked(), mOAuth2CheckBox.isChecked());
        validateFields();
    }

    private void handleErrorCheckingSettings(PEpSetupException exception) {
        if (exception.isCertificateAcceptanceNeeded()) {
            handleCertificateValidationException(exception);
        } else {
            showErrorDialog(
                    exception.getTitleResource(),
                    exception.getMessage() == null ? "" : exception.getMessage());
            Preferences.getPreferences(getActivity()).deleteAccount(mAccount);
        }
        nextProgressBar.hide();
        mNextButton.setVisibility(View.VISIBLE);
        enableViewGroup(true, (ViewGroup) rootView);
    }

    private void showErrorDialog(int stringResource, String message) {
        errorDialogTitle = stringResource;
        errorDialogMessage = message;
        errorDialog = new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(stringResource))
                .setMessage(message)
                .show();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissErrorDialogIfNeeded();
        wasLoading = mNextButton.getVisibility() != View.VISIBLE;
        nextProgressBar.hide();
    }

    private void dismissErrorDialogIfNeeded() {
        if(errorDialog != null && errorDialog.isShowing()) {
            errorDialog.dismiss();
            errorDialog = null;
            errorDialogWasShowing = true;
        }
    }


    private void handleCertificateValidationException(PEpSetupException cve) {
        PEpCertificateException certificateException = (PEpCertificateException) cve;
        Log.e(K9.LOG_TAG, "Error while testing settings (cve)", certificateException.getOriginalException());

        // Avoid NullPointerException in acceptKeyDialog()
        if (certificateException.hasCertChain()) {
            acceptKeyDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    certificateException.getOriginalException(),
                    cve.direction);
        } else {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_server_message_fmt,
                    errorMessageForCertificateException(certificateException.getOriginalException()));
        }
    }

    private String errorMessageForCertificateException(CertificateValidationException e) {
        switch (e.getReason()) {
            case Expired:
                return getString(R.string.client_certificate_expired, e.getAlias(), e.getMessage());
            case MissingCapability:
                return getString(R.string.auth_external_error);
            case RetrievalFailure:
                return getString(R.string.client_certificate_retrieval_failure, e.getAlias());
            case UseMessage:
                return e.getMessage();
            case Unknown:
            default:
                return "";
        }
    }

    private void acceptKeyDialog(
            final int msgResId,
            final CertificateValidationException ex,
            AccountSetupCheckSettings.CheckDirection direction
    ) {
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
                        final Collection<List<?>> subjectAlternativeNames = chain[i].getSubjectAlternativeNames();
                        if (subjectAlternativeNames != null) {
                            // The list of SubjectAltNames may be very long
                            //TODO: localize this string
                            StringBuilder altNamesText = new StringBuilder();
                            altNamesText.append("Subject has ").append(subjectAlternativeNames.size()).append(" alternative names\n");

                            // we need these for matching
                            String storeURIHost = (Uri.parse(mAccount.getStoreUri())).getHost();
                            String transportURIHost = (Uri.parse(mAccount.getTransportUri())).getHost();

                            for (List<?> subjectAlternativeName : subjectAlternativeNames) {
                                Integer type = (Integer) subjectAlternativeName.get(0);
                                Object value = subjectAlternativeName.get(1);
                                String name;
                                switch (type.intValue()) {
                                    case 0:
                                        Log.w(K9.LOG_TAG, "SubjectAltName of type OtherName not supported.");
                                        continue;
                                    case 1: // RFC822Name
                                        name = (String) value;
                                        break;
                                    case 2:  // DNSName
                                        name = (String) value;
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
                                        name = (String) value;
                                        break;
                                    case 7: // ip-address
                                        name = (String) value;
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
                                        acceptCertificate(chain[0], direction);
                                    }
                                })
                        .setNegativeButton(
                                getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
                                null
                        )
                        .show();
            }
        });
    }

    private void acceptCertificate(X509Certificate certificate,
                                   AccountSetupCheckSettings.CheckDirection direction) {
        try {
            if(direction.equals(AccountSetupCheckSettings.CheckDirection.INCOMING)) {
                mAccount.addCertificate(AccountSetupCheckSettings.CheckDirection.INCOMING,
                        certificate);
                checkSettings(AccountSetupCheckSettings.CheckDirection.OUTGOING);
            } else {
                mAccount.addCertificate(AccountSetupCheckSettings.CheckDirection.OUTGOING,
                        certificate);
                mAccount.setDescription(mAccount.getEmail());
                onSettingsChecked(PEpSettingsChecker.Redirection.TO_APP);
            }
        } catch (CertificateException e) {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    e.getMessage() == null ? "" : e.getMessage());
        }
    }

    @Override
    public void onSettingsCheckError(PEpSetupException exception) {
        handleErrorCheckingSettings(exception);
    }

    @Override
    public void onSettingsChecked(PEpSettingsChecker.Redirection redirection) {
        if(redirection.equals(PEpSettingsChecker.Redirection.OUTGOING)) {
            checkSettings(AccountSetupCheckSettings.CheckDirection.OUTGOING);
        } else {
            AccountSetupNames.actionSetNames(requireActivity(), mAccount, false);
            requireActivity().finish();
        }
    }

    @Override
    public void onSettingsCheckCancelled() {
        nextProgressBar.hide();
        mNextButton.setVisibility(View.VISIBLE);
        enableViewGroup(true, (ViewGroup) rootView);
    }

    static class Provider implements Serializable {
        private static final long serialVersionUID = 8511656164616538989L;

        public String id;

        public String label;

        public String domain;

        public URI incomingUriTemplate;

        public String incomingUsernameTemplate;

        public URI outgoingUriTemplate;

        public String outgoingUsernameTemplate;

        public String note;
    }
}
