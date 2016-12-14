package com.fsck.k9.pEp;

import android.content.Intent;
import android.content.IntentSender;

import com.fsck.k9.R;
import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.pEp.infrastructure.Presenter;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.models.mappers.PEpIdentityMapper;
import com.fsck.k9.pEp.ui.PEpStatusView;
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

    public void updateTrust(int position) {
        Identity id = identities.get(position);
        id = pEpProvider.updateIdentity(id);
        pEpProvider.resetTrust(id);
        List<PEpIdentity> updatedIdentities = updateRecipients(identities);
        if (isMessageIncoming) {
            onRatingChanged(Rating.pEpRatingReliable);
        } else {
            List<Address> addresses = new ArrayList<>(identities.size());
            for (PEpIdentity identity : identities) {
                addresses.add(new Address(identity.address));
            }
            Rating privacyState = pEpProvider.getPrivacyState(senderAddress, addresses, Collections.emptyList(), Collections.emptyList());
            onRatingChanged(privacyState);
        }
        view.updateIdentities(updatedIdentities);
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
            List<Address> addresses = new ArrayList<>(identities.size());
            for (PEpIdentity identity : identities) {
                addresses.add(new Address(identity.address));
            }
            Rating privacyState = pEpProvider.getPrivacyState(senderAddress, addresses, Collections.emptyList(), Collections.emptyList());
            onRatingChanged(privacyState);
        }
    }

    private List<PEpIdentity> updateRecipients(List<PEpIdentity> identities) {
        ArrayList<PEpIdentity> pEpIdentities = new ArrayList<>(identities.size());
        for (Identity recipient : identities) {
            pEpIdentities.add(updateRecipient(recipient));
        }
        return pEpIdentities;
    }

    private PEpIdentity updateRecipient(Identity recipient) {
        PEpIdentity pEpIdentity = new PEpIdentity();
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
