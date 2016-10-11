package com.fsck.k9.pEp.ui.listeners;

import com.fsck.k9.mailstore.LocalFolder;

public interface OnFolderLongClickListener {
    void onLongClick(LocalFolder folder);
    void onLongClick(Integer position);
}
