package com.fsck.k9.pEp.ui.privacy.status;

import android.content.Intent;
import android.content.IntentSender;

import com.fsck.k9.R;
import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.infrastructure.Presenter;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.models.mappers.PEpIdentityMapper;
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class PEpStatusPresenter implements Presenter {

    private final SimpleMessageLoaderHelper simpleMessageLoaderHelper;
    private final PEpUtils pEpUtils;
    private final PEpIdentityMapper pEpIdentityMapper;
    private PEpStatusView view;
    private PePUIArtefactCache cache;
    private PEpProvider pEpProvider;
    private List<PEpIdentity> identities;
    private LocalMessage localMessage;
    private boolean isMessageIncoming;
    private Address senderAddress;

    @Inject
    public PEpStatusPresenter(SimpleMessageLoaderHelper simpleMessageLoaderHelper, PEpUtils pEpUtils, PEpIdentityMapper pEpIdentityMapper) {
        this.simpleMessageLoaderHelper = simpleMessageLoaderHelper;
        this.pEpUtils = pEpUtils;
        this.pEpIdentityMapper = pEpIdentityMapper;
    }

    public void initilize(PEpStatusView pEpStatusView, PePUIArtefactCache uiCache, PEpProvider pEpProvider, boolean isMessageIncoming, Address senderAddress) {
        this.view = pEpStatusView;
        this.cache = uiCache;
        this.pEpProvider = pEpProvider;
        this.isMessageIncoming = isMessageIncoming;
        this.senderAddress = senderAddress;
        pEpIdentityMapper.initialize(pEpProvider);
    }

    public void loadMessage(MessageReference messageReference) {
        if (messageReference != null) {
            simpleMessageLoaderHelper.asyncStartOrResumeLoadingMessage(messageReference, callback());
        }
    }

    public void loadRecipients() {
        List<Identity> recipients = cache.getRecipients();
        identities = pEpIdentityMapper.mapRecipients(recipients);
        view.setupRecipients(identities);
    }

    public void resetRecipientTrust(int position) {
        Identity id = identities.get(position);
        pEpProvider.resetTrust(id, new PEpProvider.CompletedCallback() {
            @Override
            public void onComplete() {
                List<PEpIdentity> updatedIdentities = updateRecipients(identities, id);
                if (isMessageIncoming) {
                    onRatingChanged(Rating.pEpRatingReliable);
                } else {
                    setupOutgoingMessageRating();
                }
                view.updateIdentities(updatedIdentities);
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });

    }

    private void setupOutgoingMessageRating() {
        List<Address> addresses = new ArrayList<>(identities.size());
        for (PEpIdentity identity : identities) {
            addresses.add(new Address(identity.address));
        }
        pEpProvider.getPrivacyState(senderAddress, addresses, Collections.emptyList(), Collections.emptyList(), new PEpProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating rating) {
                onRatingChanged(rating);
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    private void onRatingChanged(Rating rating) {
        if (localMessage != null) {
            localMessage.setpEpRating(rating);
            localMessage.setHeader(MimeHeader.HEADER_PEP_RATING, pEpUtils.ratingToString(rating));
            view.saveLocalMessage(localMessage);
        }
        view.setRating(rating);
        view.setupBackIntent(rating);
    }

    public void loadPepTexts(Rating rating) {
        view.showPEpTexts(cache.getTitle(rating), cache.getSuggestion(rating));
    }

    public void onResult(int position) {
        ArrayList<Identity> recipients = cache.getRecipients();
        Identity partner = recipients.get(position);
        Rating pEpRating = pEpProvider.identityRating(partner);
        identities = pEpIdentityMapper.mapRecipients(recipients);
        view.updateIdentities(identities);
        if (isMessageIncoming) {
            Rating rating = pEpProvider.identityRating(senderAddress);
            onRatingChanged(rating);
        } else {
            setupOutgoingMessageRating();
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
        pEpIdentity.setRating(pEpProvider.identityRating(recipient));
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
            }

            @Override
            public void onMessageDataLoadFailed() {
                view.showError(R.string.status_loading_error);
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
}
