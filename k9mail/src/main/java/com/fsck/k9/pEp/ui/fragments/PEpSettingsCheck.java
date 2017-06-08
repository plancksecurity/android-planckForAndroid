package com.fsck.k9.pEp.ui.fragments;

import android.content.Context;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.account.AndroidAccountOAuth2TokenStore;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.pEp.infrastructure.threading.JobExecutor;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.pEp.infrastructure.threading.UIThread;

import javax.inject.Inject;

public class PEpSettingsCheck implements PEpSettingsChecker {
    public static final String INCOMING = "INCOMING";
    public static final String OUTGOING = "OUTGOING";
    public static final String LOGIN = "LOGIN";

    private final Context context;

    private ThreadExecutor threadExecutor;
    private PostExecutionThread postExecutionThread;

    private Account account;
    private AccountSetupCheckSettings.CheckDirection direction;
    private Boolean makeDefault;
    private String procedence;
    private PEpSettingsChecker.ResultCallback<PEpSettingsChecker.Redirection> callback;
    private Boolean isEditing;

    @Inject public PEpSettingsCheck(Context context) {
        this.context = context;
    }

    @Override
    public void checkSettings(String accountUuid,
                              AccountSetupCheckSettings.CheckDirection checkDirection,
                              Boolean makeDefault, String procedence, Boolean isEditing,
                              ResultCallback<Redirection> callback) {
        this.threadExecutor = new JobExecutor();
        this.postExecutionThread = new UIThread();
        this.account = Preferences.getPreferences(context).getAccount(accountUuid);
        this.direction = checkDirection;
        this.makeDefault = makeDefault;
        this.procedence = procedence;
        this.isEditing = isEditing;
        this.callback = callback;

        threadExecutor.execute(() -> {
            try {

                clearCertificateErrorNotifications(direction);

                checkServerSettings(direction);

                if(isEditing) {
                    savePreferences();
                    notifyLoaded(PEpSettingsChecker.Redirection.TO_APP);
                } else if (this.procedence.equals(INCOMING)) {
                    notifyLoaded(PEpSettingsChecker.Redirection.OUTGOING);
                } else if (this.procedence.equals(OUTGOING) || this.procedence.equals(LOGIN) ){
                    savePreferences();
                    notifyLoaded(PEpSettingsChecker.Redirection.TO_APP);
                }
            } catch (AuthenticationFailedException afe) {
                Log.e(K9.LOG_TAG, "Error while testing settings (auth failed)", afe);
                onError(afe.getMessage() == null ? "" : afe.getMessage());
            } catch (CertificateValidationException cve) {
                //TODO handleCertificateValidationException(cve);
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Error while testing settings", e);
                String message = e.getMessage() == null ? "" : e.getMessage();
                onError(message);
            }
        });
    }

    private void onError(String customMessage) {
        this.postExecutionThread.post(() -> callback.onError(customMessage));
    }

    private void notifyLoaded(PEpSettingsChecker.Redirection redirection) {
        this.postExecutionThread.post(() -> callback.onLoaded(redirection));
    }

    private void clearCertificateErrorNotifications(AccountSetupCheckSettings.CheckDirection direction) {
        final MessagingController ctrl = MessagingController.getInstance(context);
        ctrl.clearCertificateErrorNotifications(account, direction);
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
        // TODO: 07/06/17 CHECK if this checker can/should be used on other app places like AccountSetupCheckSettings
        Transport transport = TransportProvider.getInstance().getInstance(K9.app, account, K9.oAuth2TokenStore);
        transport.close();
        try {
            transport.open();
        } finally {
            transport.close();
        }
    }

    private void checkIncoming() throws MessagingException {
        Store store = account.getRemoteStore();
        store.checkSettings();

        MessagingController.getInstance(context).listFoldersSynchronous(account, true, null);
        MessagingController.getInstance(context)
                .synchronizeMailbox(account, account.getInboxFolderName(), null, null);
    }

    private void savePreferences() {
        account.setDescription(account.getEmail());
        account.save(Preferences.getPreferences(context));
        K9.setServicesEnabled(context);
    }
}
