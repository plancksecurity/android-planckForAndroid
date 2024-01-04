package com.fsck.k9.activity.compose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.loader.app.LoaderManager;
import androidx.test.core.app.ApplicationProvider;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.helper.ReplyToParser;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.message.ComposePgpInlineDecider;
import com.fsck.k9.planck.PlanckProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpApiManager;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@LooperMode(LEGACY)
//@Config(shadows = { ShadowOpenPgpAsyncTask.class })
public class RecipientPresenterTest extends RobolectricTest {
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
    private RecipientSelectPresenter toPresenter;
    private RecipientSelectPresenter ccPresenter;
    private RecipientSelectPresenter bccPresenter;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        PlanckProvider planckProvider = mock(PlanckProvider.class);

        recipientMvpView = mock(RecipientMvpView.class);
        ArgumentCaptor<RecipientPresenter> presenterCaptor = ArgumentCaptor.forClass(RecipientPresenter.class);
        doAnswer(invocation -> {
            presenterCaptor.getValue().setPresenter(toPresenter, Message.RecipientType.TO);
            presenterCaptor.getValue().setPresenter(ccPresenter, Message.RecipientType.CC);
            presenterCaptor.getValue().setPresenter(bccPresenter, Message.RecipientType.BCC);
            return null;
        }).when(recipientMvpView).setPresenter(presenterCaptor.capture());

        toPresenter = mock(RecipientSelectPresenter.class);
        ccPresenter = mock(RecipientSelectPresenter.class);
        bccPresenter = mock(RecipientSelectPresenter.class);
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
                planckProvider,
                replyToParser, listener);
    }

    @Test
    public void testInitFromReplyToMessage() throws Exception {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());

        recipientPresenter.initFromReplyToMessage(message, false);

        Thread.sleep(1000);
        shadowLooper.runOneTask();

        verify(toPresenter).addRecipients(any());
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

        verify(toPresenter).addRecipients(any());
        verify(ccPresenter).addRecipients(any());
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
        verify(recipientMvpView, times(1)).handlepEpState(anyBoolean());
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
        verify(recipientMvpView, times(2)).handlepEpState(anyBoolean());
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
        verify(recipientMvpView, times(3)).handlepEpState(anyBoolean());
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
        verify(recipientMvpView, times(3)).handlepEpState(anyBoolean());
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
        verify(recipientMvpView, times(3)).handlepEpState(anyBoolean());
    }

    @Test
    public void onRecipientsChanged_notifiesListenerOfRecipientChange() {
        recipientPresenter.onRecipientsChanged();
        verify(listener).onRecipientsChanged();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void handleUnsecureDeliveryWarning_usesViewToHandleWarning() {
        doReturn(true).when(account).isPlanckPrivacyProtected();
        doReturn(2).when(toPresenter).getUnsecureAddressChannelCount();
        doReturn(2).when(ccPresenter).getUnsecureAddressChannelCount();
        doReturn(2).when(bccPresenter).getUnsecureAddressChannelCount();
        try (MockedStatic<K9> mockK9 = mockStatic(K9.class)) {
            mockK9.when(K9::isPlanckForwardWarningEnabled).thenReturn(true);


            recipientPresenter.handleUnsecureDeliveryWarning();


            verify(toPresenter).getUnsecureAddressChannelCount();
            verify(ccPresenter).getUnsecureAddressChannelCount();
            verify(bccPresenter).getUnsecureAddressChannelCount();
            verify(recipientMvpView).showUnsecureDeliveryWarning(6);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void handleUnsecureDeliveryWarning_CallsViewMethodWith0_IfPEpIsDisabledForAccount() {
        doReturn(false).when(account).isPlanckPrivacyProtected();
        doReturn(2).when(toPresenter).getUnsecureAddressChannelCount();
        doReturn(2).when(ccPresenter).getUnsecureAddressChannelCount();
        doReturn(2).when(bccPresenter).getUnsecureAddressChannelCount();
        try (MockedStatic<K9> mockK9 = mockStatic(K9.class)) {
            mockK9.when(K9::isPlanckForwardWarningEnabled).thenReturn(true);


            recipientPresenter.handleUnsecureDeliveryWarning();


            verify(toPresenter, never()).getUnsecureAddressChannelCount();
            verify(ccPresenter, never()).getUnsecureAddressChannelCount();
            verify(bccPresenter, never()).getUnsecureAddressChannelCount();
            verify(recipientMvpView, times(2)).hideUnsecureDeliveryWarning();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void handleUnsecureDeliveryWarning_CallsViewMethodWith0_IfWarningSettingNotEnabled() {
        doReturn(true).when(account).isPlanckPrivacyProtected();
        doReturn(2).when(toPresenter).getUnsecureAddressChannelCount();
        doReturn(2).when(ccPresenter).getUnsecureAddressChannelCount();
        doReturn(2).when(bccPresenter).getUnsecureAddressChannelCount();
        try (MockedStatic<K9> mockK9 = mockStatic(K9.class)) {
            mockK9.when(K9::isPlanckForwardWarningEnabled).thenReturn(false);


            recipientPresenter.handleUnsecureDeliveryWarning();


            verify(toPresenter, never()).getUnsecureAddressChannelCount();
            verify(ccPresenter, never()).getUnsecureAddressChannelCount();
            verify(bccPresenter, never()).getUnsecureAddressChannelCount();
            verify(recipientMvpView, times(2)).hideUnsecureDeliveryWarning();
        }
    }

    @Test
    public void checkRecipientsOkForSending_calls_presenters_tryPerformCompletion() {
        recipientPresenter.checkRecipientsOkForSending();


        verify(toPresenter).tryPerformCompletion();
        verify(ccPresenter).tryPerformCompletion();
        verify(bccPresenter).tryPerformCompletion();
    }

    @Test
    public void checkRecipientsOkForSending_returns_true_if_no_presenter_has_addresses() {
        doReturn(new ArrayList<Address>()).when(toPresenter).getAddresses();
        doReturn(new ArrayList<Address>()).when(ccPresenter).getAddresses();
        doReturn(new ArrayList<Address>()).when(bccPresenter).getAddresses();


        assertTrue(recipientPresenter.checkRecipientsOkForSending());
        verify(toPresenter).showNoRecipientsError();
    }

    @Test
    public void checkRecipientsOkForSending_returns_true_if_any_presenter_reports_incompleteRecipients() {
        doReturn(true).when(toPresenter).reportedUncompletedRecipients();


        assertTrue(recipientPresenter.checkRecipientsOkForSending());
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
