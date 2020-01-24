package com.fsck.k9.activity.compose;


import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

import androidx.loader.app.LoaderManager;
import androidx.test.core.app.ApplicationProvider;

import com.fsck.k9.Account;
import com.fsck.k9.K9RobolectricTest;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoSpecialModeDisplayType;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.helper.ReplyToParser;
import com.fsck.k9.helper.ReplyToParser.ReplyToAddresses;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.message.ComposePgpInlineDecider;
import com.fsck.k9.view.RecipientSelectView.Recipient;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpApiManager;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.ShadowOpenPgpAsyncTask;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Config(shadows = {ShadowOpenPgpAsyncTask.class})
public class RecipientPresenterTest extends K9RobolectricTest {
    private static final ReplyToAddresses TO_ADDRESSES = new ReplyToAddresses(Address.parse("to@example.org"));
    private static final List<Address> ALL_TO_ADDRESSES = Arrays.asList(Address.parse("allTo@example.org"));
    private static final List<Address> ALL_CC_ADDRESSES = Arrays.asList(Address.parse("allCc@example.org"));
    private static final String CRYPTO_PROVIDER = "crypto_provider";
    private static final long CRYPTO_KEY_ID = 123L;


    private RecipientPresenter recipientPresenter;
    private ReplyToParser replyToParser;
    private ComposePgpInlineDecider composePgpInlineDecider;
    private Account account;
    private RecipientMvpView recipientMvpView;
    private RecipientPresenter.RecipientsChangedListener listener;


    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();

        recipientMvpView = mock(RecipientMvpView.class);
        account = mock(Account.class);
        composePgpInlineDecider = mock(ComposePgpInlineDecider.class);
        replyToParser = mock(ReplyToParser.class);
        LoaderManager loaderManager = mock(LoaderManager.class);
        listener = mock(RecipientPresenter.RecipientsChangedListener.class);

        OpenPgpApiManager openPgpApiManager = new OpenPgpApiManager(context, ApplicationProvider.getApplicationContext());

        recipientPresenter = new RecipientPresenter(
                context, loaderManager, openPgpApiManager, recipientMvpView, account, composePgpInlineDecider, replyToParser, listener);
        recipientPresenter.updateCryptoStatus();
    }

    @Test
    @Ignore("It looks like the support version of AsyncTaskLoader handles background tasks differently")
    public void testInitFromReplyToMessage() {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);

        recipientPresenter.initFromReplyToMessage(message, false);

        verify(recipientMvpView).addRecipients(eq(RecipientType.TO), any(Recipient[].class));
    }

    @Test
    @Ignore("It looks like the support version of AsyncTaskLoader handles background tasks differently")
    public void testInitFromReplyToAllMessage() {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);
        ReplyToAddresses replyToAddresses = new ReplyToAddresses(ALL_TO_ADDRESSES, ALL_CC_ADDRESSES);
        when(replyToParser.getRecipientsToReplyAllTo(message, account)).thenReturn(replyToAddresses);

        recipientPresenter.initFromReplyToMessage(message, true);

        verify(recipientMvpView).addRecipients(eq(RecipientType.TO), any(Recipient.class));
        verify(recipientMvpView).addRecipients(eq(RecipientType.CC), any(Recipient.class));
    }

    @Test
    public void initFromReplyToMessage_shouldCallComposePgpInlineDecider(){
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);

        recipientPresenter.initFromReplyToMessage(message, false);

        verify(composePgpInlineDecider).shouldReplyInline(message);
    }

    @Test
    public void getCurrentCryptoStatus_withoutCryptoProvider() {
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(CryptoStatusDisplayType.UNCONFIGURED, status.getCryptoStatusDisplayType());
        assertEquals(CryptoSpecialModeDisplayType.NONE, status.getCryptoSpecialModeDisplayType());
        assertNull(status.getAttachErrorStateOrNull());
        assertFalse(status.isProviderStateOk());
        assertFalse(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withCryptoProvider() throws Exception {
        setupCryptoProvider();

        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(CryptoStatusDisplayType.OPPORTUNISTIC_EMPTY, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onCryptoModeChanged(CryptoMode.OPPORTUNISTIC);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(CryptoStatusDisplayType.OPPORTUNISTIC_EMPTY, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withModeDisabled() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onCryptoModeChanged(CryptoMode.DISABLE);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(CryptoStatusDisplayType.DISABLED, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertFalse(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withModePrivate() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onCryptoModeChanged(CryptoMode.PRIVATE);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(CryptoStatusDisplayType.PRIVATE_EMPTY, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withModeSignOnly() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onMenuSetSignOnly(true);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(CryptoStatusDisplayType.SIGN_ONLY, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isSigningEnabled());
        assertTrue(status.isSignOnly());
    }

    @Test
    public void getCurrentCryptoStatus_withModeInline() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onMenuSetPgpInline(true);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(CryptoStatusDisplayType.OPPORTUNISTIC_EMPTY, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isPgpInlineModeEnabled());
    }

    @Test
    public void onToTokenAdded_notifiesListenerOfRecipientChange() {
        recipientPresenter.onToTokenAdded();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onToTokenChanged_notifiesListenerOfRecipientChange() {
        recipientPresenter.onToTokenChanged();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onToTokenRemoved_notifiesListenerOfRecipientChange() {
        recipientPresenter.onToTokenRemoved();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onCcTokenAdded_notifiesListenerOfRecipientChange() {
        recipientPresenter.onCcTokenAdded();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onCcTokenChanged_notifiesListenerOfRecipientChange() {
        recipientPresenter.onCcTokenChanged();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onCcTokenRemoved_notifiesListenerOfRecipientChange() {
        recipientPresenter.onCcTokenRemoved();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onBccTokenAdded_notifiesListenerOfRecipientChange() {
        recipientPresenter.onBccTokenAdded();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onBccTokenChanged_notifiesListenerOfRecipientChange() {
        recipientPresenter.onBccTokenChanged();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onBccTokenRemoved_notifiesListenerOfRecipientChange() {
        recipientPresenter.onBccTokenRemoved();
        verify(listener).onRecipientsChanged();
    }

    // TODO realize what is this doing and how to fix it
    private void setupCryptoProvider() throws android.os.RemoteException {
        Account account = mock(Account.class);
        OpenPgpServiceConnection openPgpServiceConnection = mock(OpenPgpServiceConnection.class);
        IOpenPgpService2 openPgpService2 = mock(IOpenPgpService2.class);
        Intent permissionPingIntent = new Intent();

        /*
        when(autocryptStatusInteractor.retrieveCryptoProviderRecipientStatus(
                any(OpenPgpApi.class), any(String[].class))).thenReturn(autocryptStatusResult);
*/
        permissionPingIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        when(account.getOpenPgpProvider()).thenReturn(CRYPTO_PROVIDER);
        when(account.getOpenPgpKey()).thenReturn(CRYPTO_KEY_ID);
        when(account.isOpenPgpProviderConfigured()).thenReturn(true);
        when(account.getOpenPgpProvider()).thenReturn(CRYPTO_PROVIDER);
        when(openPgpServiceConnection.isBound()).thenReturn(true);
        when(openPgpServiceConnection.getService()).thenReturn(openPgpService2);
        when(openPgpService2.execute(any(Intent.class), any(ParcelFileDescriptor.class), any(Integer.class)))
                .thenReturn(permissionPingIntent);

        Robolectric.getBackgroundThreadScheduler().pause();
        recipientPresenter.setOpenPgpServiceConnection(openPgpServiceConnection, CRYPTO_PROVIDER);
        recipientPresenter.onSwitchAccount(account);
        recipientPresenter.updateCryptoStatus();
        Robolectric.getBackgroundThreadScheduler().runOneTask();
    }
}
