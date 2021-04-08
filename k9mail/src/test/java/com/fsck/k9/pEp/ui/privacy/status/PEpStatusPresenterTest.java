package com.fsck.k9.pEp.ui.privacy.status;

import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.models.mappers.PEpIdentityMapper;
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PEpStatusPresenterTest {

    @Mock SimpleMessageLoaderHelper simpleMessageLoaderHelper;
    @Mock PEpStatusView pEpStatusView;
    private PEpStatusPresenter presenter;
    private PEpStatusPresenter presenterSpy;
    @Mock PePUIArtefactCache uiCache;
    @Mock PEpProvider provider;
    @Mock Address senderAddress;
    @Mock
    DisplayHtml displayHtml;
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PEpIdentityMapper pEpIdentityMapper = new PEpIdentityMapper(provider);
        presenter = new PEpStatusPresenter(simpleMessageLoaderHelper,
                pEpIdentityMapper);
        presenterSpy = spy(presenter);
    }

    @Test
    public void shouldStartMessageLoaderWhenLoadMessage() throws Exception {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, false, senderAddress, forceUnencrypted, alwaysSecure);

        presenter.loadMessage(new MessageReference("", "", "", null));

        verify(simpleMessageLoaderHelper).asyncStartOrResumeLoadingMessage(
                any(MessageReference.class), any(MessageLoaderHelper.MessageLoaderCallbacks.class),
                displayHtml
        );
    }

    @Test
    public void shouldNotGetRecipientsFromCacheWhenLoadEmptyRecipientsList() throws Exception {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        when(uiCache.getRecipients()).thenReturn(emptyRecipients());
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, false, senderAddress, forceUnencrypted, alwaysSecure);

        presenter.loadRecipients();
        verify(pEpStatusView, never()).setupRecipients(anyList());
        verify(pEpStatusView).showItsOnlyOwnMsg();
    }

    @Test
    public void shouldGetRecipientsFromCacheWhenLoadRecipients() throws Exception {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        when(uiCache.getRecipients()).thenReturn(recipients());
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, false, senderAddress, forceUnencrypted, alwaysSecure);

        presenter.loadRecipients();
        verify(pEpStatusView).setupRecipients(anyList());
    }

    /*@Test
    public void shouldShowPEpTextsWhenLoadRating() throws Exception {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress, forceUnencrypted, alwaysSecure);

        presenter.loadRating(Rating.pEpRatingReliable);

        verify(pEpStatusView).showPEpTexts(anyString(), anyString());
    }

    @Test
    public void shouldHideBadgeWhenLoadNullRating() throws Exception {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        presenter.initilize(pEpStatusView, uiCache, provider, false, senderAddress, forceUnencrypted, alwaysSecure);

        presenter.loadRating(null);

        verify(pEpStatusView).hideBadge();
    }*/

    @Test
    public void shouldExtractRatingWhenOnHandshakeResult() throws Exception {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, true, senderAddress, forceUnencrypted, alwaysSecure);
        Identity identity = new Identity();
        presenter.onHandshakeResult(identity, false);
        PEpProvider.SimpleResultCallback<Rating> callback = mock(PEpProvider.SimpleResultCallback.class);
        verify(provider).incomingMessageRating(any(), callback);
    }

    @Test
    public void shouldGetRecipientsWhenOnHandshakeResult() throws Exception {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, false, senderAddress, forceUnencrypted, alwaysSecure);
        Identity identity = new Identity();
        presenter.onHandshakeResult(identity, false);

        verify(uiCache).getRecipients();
    }

    @Test
    public void shouldUpdateViewOnHandshakeResult() throws Exception {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, false, senderAddress, forceUnencrypted, alwaysSecure);

        Identity identity = new Identity();
        presenter.onHandshakeResult(identity, false);

        verify(pEpStatusView).updateIdentities(any());
    }

    /*@Test
    public void shouldGetRatingWhenSetupOutgoingMessageRating() throws Exception {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        presenterSpy.initilize(pEpStatusView, uiCache, provider, false, senderAddress, forceUnencrypted, alwaysSecure);
        when(uiCache.getRecipients()).thenReturn(recipients());


        presenterSpy.onHandshakeResult(recipients().get(0), true);
        when(presenterSpy.getRecipientAddresses()).thenReturn(addressList());
        verify(provider).getRating(senderAddress, addressList(), addressList(), addressList(), any());
    }*/

    private ArrayList<Identity> emptyRecipients() {
        return new ArrayList<>();
    }

    private ArrayList<Identity> recipients() {
        ArrayList<Identity> identities = new ArrayList<>();
        identities.add(new Identity());
        return identities;
    }

    private ArrayList<Address> addressList() {
        ArrayList<Address> adresses = new ArrayList<>();
        adresses.add(new Address("pepe"));
        return adresses;
    }
}