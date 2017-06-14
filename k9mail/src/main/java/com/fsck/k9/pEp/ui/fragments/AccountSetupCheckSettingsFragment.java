package com.fsck.k9.pEp.ui.fragments;


import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.account.AndroidAccountOAuth2TokenStore;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.AccountSetupNames;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.fragment.ConfirmationDialogFragment;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.mail.filter.Hex;
import com.fsck.k9.mail.store.webdav.WebDavStore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

public class AccountSetupCheckSettingsFragment extends Fragment implements ConfirmationDialogFragment.ConfirmationDialogFragmentListener {

    public static final int ACTIVITY_REQUEST_CODE = 1;

    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_CHECK_DIRECTION ="checkDirection";
    private static final String EXTRA_ACTIVITY_REQUEST_CODE = "EXTRA_ACTIVITY_REQUEST_CODE";
    private static final String EXTRA_DEFAULT = "default";
    private static final String EXTRA_PROCEDENCE = "procedence";
    public static final String INCOMING = "INCOMING";
    public static final String OUTGOING = "OUTGOING";
    public static final String LOGIN = "LOGIN";
    private View rootView;

    private Handler mHandler = new Handler();

    private ProgressBar mProgressBar;

    private TextView mMessageView;

    private Account mAccount;

    private AccountSetupCheckSettings.CheckDirection mDirection;

    private boolean mCanceled;

    private boolean mDestroyed;
    private boolean mMakeDefault;
    private String mProcedence;

    public static AccountSetupCheckSettingsFragment actionCheckSettings(Account account,
                                                                        AccountSetupCheckSettings.CheckDirection direction, Boolean makeDefault, String procedence) {
        AccountSetupCheckSettingsFragment fragment = new AccountSetupCheckSettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ACCOUNT, account.getUuid());
        bundle.putSerializable(EXTRA_CHECK_DIRECTION, direction);
        bundle.putSerializable(EXTRA_ACTIVITY_REQUEST_CODE, ACTIVITY_REQUEST_CODE);
        bundle.putBoolean(EXTRA_DEFAULT, makeDefault);
        bundle.putString(EXTRA_PROCEDENCE, procedence);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_account_setup_check_settings, container, false);
        ((K9Activity) getActivity()).initializeToolbar(true, R.string.account_setup_check_settings_title);
        ((K9Activity) getActivity()).setStatusBarPepColor(getResources().getColor(R.color.pep_green));
        mMessageView = (TextView)rootView.findViewById(R.id.message);
        mProgressBar = (ProgressBar)rootView.findViewById(R.id.progress);
        rootView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel();
            }
        });

        setMessage(R.string.account_setup_check_settings_retr_info_msg);
        mProgressBar.setIndeterminate(true);

        String accountUuid = getArguments().getString(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(getActivity()).getAccount(accountUuid);
        mDirection = (AccountSetupCheckSettings.CheckDirection) getArguments().getSerializable(EXTRA_CHECK_DIRECTION);

        mMakeDefault = getArguments().getBoolean(EXTRA_DEFAULT);
        mProcedence = getArguments().getString(EXTRA_PROCEDENCE);

        new CheckAccountTask(mAccount).execute(mDirection);

        return rootView;
    }

    private void handleCertificateValidationException(CertificateValidationException cve) {
        Timber.e(cve, "Error while testing settings (cve)");

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


    @Override
    public void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
        mCanceled = true;
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

    private void setMessage(final int resId) {
        mMessageView.setText(getString(resId));
    }

    private void acceptKeyDialog(final int msgResId, final CertificateValidationException ex) {
        mHandler.post(new Runnable() {
            public void run() {
                if (mDestroyed) {
                    return;
                }
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

                mProgressBar.setIndeterminate(false);
                StringBuilder chainInfo = new StringBuilder(100);
                MessageDigest sha1 = null;
                try {
                    sha1 = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e) {
                    Timber.e(e, "Error while initializing MessageDigest");
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
                                        Timber.w("SubjectAltName of type OtherName not supported.");
                                        continue;
                                    case 1: // RFC822Name
                                        name = (String)value;
                                        break;
                                    case 2:  // DNSName
                                        name = (String)value;
                                        break;
                                    case 3:
                                        Timber.w("unsupported SubjectAltName of type x400Address");
                                        continue;
                                    case 4:
                                        Timber.w("unsupported SubjectAltName of type directoryName");
                                        continue;
                                    case 5:
                                        Timber.w("unsupported SubjectAltName of type ediPartyName");
                                        continue;
                                    case 6:  // Uri
                                        name = (String)value;
                                        break;
                                    case 7: // ip-address
                                        name = (String)value;
                                        break;
                                    default:
                                        Timber.w("unsupported SubjectAltName of unknown type");
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
                        Timber.w(e1, "cannot display SubjectAltNames in dialog");
                    }

                    chainInfo.append("Issuer: ").append(chain[i].getIssuerDN().toString()).append("\n");
                    if (sha1 != null) {
                        sha1.reset();
                        try {
                            String sha1sum = Hex.encodeHex(sha1.digest(chain[i].getEncoded()));
                            chainInfo.append("Fingerprint (SHA-1): ").append(sha1sum).append("\n");
                        } catch (CertificateEncodingException e) {
                            Timber.e(e,"Error while encoding certificate");
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

    /**
     * Permanently accepts a certificate for the INCOMING or OUTGOING direction
     * by adding it to the local key store.
     *
     * @param certificate
     */
    private void acceptCertificate(X509Certificate certificate) {
        try {
            mAccount.addCertificate(mDirection, certificate);
        } catch (CertificateException e) {
            showErrorDialog(
                    R.string.account_setup_failed_dlg_certificate_message_fmt,
                    e.getMessage() == null ? "" : e.getMessage());
        }
        AccountSetupCheckSettingsFragment accountSetupOutgoingFragment = AccountSetupCheckSettingsFragment.actionCheckSettings(mAccount,
                mDirection, mMakeDefault, AccountSetupCheckSettingsFragment.INCOMING);
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                .replace(R.id.account_setup_container, accountSetupOutgoingFragment, "accountSetupOutgoingFragment")
                .commit();
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        getActivity().setResult(resCode);
        getActivity().finish();
    }

    private void onCancel() {
        mCanceled = true;
        setMessage(R.string.account_setup_check_settings_canceling_msg);
    }

    private void showErrorDialog(final int msgResId, final Object... args) {
        mHandler.post(new Runnable() {
            public void run() {
                showDialogFragment(R.id.dialog_account_setup_error, getString(msgResId, args));
            }
        });
    }

    private void showDialogFragment(int dialogId, String customMessage) {
        if (mDestroyed) {
            return;
        }
        mProgressBar.setIndeterminate(false);

        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                fragment = ConfirmationDialogFragment.newInstance(dialogId,
                        getString(R.string.account_setup_failed_dlg_title),
                        customMessage,
                        getString(R.string.account_setup_failed_dlg_edit_details_action),
                        getString(R.string.account_setup_failed_dlg_continue_action)
                );
                fragment.setTargetFragment(this, 0);
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        FragmentTransaction ta = getFragmentManager().beginTransaction();
        ta.add(fragment, getDialogTag(dialogId));
        ta.commitAllowingStateLoss();

        // TODO: commitAllowingStateLoss() is used to prevent https://code.google.com/p/android/issues/detail?id=23761
        // but is a bad...
        //fragment.show(ta, getDialogTag(dialogId));
    }

    private String getDialogTag(int dialogId) {
        return String.format(Locale.US, "dialog-%d", dialogId);
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

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                if (mDirection.equals(AccountSetupCheckSettings.CheckDirection.INCOMING)) {
                    AccountSetupBasicsFragment accountSetupBasicsFragment = new AccountSetupBasicsFragment();
                    getFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.animator.fade_in_right, R.animator.fade_out_left)
                            .replace(R.id.account_setup_container, accountSetupBasicsFragment, "accountSetupBasicsFragment")
                            .commit();
                } else {
                    AccountSetupOutgoingFragment accountSetupOutgoingFragment = AccountSetupOutgoingFragment.actionOutgoingSettings(mAccount, false);
                    getFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                            .replace(R.id.account_setup_container, accountSetupOutgoingFragment, "accountSetupOutgoingFragment")
                            .commit();
                }

                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_account_setup_error: {
                mCanceled = false;
                getActivity().setResult(RESULT_OK);
                getFragmentManager().popBackStack();
                break;
            }
        }
    }

    @Override
    public void dialogCancelled(int dialogId) {
        // nothing to do here...
    }

    /**
     * FIXME: Don't use an AsyncTask to perform network operations.
     * See also discussion in https://github.com/k9mail/k-9/pull/560
     */
    private class CheckAccountTask extends AsyncTask<AccountSetupCheckSettings.CheckDirection, Integer, Void> {
        private final Account account;

        private CheckAccountTask(Account account) {
            this.account = account;
        }

        @Override
        protected Void doInBackground(AccountSetupCheckSettings.CheckDirection... params) {
            final AccountSetupCheckSettings.CheckDirection direction = params[0];
            try {
                /*
                 * This task could be interrupted at any point, but network operations can block,
                 * so relying on InterruptedException is not enough. Instead, check after
                 * each potentially long-running operation.
                 */
                if (cancelled()) {
                    return null;
                }

                clearCertificateErrorNotifications(direction);

                checkServerSettings(direction);

                if (cancelled()) {
                    return null;
                }
                // TODO: 17/10/16 check this
                if(Intent.ACTION_EDIT.equals(getActivity().getIntent().getAction())) {
                    savePreferences();
                    getActivity().finish();
                } else if (mProcedence.equals(INCOMING)) {
                    goToOutgoingSettings();
                } else if (mProcedence.equals(OUTGOING) || mProcedence.equals(LOGIN) ){
                    savePreferences();
                    getActivity().finish();
                }
            } catch (AuthenticationFailedException afe) {
                Timber.e(afe, "Error while testing settings (auth failed)");
                showErrorDialog(
                        R.string.account_setup_failed_dlg_auth_message_fmt,
                        afe.getMessage() == null ? "" : afe.getMessage());
            } catch (CertificateValidationException cve) {
                handleCertificateValidationException(cve);
            } catch (Exception e) {
                Timber.e(e, "Error while testing settings");
                String message = e.getMessage() == null ? "" : e.getMessage();
                showErrorDialog(R.string.account_setup_failed_dlg_server_message_fmt, message);
            }
            return null;
        }

        private void clearCertificateErrorNotifications(AccountSetupCheckSettings.CheckDirection direction) {
            final MessagingController ctrl = MessagingController.getInstance(getActivity().getApplication());
            ctrl.clearCertificateErrorNotifications(account, direction);
        }

        private boolean cancelled() {
            if (mDestroyed) {
                return true;
            }
            if (mCanceled) {
                getActivity().finish();
                return true;
            }
            return false;
        }

        private void checkServerSettings(AccountSetupCheckSettings.CheckDirection direction) throws MessagingException {
            switch (direction) {
                case INCOMING: {
                    checkIncoming();
                    break;
                }
                case OUTGOING: {
                    checkOutgoing();
                    break;
                }
            }
        }

        private void checkOutgoing() throws MessagingException {
            if (!(account.getRemoteStore() instanceof WebDavStore)) {
                publishProgress(R.string.account_setup_check_settings_check_outgoing_msg);
            }
            Transport transport = TransportProvider.getInstance().getTransport(K9.app, account, K9.oAuth2TokenStore);
            transport.close();
            try {
                transport.open();
            } finally {
                transport.close();
            }
        }

        private void checkIncoming() throws MessagingException {
            Store store = account.getRemoteStore();
            if (store instanceof WebDavStore) {
                publishProgress(R.string.account_setup_check_settings_authenticate);
            } else {
                publishProgress(R.string.account_setup_check_settings_check_incoming_msg);
            }
            store.checkSettings();

            if (store instanceof WebDavStore) {
                publishProgress(R.string.account_setup_check_settings_fetch);
            }
            MessagingController.getInstance(getActivity().getApplication()).listFoldersSynchronous(account, true, null);
            MessagingController.getInstance(getActivity().getApplication())
                    .synchronizeMailbox(account, account.getInboxFolderName(), null, null);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            setMessage(values[0]);
        }
    }

    private void goToOutgoingSettings() {
        AccountSetupOutgoingFragment accountSetupOutgoingFragment = AccountSetupOutgoingFragment.actionOutgoingSettings(mAccount, mMakeDefault);
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                .replace(R.id.account_setup_container, accountSetupOutgoingFragment, "accountSetupOutgoingFragment")
                .commit();
    }

    private void savePreferences() {
        mAccount.setDescription(mAccount.getEmail());
        mAccount.save(Preferences.getPreferences(getActivity()));
        K9.setServicesEnabled(getActivity());
        if (Intent.ACTION_EDIT.equals(getActivity().getIntent().getAction())) {
            getActivity().finish();
        } else {
            AccountSetupNames.actionSetNames(getActivity(), mAccount);
        }
    }
}
