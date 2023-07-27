package com.fsck.k9.planck.ui.keysync;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckProvider.ResultCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import foundation.pEp.jniadapter.Identity;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class KeysyncManagerPresenterTest {

    private KeysyncManagerPresenter presenter;
    @Mock KeysyncManagementView view;
    @Mock
    PlanckProvider provider;
    @Mock private Preferences preferences;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(accounts()).when(preferences).getAccounts();
        presenter = new KeysyncManagerPresenter(provider, preferences);
    }

    @Test
    public void shouldGetMasterKeysInfoWhenSetupMasterKeys() throws Exception {
        setupResultCallback();

        presenter.initialize(view);

        verify(view).showIdentities(any());
    }

    @Test
    public void shouldShowErrorWhenEngineReturnsError() throws Exception {
        setupErrorCallback();

        presenter.initialize(view);

        verify(view).showError();
    }

    private void setupResultCallback() {
        doAnswer(invocation -> {
            ResultCallback<List<Identity>> callback =
                    (ResultCallback<List<Identity>>) invocation.getArguments()[0];
            callback.onLoaded(identities());
            return null;
        }).when(provider).loadOwnIdentities(any(ResultCallback.class));
    }

    private void setupErrorCallback() {
        doAnswer(invocation -> {
            ResultCallback<List<Identity>> callback =
                    (ResultCallback<List<Identity>>) invocation.getArguments()[0];
            callback.onError(new Throwable());
            return null;
        }).when(provider).loadOwnIdentities(any(ResultCallback.class));
    }

    private List<Identity> identities() {
        return Collections.emptyList();
    }

    private List<Account> accounts() {
        return Collections.emptyList();
    }
}