package com.fsck.k9.planck.ui.keys;

import com.fsck.k9.planck.ui.blacklist.KeyListItem;

import java.util.List;

public interface PlanckExtraKeysView {
    void showKeys(List<KeyListItem> availableKeys);
}
