package com.fsck.k9.pEp.ui.privacy.status;

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
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PEpStatusPresenterTest {

    @Mock SimpleMessageLoaderHelper simpleMessageLoaderHelper;
    @Mock PEpStatusView pEpStatusView;
    private PEpStatusPresenter presenter;
    @Mock PePUIArtefactCache uiCache;
    @Mock PEpProvider provider;
    @Mock Address senderAddress;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PEpIdentityMapper pEpIdentityMapper = new PEpIdentityMapper();
        presenter = new PEpStatusPresenter(simpleMessageLoaderHelper,
                pEpIdentityMapper);
    }

    @Test
    public void shouldStartMessageLoaderWhenLoadMessage() throws Exception {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.loadMessage(new MessageReference("","","", null));

        verify(simpleMessageLoaderHelper).asyncStartOrResumeLoadingMessage(
                any(MessageReference.class), any(MessageLoaderHelper.MessageLoaderCallbacks.class)
        );
    }

    @Test
    public void shouldGetRecipientsFromCacheWhenLoadRecipients() throws Exception {
        when(uiCache.getRecipients()).thenReturn(recipients());
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.loadRecipients();

        verify(pEpStatusView).setupRecipients(any());
    }

    @Test
    public void shouldShowPEpTextsWhenLoadRating() throws Exception {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.loadRating(Rating.pEpRatingReliable);

        verify(pEpStatusView).showPEpTexts(anyString(), anyString());
    }

    @Test
    public void shouldHideBadgeWhenLoadNullRating() throws Exception {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.loadRating(null);

        verify(pEpStatusView).hideBadge();
    }

    @Test
    public void shouldExtractRatingWhenOnResult() throws Exception {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.onResult();

        verify(provider).incomingMessageRating(any());
    }

    @Test
    public void shouldGetRecipientsWhenOnResult() throws Exception {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);

        presenter.onResult();

        verify(uiCache).getRecipients();
    }

    @Test
    public void shouldUpdateViewOnResult() throws Exception {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);
        when(uiCache.getRecipients()).thenReturn(recipients());

        presenter.onResult();

        verify(pEpStatusView).updateIdentities(any());
    }

    @Test
    public void shouldGetRatingWhenSetupOutgoingMessageRating() throws Exception {
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress);
        when(uiCache.getRecipients()).thenReturn(recipients());

        presenter.onResult();

        verify(provider).getRating(any(), any(), any(), any(), any());
    }

    private ArrayList<Identity> recipients() {
        return new ArrayList<>();
    }
}