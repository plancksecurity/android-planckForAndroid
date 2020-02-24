package com.fsck.k9.pEp.ui.privacy.status;

import android.content.Intent;
import android.content.IntentSender;

import androidx.annotation.NonNull;

import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.infrastructure.Presenter;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.models.mappers.PEpIdentityMapper;
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class PEpStatusPresenter implements Presenter {

    private final SimpleMessageLoaderHelper simpleMessageLoaderHelper;
    private final PEpIdentityMapper pEpIdentityMapper;
    private PEpStatusView view;
    private PePUIArtefactCache cache;
    private PEpProvider pEpProvider;
    private List<PEpIdentity> identities;
    private LocalMessage localMessage;
    private boolean isMessageIncoming;
    private Address senderAddress;
    private Rating currentRating;
    private Identity latestHandshakeId;

    @Inject
    PEpStatusPresenter(SimpleMessageLoaderHelper simpleMessageLoaderHelper, PEpIdentityMapper pEpIdentityMapper) {
        this.simpleMessageLoaderHelper = simpleMessageLoaderHelper;
        this.pEpIdentityMapper = pEpIdentityMapper;
    }

    void initilize(PEpStatusView pEpStatusView, PePUIArtefactCache uiCache, PEpProvider pEpProvider, boolean isMessageIncoming, Address senderAddress) {
        this.view = pEpStatusView;
        this.cache = uiCache;
        this.pEpProvider = pEpProvider;
        this.isMessageIncoming = isMessageIncoming;
        this.senderAddress = senderAddress;
        pEpIdentityMapper.initialize(pEpProvider);
    }

    void loadMessage(MessageReference messageReference) {
        if (messageReference != null) {
            simpleMessageLoaderHelper.asyncStartOrResumeLoadingMessage(messageReference, callback());
        }
    }

    void loadRecipients() {
        List<Identity> recipients = cache.getRecipients();
        identities = pEpIdentityMapper.mapRecipients(recipients);
        view.setupRecipients(identities);
    }

    void resetRecipientTrust(Identity id) {
        resetTrust(id);
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
        pEpProvider.loadOutgoingMessageRatingAfterResetTrust(id, senderAddress, addresses, Collections.emptyList(), Collections.emptyList(), new PEpProvider.ResultCallback<Rating>() {
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
        List<PEpIdentity> updatedIdentities = updateRecipients(identities, id);
        onRatingChanged(rating);
        view.updateIdentities(updatedIdentities);
    }

    private void resetIncomingMessageTrust(Identity id) {
        pEpProvider.loadMessageRatingAfterResetTrust(localMessage, isMessageIncoming, id, new PEpProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating result) {
                onTrustReset(result, id);
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    private void setupOutgoingMessageRating() {
        List<Address> addresses = getRecipientAddresses();
        pEpProvider.getRating(senderAddress, addresses, Collections.emptyList(), Collections.emptyList(), new PEpProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating rating) {
                onRatingChanged(rating);
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    @NonNull
    private List<Address> getRecipientAddresses() {
        List<Address> addresses = new ArrayList<>(identities.size());
        for (PEpIdentity identity : identities) {
            addresses.add(new Address(identity.address));
        }
        return addresses;
    }

    private void onRatingChanged(Rating rating) {
        this.currentRating = rating;
        if (localMessage != null) {
            localMessage.setpEpRating(rating);
        }
        view.setRating(rating);
        view.setupBackIntent(rating);
    }

    void loadRating(Rating rating) {
        view.showPEpTexts(cache.getTitle(rating), cache.getSuggestion(rating));
        if (rating == null) {
            view.hideBadge();
        } else {
            view.showBadge(rating);
        }
    }

    void onResult(Intent data) {
        latestHandshakeId = ((Identity) data.getSerializableExtra(PEpTrustwords.PARTNER_DATA));
        PEpProvider.TrustAction trustAction = ((PEpProvider.TrustAction) data.getSerializableExtra(PEpTrustwords.PARTNER_ACTION));

        updateIdentities();

        refreshRating();

        showUndoAction(trustAction);
    }

    private void updateIdentities() {
        ArrayList<Identity> recipients = cache.getRecipients();
        identities = pEpIdentityMapper.mapRecipients(recipients);
        view.updateIdentities(identities);
    }

    private void refreshRating() {
        if (isMessageIncoming) {
            Rating rating = pEpProvider.incomingMessageRating(localMessage);
            onRatingChanged(rating);
        } else {
            setupOutgoingMessageRating();
        }
    }

    private void showUndoAction(PEpProvider.TrustAction trustAction) {
        switch (trustAction) {
            case TRUST:
                view.showUndoTrust(latestHandshakeId.username);
                break;
            case MISTRUST:
                view.showUndoMistrust(latestHandshakeId.username);
                break;
        }
    }

    private List<PEpIdentity> updateRecipients(List<PEpIdentity> identities, Identity id) {
        ArrayList<PEpIdentity> pEpIdentities = new ArrayList<>(identities.size());
        for (Identity recipient : identities) {
            pEpIdentities.add(updateRecipient(recipient, id));
        }
        return pEpIdentities;
    }

    private PEpIdentity updateRecipient(Identity recipient, Identity id) {
        PEpIdentity pEpIdentity = pEpIdentityMapper.mapRecipient(recipient);
        pEpIdentity.setRating(pEpProvider.getRating(recipient));
        return pEpIdentity;
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {

    }

    public MessageLoaderHelper.MessageLoaderCallbacks callback() {
        return new MessageLoaderHelper.MessageLoaderCallbacks() {
            @Override
            public void onMessageDataLoadFinished(LocalMessage message) {
                localMessage = message;
                currentRating = localMessage.getpEpRating();
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

    public void resetpEpData(Identity id) {
//            resetIncomingMessageTrust(id);
        pEpProvider.keyResetIdentity(id, null);
        //Rating rating = pEpProvider.incomingMessageRating(localMessage);
        refreshRating();
        //onTrustReset(currentRating, id);
        view.showResetpEpDataFeedback();
    //    view.finish();
    }

    public void undoTrust() {
        if (latestHandshakeId != null) {
            resetTrust(latestHandshakeId);
        }
    }
}
