package com.fsck.k9.pEp.ui.keysync;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fsck.k9.Account;
import com.fsck.k9.RobolectricTest;
import com.fsck.k9.pEp.PEpProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import foundation.pEp.jniadapter.Identity;

public class AddDevicePresenterTest extends RobolectricTest {
    private static final String PARTNER_USER_ID = "partner_user_id";
    private static final String PARTNER_ADDRESS = "partner@address";
    private AddDevicePresenter addDevicePresenter;
    @Mock private AddDeviceView view;
    @Mock private PEpProvider pEpProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        addDevicePresenter = new AddDevicePresenter();
        addDevicePresenter.initialize(view, pEpProvider, identity(), identity(), accounts(),false, "");
    }

    @Test
    public void shouldAcceptHandshakeOnEngineWhenAccepting() {
        addDevicePresenter.acceptHandshake();

        verify(pEpProvider).acceptSync();
    }

    @Test
    public void shouldCancelHandshakeOnEngineWhenCancelling() {
        addDevicePresenter.cancelHandshake();

        verify(pEpProvider).cancelSync();
    }

    @Test
    public void shouldRejectHandshakeOnEngineWhenRejecting() {
        addDevicePresenter.rejectHandshake();

        verify(pEpProvider).rejectSync();
    }

    @Test
    public void shouldCloseViewWhenAccepting() {
        addDevicePresenter.acceptHandshake();

        verify(view).close(true);
    }

    @Test
    public void shouldCloseViewWhenRejecting() {
        addDevicePresenter.rejectHandshake();

        verify(view).close(false);
    }

    @Test
    public void shouldCloseViewWhenCancelling() {
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