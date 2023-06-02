package com.fsck.k9.planck.ui.fragments;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.planck.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.planck.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.planck.infrastructure.threading.UIThread;
import com.fsck.k9.planck.ui.infrastructure.exceptions.PlanckAuthenticationException;
import com.fsck.k9.planck.ui.infrastructure.exceptions.PlanckCertificateException;
import com.fsck.k9.planck.ui.infrastructure.exceptions.PlanckMessagingException;
import com.fsck.k9.planck.ui.infrastructure.exceptions.PlanckSetupException;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class PlanckSettingsCheck implements PlanckSettingsChecker {
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
    private PlanckSettingsChecker.ResultCallback<PlanckSettingsChecker.Redirection> callback;
    private Boolean isEditing;

    @Inject public PlanckSettingsCheck(Context context, ThreadExecutor threadExecutor, UIThread uiThread) {
        this.context = context;
        this.threadExecutor = threadExecutor;
        this.postExecutionThread = uiThread;
    }

    @Override
    public void checkSettings(Account account,
                              AccountSetupCheckSettings.CheckDirection checkDirection,
                              Boolean makeDefault, String procedence, Boolean isEditing,
                              ResultCallback<Redirection> callback) {
        this.account = account;
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
                    notifyLoaded(PlanckSettingsChecker.Redirection.TO_APP);
                } else if (this.procedence.equals(INCOMING)) {
                    notifyLoaded(PlanckSettingsChecker.Redirection.OUTGOING);
                } else if (this.procedence.equals(OUTGOING)){
                    account.setDescription(account.getEmail());
                    notifyLoaded(PlanckSettingsChecker.Redirection.TO_APP);
                } else {
                    account.setDescription(account.getEmail());
                    if(direction.equals(AccountSetupCheckSettings.CheckDirection.INCOMING)) {
                        notifyLoaded(Redirection.OUTGOING);
                    } else {
                        notifyLoaded(PlanckSettingsChecker.Redirection.TO_APP);
                    }
                }
            } catch (AuthenticationFailedException exception) {
                onError(new PlanckAuthenticationException(exception));
            } catch (CertificateValidationException exception) {
                onError(new PlanckCertificateException(exception));
            } catch (MessagingException exception) {
                if (!Utility.hasConnectivity(context)) {
                    exception = new MessagingException(context.getString(R.string.device_offline_warning));
                    onError(new PlanckMessagingException(exception));
                } else {
                    Timber.d(K9.LOG_TAG, "Error while testing settings", exception);
                    String message = exception.getMessage() == null ? "" : exception.getMessage();
                    exception = new MessagingException(message);
                    onError(new PlanckMessagingException(exception));
                }
            }
        });
    }

    private void onError(PlanckSetupException exception) {
        exception.direction = direction;
        this.postExecutionThread.post(() -> callback.onError(exception));
    }

    private void notifyLoaded(PlanckSettingsChecker.Redirection redirection) {
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
        Transport transport = TransportProvider.getInstance().getTransport(K9.app, account);
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
    }

    private void savePreferences() {
        account.setDescription(account.getEmail());
        account.save(Preferences.getPreferences(context));
        K9.setServicesEnabled(context);
    }
}
