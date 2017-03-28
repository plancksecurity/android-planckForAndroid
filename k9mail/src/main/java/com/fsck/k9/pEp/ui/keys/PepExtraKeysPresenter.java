package com.fsck.k9.pEp.ui.keys;

import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.Presenter;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import java.util.List;

import javax.inject.Inject;

public class PepExtraKeysPresenter implements Presenter {

    private PEpProvider pEp;
    private PepExtraKeysView view;

    @Inject
    public PepExtraKeysPresenter() {
    }

    public void initialize(PepExtraKeysView view, PEpProvider pEp) {
        this.pEp = pEp;
        this.view = view;
        List<KeyListItem> availableKeys = pEp.getAvailableKey();
        view.showKeys(availableKeys);
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {

    }
}
