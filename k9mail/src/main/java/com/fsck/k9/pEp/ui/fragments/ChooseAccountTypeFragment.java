package com.fsck.k9.pEp.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.AccountSetupIncoming;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.setup.ServerNameSuggester;

import java.net.URI;
import java.net.URISyntaxException;

import static com.fsck.k9.mail.ServerSettings.Type.IMAP;
import static com.fsck.k9.mail.ServerSettings.Type.POP3;
import static com.fsck.k9.mail.ServerSettings.Type.SMTP;
import static com.fsck.k9.mail.ServerSettings.Type.WebDAV;

public class ChooseAccountTypeFragment extends Fragment {

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private final ServerNameSuggester serverNameSuggester = new ServerNameSuggester();
    private Account mAccount;
    private boolean mMakeDefault;
    private View rootView;
    private AccountSetupNavigator accountSetupNavigator;

    public static ChooseAccountTypeFragment actionSelectAccountType(Account account, boolean makeDefault) {
        ChooseAccountTypeFragment fragment = new ChooseAccountTypeFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, account.getUuid());
        bundle.putBoolean(EXTRA_MAKE_DEFAULT, makeDefault);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_choose_account_type, container, false);
        rootView.findViewById(R.id.pop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPopClicked();
            }
        });
        rootView.findViewById(R.id.imap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onIMAPClicked();
            }
        });
        rootView.findViewById(R.id.webdav).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDavClicked();
            }
        });

        String accountUuid = getArguments().getString(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(getActivity()).getAccount(accountUuid);
        mMakeDefault = getArguments().getBoolean(EXTRA_MAKE_DEFAULT, false);
        ((AccountSetupBasics) getActivity()).initializeToolbar(true, R.string.account_setup_account_type_title);
        ((AccountSetupBasics) getActivity()).setStatusBarPepColor(getResources().getColor(R.color.pep_green));
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        accountSetupNavigator = ((AccountSetupBasics) getActivity()).getAccountSetupNavigator();
        accountSetupNavigator.setCurrentStep(AccountSetupNavigator.Step.ACCOUNT_TYPE, mAccount);
    }

    private void onDavClicked() {
        try {
            setupDav();
            goForward();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void goForward() {
        accountSetupNavigator.goForward(getFragmentManager(), mAccount, mMakeDefault);
    }

    private void onIMAPClicked() {
        try {
            setupStoreAndSmtpTransport(IMAP, "imap+ssl+");
            goForward();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void onPopClicked() {
        try {
            setupStoreAndSmtpTransport(POP3, "pop3+ssl+");
            goForward();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void setupStoreAndSmtpTransport(ServerSettings.Type serverType, String schemePrefix) throws URISyntaxException {
        String domainPart = EmailHelper.getDomainFromEmailAddress(mAccount.getEmail());

        String suggestedStoreServerName = serverNameSuggester.suggestServerName(serverType, domainPart);
        URI storeUriForDecode = new URI(mAccount.getStoreUri());
        URI storeUri = new URI(schemePrefix, storeUriForDecode.getUserInfo(), suggestedStoreServerName,
                storeUriForDecode.getPort(), null, null, null);
        mAccount.setStoreUri(storeUri.toString());

        String suggestedTransportServerName = serverNameSuggester.suggestServerName(SMTP, domainPart);
        URI transportUriForDecode = new URI(mAccount.getTransportUri());
        URI transportUri = new URI("smtp+tls+", transportUriForDecode.getUserInfo(), suggestedTransportServerName,
                transportUriForDecode.getPort(), null, null, null);
        mAccount.setTransportUri(transportUri.toString());
    }

    private void setupDav() throws URISyntaxException {
        URI uriForDecode = new URI(mAccount.getStoreUri());

        /*
         * The user info we have been given from
         * AccountSetupBasics.onManualSetup() is encoded as an IMAP store
         * URI: AuthType:UserName:Password (no fields should be empty).
         * However, AuthType is not applicable to WebDAV nor to its store
         * URI. Re-encode without it, using just the UserName and Password.
         */
        String userPass = "";
        String[] userInfo = uriForDecode.getUserInfo().split(":");
        if (userInfo.length > 1) {
            userPass = userInfo[1];
        }
        if (userInfo.length > 2) {
            userPass = userPass + ":" + userInfo[2];
        }

        String domainPart = EmailHelper.getDomainFromEmailAddress(mAccount.getEmail());
        String suggestedServerName = serverNameSuggester.suggestServerName(WebDAV, domainPart);
        URI uri = new URI("webdav+ssl+", userPass, suggestedServerName, uriForDecode.getPort(), null, null, null);
        mAccount.setStoreUri(uri.toString());
    }

    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.pop: {
                    setupStoreAndSmtpTransport(POP3, "pop3+ssl+");
                    break;
                }
                case R.id.imap: {
                    setupStoreAndSmtpTransport(IMAP, "imap+ssl+");
                    break;
                }
                case R.id.webdav: {
                    setupDav();
                    break;
                }
            }
        } catch (Exception ex) {
            failure(ex);
        }

        AccountSetupIncoming.actionIncomingSettings(getActivity(), mAccount, mMakeDefault);
        getActivity().finish();
    }

    private void failure(Exception use) {
        Log.e(K9.LOG_TAG, "Failure", use);
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        FeedbackTools.showLongFeedback(rootView, toastText);
    }

}
