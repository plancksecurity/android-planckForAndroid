package com.fsck.k9.planck.ui.privacy.status;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.models.PlanckIdentity;
import com.fsck.k9.planck.models.mappers.PlanckIdentityMapper;
import com.fsck.k9.planck.ui.SimpleMessageLoaderHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;

public class PlanckStatusPresenter {

    private static final String STATE_FORCE_UNENCRYPTED = "forceUnencrypted";
    private static final String STATE_ALWAYS_SECURE = "alwaysSecure";
    private static final int LOAD_RECIPIENTS = 1, ON_TRUST_RESET = 2, UPDATE_IDENTITIES = 3;
    private final SimpleMessageLoaderHelper simpleMessageLoaderHelper;
    private final PlanckIdentityMapper planckIdentityMapper;
    private PlanckStatusView view;
    private PlanckUIArtefactCache cache;
    private PlanckProvider planckProvider;
    private List<PlanckIdentity> identities;
    private LocalMessage localMessage;
    private boolean isMessageIncoming;
    private Address senderAddress;
    private Rating currentRating;
    private Identity latestHandshakeId;
    private boolean forceUnencrypted = false;
    private boolean isAlwaysSecure = false;
    private DisplayHtml displayHtml;

    private Handler mainThreadHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            List<PlanckIdentity> newIdentities = (List<PlanckIdentity>) msg.obj;
            switch (msg.what) {
                case ON_TRUST_RESET:
                    trustWasReset(newIdentities, Rating.getByInt(msg.arg1));
                    break;
                case UPDATE_IDENTITIES:
                    identitiesUpdated(newIdentities);
                    break;
                case LOAD_RECIPIENTS:
                    recipientsLoaded(newIdentities);
                    break;
            }
        }
    };

    @Inject
    PlanckStatusPresenter(SimpleMessageLoaderHelper simpleMessageLoaderHelper, PlanckIdentityMapper planckIdentityMapper) {
        this.simpleMessageLoaderHelper = simpleMessageLoaderHelper;
        this.planckIdentityMapper = planckIdentityMapper;
    }

    void initialize(PlanckStatusView planckStatusView, PlanckUIArtefactCache uiCache, PlanckProvider planckProvider,
                    DisplayHtml displayHtml, boolean isMessageIncoming, Address senderAddress,
                    boolean forceUnencrypted, boolean alwaysSecure) {
        this.view = planckStatusView;
        this.cache = uiCache;
        this.planckProvider = planckProvider;
        this.displayHtml = displayHtml;
        this.isMessageIncoming = isMessageIncoming;
        this.senderAddress = senderAddress;
        this.forceUnencrypted = forceUnencrypted;
        this.isAlwaysSecure = alwaysSecure;
    }

    void loadMessage(MessageReference messageReference) {
        if (messageReference != null) {
            simpleMessageLoaderHelper.asyncStartOrResumeLoadingMessage(messageReference, callback(), displayHtml);
        }
    }

    void loadRecipients() {
        List<Identity> recipients = cache.getRecipients();
        WorkerThread workerThread = new WorkerThread(recipients, LOAD_RECIPIENTS);
        workerThread.start();
    }

    private void recipientsLoaded(List<PlanckIdentity> newIdentities) {
        identities = newIdentities;
        if (!identities.isEmpty()) {
            view.setupRecipients(identities);
        } else {
            view.showItsOnlyOwnMsg();
        }
    }

    private void resetTrust(Identity id) {
        if (isMessageIncoming) {
            resetIncomingMessageTrust(id);
        } else {
            List<Address> addresses = getRecipientAddresses();
            resetOutgoingMessageTrust(id, addresses);
        }
    }

    private void resetOutgoingMessageTrust(Identity id, List<Address> addresses) {
        planckProvider.loadOutgoingMessageRatingAfterResetTrust(id, senderAddress, addresses, Collections.emptyList(), Collections.emptyList(), new PlanckProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating rating) {
                onTrustReset(rating, id);
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    private void onTrustReset(Rating rating, Identity id) {
        WorkerThread workerThread = new WorkerThread(identities, ON_TRUST_RESET, rating);
        workerThread.start();
    }

    private void trustWasReset(List<PlanckIdentity> newIdentities, Rating rating) {
        onRatingChanged(rating);
        view.updateIdentities(newIdentities);
    }

    private void resetIncomingMessageTrust(Identity id) {
        planckProvider.loadMessageRatingAfterResetTrust(localMessage, isMessageIncoming, id, new PlanckProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating result) {
                onTrustReset(result, id);
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    private void setupOutgoingMessageRating(PlanckProvider.ResultCallback<Rating> callback) {
        List<Address> addresses = getRecipientAddresses();
        planckProvider.getRating(senderAddress, addresses, Collections.emptyList(),
                Collections.emptyList(), callback);
    }

    @NonNull
    List<Address> getRecipientAddresses() {
        List<Address> addresses = new ArrayList<>(identities.size());
        for (PlanckIdentity identity : identities) {
            addresses.add(new Address(identity.address));
        }
        return addresses;
    }

    private void onRatingChanged(Rating rating) {
        this.currentRating = rating;
        if (localMessage != null) {
            localMessage.setPlanckRating(rating);
        }
        view.setupBackIntent(rating, forceUnencrypted, isAlwaysSecure);
    }

    void onHandshakeResult(Identity id, boolean trust) {
        latestHandshakeId = id;
        refreshRating(new PlanckProvider.SimpleResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating rating) {
                onRatingChanged(rating);
                if (trust) {
                    showUndoAction(PlanckProvider.TrustAction.TRUST);
                } else {
                    view.showMistrustFeedback(latestHandshakeId.username);
                }
                updateIdentities();
            }
        });
    }

    public void resetpEpData(Identity id) {
        try {
            planckProvider.keyResetIdentity(id, null);
            refreshRating(new PlanckProvider.SimpleResultCallback<Rating>() {
                @Override
                public void onLoaded(Rating rating) {
                    onRatingChanged(rating);
                    onTrustReset(currentRating, id);
                }
            });

            view.showResetPartnerKeySuccessFeedback();
        } catch (Exception e) {
            view.showResetPartnerKeyErrorFeedback();
        }

    }

    private void refreshRating(PlanckProvider.ResultCallback<Rating> callback) {
        if (isMessageIncoming) {
            planckProvider.incomingMessageRating(localMessage, callback);
        } else {
            setupOutgoingMessageRating(callback);
        }
    }

    private void updateIdentities() {
        ArrayList<Identity> recipients = cache.getRecipients();
        WorkerThread workerThread = new WorkerThread(recipients, UPDATE_IDENTITIES);
        workerThread.start();
    }

    private void identitiesUpdated(List<PlanckIdentity> newIdentities) {
        identities = newIdentities;
        view.updateIdentities(identities);
    }

    private void showUndoAction(PlanckProvider.TrustAction trustAction) {
        switch (trustAction) {
            case TRUST:
                view.showUndoTrust(latestHandshakeId.username);
                break;
            case MISTRUST:
                view.showUndoMistrust(latestHandshakeId.username);
                break;
        }
    }

    public MessageLoaderHelper.MessageLoaderCallbacks callback() {
        return new MessageLoaderHelper.MessageLoaderCallbacks() {
            @Override
            public void onMessageDataLoadFinished(LocalMessage message) {
                localMessage = message;
                currentRating = localMessage.getPlanckRating();
            }

            @Override
            public void onMessageDataLoadFailed() {
                view.showDataLoadError();
            }

            @Override
            public void onMessageViewInfoLoadFinished(MessageViewInfo messageViewInfo) {
            }

            @Override
            public void onMessageViewInfoLoadFailed(MessageViewInfo messageViewInfo) {
            }

            @Override
            public void setLoadingProgress(int current, int max) {
            }

            @Override
            public void onDownloadErrorMessageNotFound() {
            }

            @Override
            public void onDownloadErrorNetworkError() {
            }

            @Override
            public void startIntentSenderForMessageLoaderHelper(IntentSender si, int requestCode, Intent fillIntent,
                                                                int flagsMask, int flagValues, int extraFlags) {
            }
        };
    }

    public void undoTrust() {
        if (latestHandshakeId != null) {
            resetTrust(latestHandshakeId);
        }
    }

    public void saveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_FORCE_UNENCRYPTED, forceUnencrypted);
        outState.putBoolean(STATE_ALWAYS_SECURE, isAlwaysSecure);
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            forceUnencrypted = savedInstanceState.getBoolean(STATE_FORCE_UNENCRYPTED);
            isAlwaysSecure = savedInstanceState.getBoolean(STATE_ALWAYS_SECURE);
        }
    }

    public boolean isForceUnencrypted() {
        return forceUnencrypted;
    }

    public void setForceUnencrypted(boolean forceUnencrypted) {
        this.forceUnencrypted = forceUnencrypted;
        view.setupBackIntent(currentRating, forceUnencrypted, isAlwaysSecure);
    }

    public boolean isAlwaysSecure() {
        return isAlwaysSecure;
    }

    public void setAlwaysSecure(boolean alwaysSecure) {
        isAlwaysSecure = alwaysSecure;
        view.setupBackIntent(currentRating, forceUnencrypted, alwaysSecure);
    }

    private class WorkerThread extends Thread {

        private List<Identity> identities;
        private int what;
        private Rating rating;

        public WorkerThread(List<Identity> identities, int what) {
            this.identities = identities;
            this.what = what;
        }

        public WorkerThread(List<PlanckIdentity> identities, int what, Rating rating) {
            this.identities = new ArrayList<>(identities);
            this.what = what;
            this.rating = rating;
        }

        @Override
        public void run() {
            List<PlanckIdentity> updatedIdentities = planckIdentityMapper.mapRecipients(identities);
            Message childThreadMessage = new Message();
            childThreadMessage.what = what;
            childThreadMessage.obj = updatedIdentities;
            if (rating != null) {
                childThreadMessage.arg1 = rating.value;
            }
            mainThreadHandler.sendMessage(childThreadMessage);
        }
    }

}
