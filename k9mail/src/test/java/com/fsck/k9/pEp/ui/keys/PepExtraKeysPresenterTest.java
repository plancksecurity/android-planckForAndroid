package com.fsck.k9.pEp.ui.keys;

import com.fsck.k9.pEp.PEpProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

public class PepExtraKeysPresenterTest {

    private PepExtraKeysPresenter presenter;
    @Mock
    private PepExtraKeysView view;
    @Mock
    PEpProvider provider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        presenter = new PepExtraKeysPresenter();
    }

    @Test
    public void shouldGetMasterKeysInfoWhenSetupMasterKeys() {
        presenter.initialize(view, provider, keys());

        verify(provider).getMasterKeysInfo();
    }

    @Test
    public void shouldShowKeysInfoWhenSetupMasterKeys() {
        presenter.initialize(view, provider, keys());

        verify(view).showKeys(any());
    }

    private Set<String> keys() {
        return Collections.emptySet();
    }
}