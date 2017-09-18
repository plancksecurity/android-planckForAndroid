package com.fsck.k9.pEp.ui.keysync;

import com.fsck.k9.Account;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpProvider.ResultCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pEp.jniadapter.Identity;

import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

public class KeysyncManagerPresenterTest {

    private KeysyncManagerPresenter presenter;
    @Mock KeysyncManagementView view;
    @Mock PEpProvider provider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        presenter = new KeysyncManagerPresenter();
    }

    @Test
    public void shouldGetMasterKeysInfoWhenSetupMasterKeys() throws Exception {
        setupResultCallback();

        presenter.initialize(view, provider, accounts());

        verify(view).showIdentities(any());
    }

    private void setupResultCallback() {
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                ResultCallback<List<Identity>> callback =
                        (ResultCallback<List<Identity>>) invocation.getArguments()[0];
                callback.onLoaded(identities());
                return null;
            }
        }).when(provider).loadOwnIdentities(any(ResultCallback.class));
    }

    @Test
    public void shouldShowErrorWhenEngineReturnsError() throws Exception {
        setupErrorCallback();

        presenter.initialize(view, provider, accounts());

        verify(view).showError();
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