package com.fsck.k9.pEp.ui.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.fsck.k9.Account;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.account.AndroidAccountOAuth2TokenStore;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.AccountSetupNames;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.pEp.UIUtils;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.view.ClientCertificateSpinner;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

public class AccountSetupBasicsFragment extends PEpFragment
        implements View.OnClickListener, TextWatcher, CompoundButton.OnCheckedChangeListener, ClientCertificateSpinner.OnClientCertificateChangedListener {
    private final static String EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account";
    private final static int DIALOG_NOTE = 1;
    private final static String STATE_KEY_PROVIDER =
            "com.fsck.k9.AccountSetupBasics.provider";
    private final static String STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming";

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

    public boolean ismCheckedIncoming() {
        return mCheckedIncoming;
    }
    private CheckBox mShowPasswordCheckBox;

    @Inject PEpSettingsChecker pEpSettingsChecker;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_account_login, container, false);
        setupToolbar();
        mEmailView = (EditText) rootView.findViewById(R.id.account_email);
        mPasswordView = (EditText) rootView.findViewById(R.id.account_password);
        mClientCertificateCheckBox = (CheckBox) rootView.findViewById(R.id.account_client_certificate);
        mClientCertificateSpinner = (ClientCertificateSpinner) rootView.findViewById(R.id.account_client_certificate_spinner);
        mOAuth2CheckBox = (CheckBox) rootView.findViewById(R.id.account_oauth2);
        mAccountSpinner = (Spinner) rootView.findViewById(R.id.account_spinner);
        accountTokenStore = K9.oAuth2TokenStore;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), R.layout.simple_spinner_item, accountTokenStore.getAccounts());
        mAccountSpinner.setAdapter(adapter);
        mNextButton = (Button) rootView.findViewById(R.id.next);
        mManualSetupButton = (Button) rootView.findViewById(R.id.manual_setup);
        mShowPasswordCheckBox = (CheckBox) rootView.findViewById(R.id.show_password);
        mNextButton.setOnClickListener(this);
        mManualSetupButton.setOnClickListener(this);

        initializeViewListeners();
        validateFields();

        String email = UIUtils.getEmailInPreferences(getActivity());
        String password = UIUtils.getPasswordInPreferences(getActivity());
        if (email != null && password != null) {
            mEmailView.setText(email);
            mPasswordView.setText(password);
        }
        return rootView;
    }

    private void setupToolbar() {
        ((AccountSetupBasics) getActivity()).initializeToolbar(true, R.string.account_setup_basics_title);
        ((AccountSetupBasics) getActivity()).setStatusBarPepColor(getResources().getColor(R.color.pep_green));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home: {
                getActivity().finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViewListeners() {
        mEmailView.addTextChangedListener(this);
        mPasswordView.addTextChangedListener(this);
        mClientCertificateCheckBox.setOnCheckedChangeListener(this);
        mClientCertificateSpinner.setOnClientCertificateChangedListener(this);
        mClientCertificateSpinner.setOnClientCertificateChangedListener(this);

        mOAuth2CheckBox.setOnCheckedChangeListener(this);

        mShowPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showPassword(isChecked);
            }
        });

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
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
                String accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
                mAccount = Preferences.getPreferences(getActivity()).getAccount(accountUuid);
            }

            if (savedInstanceState.containsKey(STATE_KEY_PROVIDER)) {
                mProvider = (AccountSetupBasicsFragment.Provider) savedInstanceState.getSerializable(STATE_KEY_PROVIDER);
            }

            mCheckedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING);

            updateViewVisibility(mClientCertificateCheckBox.isChecked(), mOAuth2CheckBox.isChecked());

            showPassword(mShowPasswordCheckBox.isChecked());
        }
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
        updateViewVisibility(mClientCertificateCheckBox.isChecked(), mOAuth2CheckBox.isChecked());
        validateFields();

        // Have the user select (or confirm) the client certificate
        if (buttonView.equals(mClientCertificateCheckBox) && isChecked) {
            mClientCertificateSpinner.chooseCertificate();
        }
    }

    private void updateViewVisibility(boolean usingCertificates, boolean usingXoauth) {
        if (usingCertificates) {
            // hide password fields, show client certificate spinner
            mPasswordView.setVisibility(View.GONE);
            mShowPasswordCheckBox.setVisibility(View.GONE);
            mClientCertificateSpinner.setVisibility(View.VISIBLE);
            mOAuth2CheckBox.setEnabled(false);
        } else if (usingXoauth) {
            // hide username and password fields, show account spinner
            mEmailView.setVisibility(View.GONE);
            mAccountSpinner.setVisibility(View.VISIBLE);
            mShowPasswordCheckBox.setVisibility(View.GONE);
            mPasswordView.setVisibility(View.GONE);
        } else {
            // show username & password fields, hide client certificate spinner
            mEmailView.setVisibility(View.VISIBLE);
            mAccountSpinner.setVisibility(View.GONE);
            mPasswordView.setVisibility(View.VISIBLE);
            mShowPasswordCheckBox.setVisibility(View.VISIBLE);
            mClientCertificateSpinner.setVisibility(View.GONE);
            mClientCertificateCheckBox.setEnabled(true);
            mOAuth2CheckBox.setEnabled(true);
        }
    }

    private void showPassword(boolean show) {
        int cursorPosition = mPasswordView.getSelectionStart();
        if (show) {
            mPasswordView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        mPasswordView.setSelection(cursorPosition);
    }

    private void validateFields() {
        boolean clientCertificateChecked = mClientCertificateCheckBox.isChecked();
        boolean oauth2Checked = mOAuth2CheckBox.isChecked();
        String clientCertificateAlias = mClientCertificateSpinner.getAlias();
        String email = mEmailView.getText().toString().trim();

        boolean valid =
                (oauth2Checked
                        && mAccountSpinner.getSelectedItem() != null
                        && mAccountSpinner.getSelectedItem().toString() != null
                        && !mAccountSpinner.getSelectedItem().toString().isEmpty()) ||
                        (Utility.requiredFieldValid(mEmailView)
                                && ((!clientCertificateChecked && Utility.requiredFieldValid(mPasswordView))
                                || (clientCertificateChecked && clientCertificateAlias != null)))
                                && mEmailValidator.isValidAddressOnly(email);

        mNextButton.setEnabled(valid);
        mManualSetupButton.setEnabled(valid);
        /*
         * Dim the next button's icon to 50% if the button is disabled.
         * TODO this can probably be done with a stateful drawable. Check into it.
         * android:state_enabled
         */
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    private String getOwnerName() {
        String name = null;
        try {
            name = getDefaultAccountName();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not get default account name", e);
        }

        if (name == null) {
            name = "";
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
            email = mAccountSpinner.getSelectedItem().toString();
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
            String incomingUserInfo = incomingUsername + ":" + passwordEnc;
            if (usingXOAuth2)
                incomingUserInfo = AuthType.XOAUTH2 + ":" + incomingUserInfo;
            URI incomingUri = new URI(incomingUriTemplate.getScheme(), incomingUserInfo,
                    incomingUriTemplate.getHost(), incomingUriTemplate.getPort(),
                    null, null, null);

            String outgoingUsername = mProvider.outgoingUsernameTemplate;

            URI outgoingUriTemplate = mProvider.outgoingUriTemplate;


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
                        outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                        null, null);

            } else {
                outgoingUri = new URI(outgoingUriTemplate.getScheme(),
                        null, outgoingUriTemplate.getHost(), outgoingUriTemplate.getPort(), null,
                        null, null);


            }
            if (mAccount == null) {
                mAccount = Preferences.getPreferences(getActivity()).newAccount();
            }
            mAccount.setName(getOwnerName());
            mAccount.setEmail(email);
            mAccount.setStoreUri(incomingUri.toString());
            mAccount.setTransportUri(outgoingUri.toString());

            setupFolderNames(incomingUriTemplate.getHost().toLowerCase(Locale.US));

            ServerSettings incomingSettings = RemoteStore.decodeStoreUri(incomingUri.toString());
            mAccount.setDeletePolicy(AccountCreator.getDefaultDeletePolicy(incomingSettings.type));

            // Check incoming here.  Then check outgoing in onActivityResult()
            saveCredentialsInPreferences();
            pEpSettingsChecker.checkSettings(mAccount.getUuid(), AccountSetupCheckSettings.CheckDirection.OUTGOING, false, AccountSetupCheckSettingsFragment.LOGIN,
                    false,
                    new PEpSettingsChecker.ResultCallback<PEpSettingsChecker.Redirection>() {
                        @Override
                        public void onLoaded(PEpSettingsChecker.Redirection redirection) {
                            AccountSetupNames.actionSetNames(getActivity(), mAccount);
                            getActivity().finish();
                        }

                        @Override
                        public void onError(String customMessage) {
                            Preferences.getPreferences(getActivity()).deleteAccount(mAccount);
                            showDialogFragment(customMessage);
                        }
                    });
        } catch (URISyntaxException use) {
            /*
             * If there is some problem with the URI we give up and go on to
             * manual setup.
             */
            onManualSetup();
        }
    }

    private void saveCredentialsInPreferences() {
        UIUtils.saveCredentialsInPreferences(getActivity(),  mEmailView.getText().toString(), mPasswordView.getText().toString());
    }

    private void onNext() {
        if (mClientCertificateCheckBox.isChecked() || mOAuth2CheckBox.isChecked()) {
            // Auto-setup doesn't support client certificates.
            onManualSetup();
            return;
        }
        String email;
        if (mEmailView.getVisibility() == View.VISIBLE) {
            email = mEmailView.getText().toString().trim();
        } else {
            email = mAccountSpinner.getSelectedItem().toString();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (!mCheckedIncoming) {
                //We've successfully checked incoming.  Now check outgoing.
                mCheckedIncoming = true;
                saveCredentialsInPreferences();
                pEpSettingsChecker.checkSettings(mAccount.getUuid(), AccountSetupCheckSettings.CheckDirection.OUTGOING, false, AccountSetupCheckSettingsFragment.LOGIN,
                        false,
                        new PEpSettingsChecker.ResultCallback<PEpSettingsChecker.Redirection>() {
                            @Override
                            public void onError(String customMessage) {
                                showDialogFragment(customMessage);
                            }

                            @Override
                            public void onLoaded(PEpSettingsChecker.Redirection redirection) {
                                ChooseAccountTypeFragment chooseAccountTypeFragment = ChooseAccountTypeFragment.actionSelectAccountType(mAccount, false);
                                getFragmentManager()
                                        .beginTransaction()
                                        .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                                        .replace(R.id.account_setup_container, chooseAccountTypeFragment, "chooseAccountTypeFragment")
                                        .commit();
                            }
                        });
            } else {
                //We've successfully checked outgoing as well.
                mAccount.setDescription(mAccount.getEmail());
                mAccount.save(Preferences.getPreferences(getActivity()));
                K9.setServicesEnabled(getActivity());
                AccountSetupNames.actionSetNames(getActivity(), mAccount);
                getActivity().finish();
            }
        }
    }

    private void onManualSetup() {
        String email;
        if (mOAuth2CheckBox.isChecked()) {
            email = mAccountSpinner.getSelectedItem().toString();
        } else {
            email = mEmailView.getText().toString().trim();
        }
        String[] emailParts = splitEmail(email);
        String user = email;
        String domain = emailParts[1];

        String password = null;
        String clientCertificateAlias = null;
        AuthType authenticationType;

        String imapHost = "mail." + domain;
        String smtpHost = "mail." + domain;

        if (mClientCertificateCheckBox.isChecked()) {
            authenticationType = AuthType.EXTERNAL;
            clientCertificateAlias = mClientCertificateSpinner.getAlias();
        } else if (mOAuth2CheckBox.isChecked()) {
            authenticationType = AuthType.XOAUTH2;
            imapHost = "imap.gmail.com";
            smtpHost = "smtp.gmail.com";
        } else {
            authenticationType = AuthType.PLAIN;
            password = mPasswordView.getText().toString();
        }

        if (mAccount == null) {
            mAccount = Preferences.getPreferences(getActivity()).newAccount();
        }
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
        ChooseAccountTypeFragment chooseAccountTypeFragment = ChooseAccountTypeFragment.actionSelectAccountType(mAccount, false);
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                .replace(R.id.account_setup_container, chooseAccountTypeFragment, "chooseAccountTypeFragment")
                .commit();
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
        try {
            XmlResourceParser xml = getResources().getXml(R.xml.providers);
            int xmlEventType;
            AccountSetupBasicsFragment.Provider provider = null;
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
    protected void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        DaggerPEpComponent.builder()
                .applicationComponent(applicationComponent)
                .pEpModule(new PEpModule(getActivity(), getLoaderManager(), getFragmentManager()))
                .build()
                .inject(this);
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
