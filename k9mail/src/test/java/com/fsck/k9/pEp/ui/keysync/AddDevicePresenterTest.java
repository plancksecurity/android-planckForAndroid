package com.fsck.k9.pEp.ui.keysync;

import com.fsck.k9.Account;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.pEp.PEpProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pEp.jniadapter.Identity;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class AddDevicePresenterTest {
    private static final String PARTNER_USER_ID = "partner_user_id";
    private static final String PARTNER_ADDRESS = "partner@address";
    private AddDevicePresenter addDevicePresenter;
    @Mock private AddDeviceView view;
    @Mock private PEpProvider pEpProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        addDevicePresenter = new AddDevicePresenter();
        addDevicePresenter.initialize(view, pEpProvider, identity(), accounts());
    }

    @Test
    public void shouldAcceptHandshakeOnEngineWhenAccepting() throws Exception {
        addDevicePresenter.acceptHandshake();

        verify(pEpProvider).acceptHandshake(any(Identity.class));
    }

    @Test
    public void shouldCancelHandshakeOnEngineWhenCancelling() throws Exception {
        addDevicePresenter.cancelHandshake();

        verify(pEpProvider).cancelHandshake(any(Identity.class));
    }

    @Test
    public void shouldRejectHandshakeOnEngineWhenRejecting() throws Exception {
        addDevicePresenter.rejectHandshake();

        verify(pEpProvider).rejectHandshake(any(Identity.class));
    }

    @Test
    public void shouldCloseViewWhenAccepting() throws Exception {
        addDevicePresenter.acceptHandshake();

        verify(view).close();
    }

    @Test
    public void shouldCloseViewWhenRejecting() throws Exception {
        addDevicePresenter.rejectHandshake();

        verify(view).close();
    }

    @Test
    public void shouldCloseViewWhenCancelling() throws Exception {
        addDevicePresenter.cancelHandshake();

        verify(view).goBack();
    }

    private List<Account> accounts() {
        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn(PARTNER_ADDRESS);
        return Collections.singletonList(account);
    }

    private Identity identity() {
        Identity partner = new Identity();
        partner.username = "username";
        partner.user_id = PARTNER_USER_ID;
        partner.flags = 0;
        partner.fpr = "111122223333444455556666777788889999AAAA";
        partner.address = PARTNER_ADDRESS;
        partner.lang = "ES";
        return partner;
    }

}