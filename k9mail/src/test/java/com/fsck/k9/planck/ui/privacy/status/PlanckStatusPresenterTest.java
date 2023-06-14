package com.fsck.k9.planck.ui.privacy.status;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Looper;

import com.fsck.k9.RobolectricTest;
import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.planck.models.PlanckIdentity;
import com.fsck.k9.planck.models.mappers.PlanckIdentityMapper;
import com.fsck.k9.planck.ui.SimpleMessageLoaderHelper;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Collections;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;


public class PlanckStatusPresenterTest extends RobolectricTest {

    @Captor
    private ArgumentCaptor<PlanckProvider.SimpleResultCallback<Rating>> simpleResultCallbackCaptor;

    @Mock SimpleMessageLoaderHelper simpleMessageLoaderHelper;
    @Mock
    PlanckStatusView planckStatusView;
    private PlanckStatusPresenter presenter;
    @Mock
    PlanckUIArtefactCache uiCache;
    @Mock
    PlanckProvider provider;
    @Mock Address senderAddress;
    @Mock
    DisplayHtml displayHtml;
    @Mock
    PlanckIdentityMapper identityMapper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        presenter = new PlanckStatusPresenter(simpleMessageLoaderHelper,
                identityMapper);
    }

    @Test
    public void shouldStartMessageLoaderWhenLoadMessage() {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        presenter.initialize(planckStatusView, uiCache, provider, displayHtml, false,
                senderAddress, forceUnencrypted, alwaysSecure);

        presenter.loadMessage(new MessageReference("", "", "", null));

        verify(simpleMessageLoaderHelper).asyncStartOrResumeLoadingMessage(
                any(MessageReference.class), any(MessageLoaderHelper.MessageLoaderCallbacks.class),
                eq(displayHtml)
        );
    }

    @Test
    public void shouldNotGetRecipientsFromCacheWhenLoadEmptyRecipientsList() throws Exception {
        when(uiCache.getRecipients()).thenReturn(emptyRecipients());

        runLoadRecipients();

        verify(planckStatusView, never()).setupRecipients(anyList());
        verify(planckStatusView).showItsOnlyOwnMsg();
    }

    @Test
    public void shouldGetRecipientsFromCacheWhenLoadRecipients() throws Exception {
        when(uiCache.getRecipients()).thenReturn(recipients());
        when(identityMapper.mapRecipients(anyList())).thenReturn(mappedRecipients());

        runLoadRecipients();

        verify(planckStatusView).setupRecipients(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldExtractRatingWhenOnHandshakeResult() {
        presenter.initialize(planckStatusView, uiCache, provider, displayHtml, true,
                senderAddress, false, false);
        Identity identity = new Identity();

        presenter.onHandshakeResult(identity, false);

        verify(provider).incomingMessageRating(any(), any(PlanckProvider.SimpleResultCallback.class));
    }

    @Test
    public void shouldGetRecipientsWhenOnHandshakeResult() throws Exception {
        when(uiCache.getRecipients()).thenReturn(recipients());
        when(identityMapper.mapRecipients(anyList())).thenReturn(mappedRecipients());

        runOnHandshakeResult(false, false);

        verify(uiCache).getRecipients();
    }

    @Test
    public void shouldUpdateViewOnHandshakeResult() throws Exception {
        runOnHandshakeResultAndStubCallback(false, false);

        verify(planckStatusView).updateIdentities(any());
    }

    @Test
    public void shouldShowMistrustFeedbackOnNegativeHandshakeResult() throws Exception {
        runOnHandshakeResultAndStubCallback(false, false);

        verify(planckStatusView).showMistrustFeedback(any());
    }

    @Test
    public void shouldShowUndoTrustOnPositiveHandshakeResult() throws Exception {
        runOnHandshakeResultAndStubCallback(false, true);

        verify(planckStatusView).showUndoTrust(any());
    }

    @Test
    public void shouldCallRatingChangedOnHandshakeResult() throws Exception {
        runOnHandshakeResultAndStubCallback(false, false);


        verify(planckStatusView).setupBackIntent(Rating.pEpRatingReliable, false, false);
    }

    private void runOnHandshakeResultAndStubCallback(boolean isMessageIncoming, boolean trust)
            throws Exception {
        when(uiCache.getRecipients()).thenReturn(recipients());
        when(identityMapper.mapRecipients(anyList())).thenReturn(mappedRecipients());

        stubHandshakeCallbackCall();
        runOnHandshakeResult(isMessageIncoming, trust);
    }

    private void stubHandshakeCallbackCall() {
        Mockito.doAnswer(invocation -> {
            simpleResultCallbackCaptor.getValue().onLoaded(Rating.pEpRatingReliable);
            return null;
        }).when(provider).getRating(eq(senderAddress), anyList(), anyList(), anyList(),
                simpleResultCallbackCaptor.capture());
    }

    @Test
    public void shouldGetRatingWhenonHandshakeResultWithMessageOutgoing() throws Exception {
        runOnHandshakeResult(false, false);


        verify(provider).getRating(eq(senderAddress), eq(addressList()),
                eq(Collections.emptyList()), eq(Collections.emptyList()), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetRatingWhenonHandshakeResultWithMessageIncoming() throws Exception {
        runOnHandshakeResult(true, false);


        verify(provider).incomingMessageRating(any(), any(PlanckProvider.SimpleResultCallback.class));
    }

    private void runOnHandshakeResult(boolean isMessageIncoming, boolean trust) throws Exception {
        when(uiCache.getRecipients()).thenReturn(recipients());
        when(identityMapper.mapRecipients(anyList())).thenReturn(mappedRecipients());

        presenter.initialize(planckStatusView, uiCache, provider, displayHtml, isMessageIncoming,
                senderAddress, false, false);

        prepareAndRunOnHandshakeResult(trust);
    }

    private void prepareAndRunOnHandshakeResult(boolean trust) throws Exception {
        ShadowLooper shadowLooper = prepareAndCallLoadRecipients(); // requirement: Identities have been loaded

        verify(planckStatusView).setupRecipients(anyList());

        Identity identity = new Identity();
        presenter.onHandshakeResult(identity, trust);
        Thread.sleep(1000);
        shadowLooper.runOneTask();
    }

    @NotNull
    private ShadowLooper prepareAndCallLoadRecipients() throws Exception {
        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());

        presenter.loadRecipients();

        Thread.sleep(1000);
        shadowLooper.runOneTask();
        return shadowLooper;
    }

    private void runLoadRecipients() throws Exception {
        presenter.initialize(planckStatusView, uiCache, provider, displayHtml,false,
                senderAddress, false, false);
        prepareAndCallLoadRecipients();
    }

    @Test
    public void setForceUnencryptedCallsViewMethods() {
        presenter.initialize(planckStatusView, uiCache, provider, displayHtml, true,
                senderAddress, false, false);

        presenter.setForceUnencrypted(true);


        verify(planckStatusView).setupBackIntent(any(), eq(true), eq(false));
    }

    @Test
    public void setAlwaysSecureCallsSetupBackIntent() {
        presenter.initialize(planckStatusView, uiCache, provider, displayHtml, true,
                senderAddress, false, false);

        presenter.setAlwaysSecure(true);

        verify(planckStatusView).setupBackIntent(any(), eq(false), eq(true));
    }



    private ArrayList<Identity> emptyRecipients() {
        return new ArrayList<>();
    }

    private ArrayList<Identity> recipients() {
        ArrayList<Identity> identities = new ArrayList<>();
        Identity identity = new Identity();
        identity.address = "ignacioxpep@hello.ch";
        identities.add(identity);
        return identities;
    }

    private PlanckIdentity mappedIdentity() {
        Identity recipient = recipients().get(0);
        PlanckIdentity out = new PlanckIdentity();
        out.address = recipient.address;
        out.comm_type = recipient.comm_type;
        out.flags = recipient.flags;
        out.fpr = recipient.fpr;
        out.lang = recipient.lang;
        out.user_id = recipient.user_id;
        out.username = recipient.username;
        out.me = recipient.me;
        out.setRating(Rating.pEpRatingReliable);
        return out;
    }

    private ArrayList<PlanckIdentity> mappedRecipients() {
        ArrayList<PlanckIdentity> identities = new ArrayList<>();
        identities.add(mappedIdentity());
        return identities;
    }

    private ArrayList<Address> addressList() {
        ArrayList<Address> adresses = new ArrayList<>();
        adresses.add(new Address("ignacioxpep@hello.ch"));
        return adresses;
    }
}