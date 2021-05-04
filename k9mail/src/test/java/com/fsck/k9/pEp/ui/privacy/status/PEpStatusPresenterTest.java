package com.fsck.k9.pEp.ui.privacy.status;

import android.os.Looper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.message.html.DisplayHtml;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.models.mappers.PEpIdentityMapper;
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class PEpStatusPresenterTest {

    @Captor
    private ArgumentCaptor<PEpProvider.SimpleResultCallback<Rating>> simpleResultCallbackCaptor;

    @Mock SimpleMessageLoaderHelper simpleMessageLoaderHelper;
    @Mock PEpStatusView pEpStatusView;
    private PEpStatusPresenter presenter;
    @Mock PePUIArtefactCache uiCache;
    @Mock PEpProvider provider;
    @Mock Address senderAddress;
    @Mock
    DisplayHtml displayHtml;
    @Mock PEpIdentityMapper identityMapper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        presenter = new PEpStatusPresenter(simpleMessageLoaderHelper,
                identityMapper);
    }

    @Test
    public void shouldStartMessageLoaderWhenLoadMessage() {
        boolean forceUnencrypted = false;
        boolean alwaysSecure = false;
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, false,
                senderAddress, forceUnencrypted, alwaysSecure);

        presenter.loadMessage(new MessageReference("", "", "", null));

        verify(simpleMessageLoaderHelper).asyncStartOrResumeLoadingMessage(
                any(MessageReference.class), any(MessageLoaderHelper.MessageLoaderCallbacks.class),
                displayHtml
        );
    }

    @Test
    public void shouldNotGetRecipientsFromCacheWhenLoadEmptyRecipientsList() throws Exception {
        when(uiCache.getRecipients()).thenReturn(emptyRecipients());

        runLoadRecipients();

        verify(pEpStatusView, never()).setupRecipients(anyList());
        verify(pEpStatusView).showItsOnlyOwnMsg();
    }

    @Test
    public void shouldGetRecipientsFromCacheWhenLoadRecipients() throws Exception {
        when(uiCache.getRecipients()).thenReturn(recipients());
        when(identityMapper.mapRecipients(anyList())).thenReturn(mappedRecipients());

        runLoadRecipients();

        verify(pEpStatusView).setupRecipients(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldExtractRatingWhenOnHandshakeResult() {
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, true,
                senderAddress, false, false);
        Identity identity = new Identity();

        presenter.onHandshakeResult(identity, false);

        verify(provider).incomingMessageRating(any(), any(PEpProvider.SimpleResultCallback.class));
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

        verify(pEpStatusView).updateIdentities(any());
    }

    @Test
    public void shouldShowMistrustFeedbackOnNegativeHandshakeResult() throws Exception {
        runOnHandshakeResultAndStubCallback(false, false);

        verify(pEpStatusView).showMistrustFeedback(any());
    }

    @Test
    public void shouldShowUndoTrustOnPositiveHandshakeResult() throws Exception {
        runOnHandshakeResultAndStubCallback(false, true);

        verify(pEpStatusView).showUndoTrust(any());
    }

    @Test
    public void shouldCallRatingChangedOnHandshakeResult() throws Exception {
        runOnHandshakeResultAndStubCallback(false, false);


        verify(pEpStatusView).setRating(Rating.pEpRatingReliable);
        verify(pEpStatusView).setupBackIntent(Rating.pEpRatingReliable, false, false);
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


        verify(provider).incomingMessageRating(any(), any(PEpProvider.SimpleResultCallback.class));
    }

    private void runOnHandshakeResult(boolean isMessageIncoming, boolean trust) throws Exception {
        when(uiCache.getRecipients()).thenReturn(recipients());
        when(identityMapper.mapRecipients(anyList())).thenReturn(mappedRecipients());

        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, isMessageIncoming,
                senderAddress, false, false);

        prepareAndRunOnHandshakeResult(trust);
    }

    private void prepareAndRunOnHandshakeResult(boolean trust) throws Exception {
        ShadowLooper shadowLooper = prepareAndCallLoadRecipients(); // requirement: Identities have been loaded

        verify(pEpStatusView).setupRecipients(anyList());

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
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml,false,
                senderAddress, false, false);
        prepareAndCallLoadRecipients();
    }

    @Test
    public void setForceUnencryptedCallsViewMethods() {
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, true,
                senderAddress, false, false);

        presenter.setForceUnencrypted(true);

        verify(pEpStatusView).updateToolbarColor(Rating.pEpRatingUnencrypted);
        verify(pEpStatusView).setupBackIntent(any(), eq(true), eq(false));
    }

    @Test
    public void setAlwaysSecureCallsSetupBackIntent() {
        presenter.initialize(pEpStatusView, uiCache, provider, displayHtml, true,
                senderAddress, false, false);

        presenter.setAlwaysSecure(true);

        verify(pEpStatusView).setupBackIntent(any(), eq(false), eq(true));
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

    private PEpIdentity mappedIdentity() {
        Identity recipient = recipients().get(0);
        PEpIdentity out = new PEpIdentity();
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

    private ArrayList<PEpIdentity> mappedRecipients() {
        ArrayList<PEpIdentity> identities = new ArrayList<>();
        identities.add(mappedIdentity());
        return identities;
    }

    private ArrayList<Address> addressList() {
        ArrayList<Address> adresses = new ArrayList<>();
        adresses.add(new Address("ignacioxpep@hello.ch"));
        return adresses;
    }
}