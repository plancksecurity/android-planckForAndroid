package com.fsck.k9.pEp.ui.privacy.status;

import android.content.Intent;

import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.models.mappers.PEpIdentityMapper;
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;

import static com.fsck.k9.pEp.ui.privacy.status.PEpTrustwords.PARTNER_ACTION;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//TODO FIX TESTS
public class PEpStatusPresenterTest {

    @Mock
    SimpleMessageLoaderHelper simpleMessageLoaderHelper;
    @Mock
    PEpStatusView pEpStatusView;
    private PEpStatusPresenter presenter;
    @Mock
    PePUIArtefactCache uiCache;
    @Mock
    PEpProvider provider;
    @Mock
    Address senderAddress;
    @Mock
    Intent intent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PEpIdentityMapper pEpIdentityMapper = new PEpIdentityMapper();
        presenter = new PEpStatusPresenter(simpleMessageLoaderHelper,
                pEpIdentityMapper);
        intent = mock(Intent.class);
        intent.putExtra(PARTNER_ACTION, PEpProvider.TrustAction.TRUST);
    }

    @Test
    public void shouldStartMessageLoaderWhenLoadMessage() {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.loadMessage(new MessageReference("", "", "", null));

        verify(simpleMessageLoaderHelper).asyncStartOrResumeLoadingMessage(
                any(MessageReference.class), any(MessageLoaderHelper.MessageLoaderCallbacks.class)
        );
    }

    @Test
    public void shouldGetRecipientsFromCacheWhenLoadRecipients() {
        when(uiCache.getRecipients()).thenReturn(recipients());
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.loadRecipients();

        verify(pEpStatusView).setupRecipients(any());
    }

    @Test
    public void shouldShowPEpTextsWhenLoadRating() {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.loadRating(Rating.pEpRatingReliable);

        verify(pEpStatusView).showPEpTexts(anyString(), anyString());
    }

    @Test
    public void shouldHideBadgeWhenLoadNullRating() {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.loadRating(null);

        verify(pEpStatusView).hideBadge();
    }

    @Test
    public void shouldExtractRatingWhenOnResult() {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        Intent data = new Intent();
        data.putExtra(PARTNER_ACTION, PEpProvider.TrustAction.TRUST);
        presenter.onResult(data);

        verify(provider).incomingMessageRating(any());
    }

    @Test
    public void shouldGetRecipientsWhenOnResult() {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.onResult(new Intent());

        verify(uiCache).getRecipients();
    }

    @Test
    public void shouldUpdateViewOnResult() {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);
        when(uiCache.getRecipients()).thenReturn(recipients());

        presenter.onResult(new Intent());

        verify(pEpStatusView).updateIdentities(any());
    }

    @Test
    public void shouldGetRatingWhenSetupOutgoingMessageRating() {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);
        when(uiCache.getRecipients()).thenReturn(recipients());

        presenter.onResult(new Intent());

        verify(provider).getRating(any(), any(), any(), any(), any());
    }

    private ArrayList<Identity> recipients() {
        return new ArrayList<>();
    }
}