package com.fsck.k9.pEp.ui.keysync;

import android.test.mock.MockApplication;

import com.fsck.k9.Account;
import com.fsck.k9.pEp.PEpProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pEp.jniadapter.Identity;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21, application = MockApplication.class)
public class AddDevicePresenterTest {
    public static final String PARTNER_USER_ID = "partner_user_id";
    public static final String PARTNER_ADDRESS = "partner_address";
    private AddDevicePresenter addDevicePresenter;
    @Mock AddDeviceView view;
    //@Mock PEpProvider pEpProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        addDevicePresenter = new AddDevicePresenter();
    }

    @Test
    public void shouldNotFilterIdentitiesIfNoAccountsRemoved() throws Exception {
        //when(pEpProvider.updateIdentity(any(Identity.class))).thenReturn(identity());
        //addDevicePresenter.initialize(view, pEpProvider, PARTNER_USER_ID, PARTNER_ADDRESS, accounts());

        List<Identity> identities = identities();
        /*doAnswer(invocation -> {
            PEpProvider.ResultCallback<List<Identity>> callback =
                    (PEpProvider.ResultCallback<List<Identity>>) invocation.getArguments()[0];
            callback.onLoaded(identities);
            return null;
        }).when(pEpProvider).loadOwnIdentities(any(PEpProvider.ResultCallback.class));
*/
        verify(view).showIdentities(identities);
    }

    private List<Account> accounts() {
        Account account = mock(Account.class);
        when(account.getEmail()).thenReturn(PARTNER_ADDRESS);
        return Collections.singletonList(account);
    }

    private List<Identity> identities() {
        return Collections.singletonList(identity());
    }

    private Identity identity() {
        Identity partner = new Identity();
        partner.username = "username";
        partner.user_id = PARTNER_USER_ID;
        partner.flags = 0;
        partner.fpr = "fpr";
        partner.address = PARTNER_ADDRESS;
        partner.lang = "ES";
        partner.me = false;
        return partner;
    }
}