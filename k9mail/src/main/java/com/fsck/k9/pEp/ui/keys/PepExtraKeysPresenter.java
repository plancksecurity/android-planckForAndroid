package com.fsck.k9.pEp.ui.keys;

import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.infrastructure.Presenter;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PepExtraKeysPresenter implements Presenter {

    private PEpProvider pEp;
    private PepExtraKeysView view;

    @Inject
    public PepExtraKeysPresenter() {
    }

    public void initialize(PepExtraKeysView view, PEpProvider pEp, List<String> keys) {
        this.pEp = pEp;
        this.view = view;
        setupMasterKeys(keys);
    }

    private void setupMasterKeys(List<String> keys) {
        List<KeyListItem> availableKeys = pEp.getMasterKeysInfo();
        List<KeyListItem> masterKeys = new ArrayList<>(availableKeys.size());
        for (KeyListItem availableKey : availableKeys) {
            availableKey.setSelected(keys.contains(availableKey.getFpr()));
            masterKeys.add(availableKey);
        }
        view.showKeys(masterKeys);
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
