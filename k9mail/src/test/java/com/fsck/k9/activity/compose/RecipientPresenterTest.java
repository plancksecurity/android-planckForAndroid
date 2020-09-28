package com.fsck.k9.activity.compose;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import androidx.loader.app.LoaderManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fsck.k9.Account;
import com.fsck.k9.helper.ReplyToParser;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.message.ComposePgpInlineDecider;
import com.fsck.k9.pEp.PEpProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpApiManager;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
//@Config(shadows = { ShadowOpenPgpAsyncTask.class })
public class RecipientPresenterTest {
    private static final ReplyToParser.ReplyToAddresses TO_ADDRESSES = new ReplyToParser.ReplyToAddresses(Address.parse("to@example.org"));
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
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        PEpProvider pEpProvider = mock(PEpProvider.class);

        recipientMvpView = mock(RecipientMvpView.class);
        account = mock(Account.class);
        composePgpInlineDecider = mock(ComposePgpInlineDecider.class);
        replyToParser = mock(ReplyToParser.class);
        LoaderManager loaderManager = mock(LoaderManager.class);
        listener = mock(RecipientPresenter.RecipientsChangedListener.class);
        OpenPgpApiManager openPgpApiManager = mock(OpenPgpApiManager.class);

        recipientPresenter = new RecipientPresenter(
                context,
                loaderManager,
                openPgpApiManager,
                recipientMvpView,
                account,
                composePgpInlineDecider,
                pEpProvider,
                replyToParser, listener);
        recipientPresenter.updateCryptoStatus();
    }

    @Test
    public void testInitFromReplyToMessage() throws Exception {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());

        recipientPresenter.initFromReplyToMessage(message, false);

        Thread.sleep(1000);
        shadowLooper.runOneTask();

        verify(recipientMvpView).addRecipients(eq(Message.RecipientType.TO), any());
    }

    @Test
    public void testInitFromReplyToAllMessage() throws Exception {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);
        ReplyToParser.ReplyToAddresses replyToAddresses = new ReplyToParser.ReplyToAddresses(ALL_TO_ADDRESSES, ALL_CC_ADDRESSES);
        when(replyToParser.getRecipientsToReplyAllTo(message, account)).thenReturn(replyToAddresses);

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());

        recipientPresenter.initFromReplyToMessage(message, true);

        Thread.sleep(1000);
        shadowLooper.runToEndOfTasks();

        verify(recipientMvpView).addRecipients(eq(Message.RecipientType.TO), any());
        verify(recipientMvpView).addRecipients(eq(Message.RecipientType.CC), any());
    }

    @Test
    public void initFromReplyToMessage_shouldCallComposePgpInlineDecider() {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);

        recipientPresenter.initFromReplyToMessage(message, false);

        verify(composePgpInlineDecider).shouldReplyInline(message);
    }

    @Test
    public void getCurrentCryptoStatus_withoutCryptoProvider() {
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(RecipientMvpView.CryptoStatusDisplayType.UNCONFIGURED, status.getCryptoStatusDisplayType());
        assertEquals(RecipientMvpView.CryptoSpecialModeDisplayType.NONE, status.getCryptoSpecialModeDisplayType());
        assertNull(status.getAttachErrorStateOrNull());
        assertFalse(status.isProviderStateOk());
        assertFalse(status.shouldUsePgpMessageBuilder());
        verify(recipientMvpView, times(2)).handlepEpState(any());
    }

    @Test
    public void getCurrentCryptoStatus_withCryptoProvider() throws Exception {
        setupCryptoProvider();

        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(RecipientMvpView.CryptoStatusDisplayType.UNCONFIGURED, status.getCryptoStatusDisplayType());
        assertTrue(status.isEncryptionOpportunistic());
        assertFalse(status.isProviderStateOk());
        assertTrue(status.isSigningEnabled());
        assertFalse(status.shouldUsePgpMessageBuilder());
        verify(recipientMvpView, times(3)).handlepEpState(any());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onCryptoModeChanged(RecipientPresenter.CryptoMode.OPPORTUNISTIC);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(RecipientMvpView.CryptoStatusDisplayType.UNCONFIGURED, status.getCryptoStatusDisplayType());
        assertFalse(status.isProviderStateOk());
        assertTrue(status.isEncryptionOpportunistic());
        assertTrue(status.isSigningEnabled());
        assertFalse(status.shouldUsePgpMessageBuilder());
        verify(recipientMvpView, times(4)).handlepEpState(any());
    }

    @Test
    public void getCurrentCryptoStatus_withModeDisabled() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onCryptoModeChanged(RecipientPresenter.CryptoMode.DISABLE);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(RecipientMvpView.CryptoStatusDisplayType.UNCONFIGURED, status.getCryptoStatusDisplayType());
        assertFalse(status.isProviderStateOk());
        assertTrue(status.isCryptoDisabled());
        assertFalse(status.shouldUsePgpMessageBuilder());
        verify(recipientMvpView, times(4)).handlepEpState(any());
    }

    @Test
    public void getCurrentCryptoStatus_withModePrivate() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onCryptoModeChanged(RecipientPresenter.CryptoMode.PRIVATE);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(RecipientMvpView.CryptoStatusDisplayType.UNCONFIGURED, status.getCryptoStatusDisplayType());
        assertFalse(status.isProviderStateOk());
        assertTrue(status.isSigningEnabled());
        assertFalse(status.shouldUsePgpMessageBuilder());
        verify(recipientMvpView, times(4)).handlepEpState(any());
    }

    @Test
    public void getCurrentCryptoStatus_withModeSignOnly() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onMenuSetSignOnly(true);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(RecipientMvpView.CryptoStatusDisplayType.UNCONFIGURED, status.getCryptoStatusDisplayType());
        assertFalse(status.isProviderStateOk());
        assertTrue(status.isSigningEnabled());
        assertFalse(status.shouldUsePgpMessageBuilder());
        verify(recipientMvpView).showOpenPgpSignOnlyDialog(true);
        verify(recipientMvpView, times(4)).handlepEpState(any());
    }

    @Test
    public void getCurrentCryptoStatus_withModeInline() throws Exception {
        setupCryptoProvider();

        recipientPresenter.onMenuSetPgpInline(true);
        ComposeCryptoStatus status = recipientPresenter.getCurrentCryptoStatus();

        assertEquals(RecipientMvpView.CryptoStatusDisplayType.UNCONFIGURED, status.getCryptoStatusDisplayType());
        assertFalse(status.isProviderStateOk());
        assertTrue(status.isEncryptionOpportunistic());
        assertTrue(status.isSigningEnabled());
        assertFalse(status.shouldUsePgpMessageBuilder());
        verify(recipientMvpView).showOpenPgpInlineDialog(true);
        verify(recipientMvpView, times(4)).handlepEpState(any());
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

    private void setupCryptoProvider() throws android.os.RemoteException {
        Account account = mock(Account.class);
        OpenPgpServiceConnection openPgpServiceConnection = mock(OpenPgpServiceConnection.class);
        IOpenPgpService2 openPgpService2 = mock(IOpenPgpService2.class);
        Intent permissionPingIntent = new Intent();


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
