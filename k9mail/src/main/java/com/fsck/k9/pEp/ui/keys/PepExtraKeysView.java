package com.fsck.k9.pEp.ui.keys;

import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import java.util.List;

public interface PepExtraKeysView {
    void showKeys(List<KeyListItem> availableKeys);
}
