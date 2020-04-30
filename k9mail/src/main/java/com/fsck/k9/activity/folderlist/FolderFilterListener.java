package com.fsck.k9.activity.folderlist;

import com.fsck.k9.activity.FolderInfoHolder;

import java.util.List;

public interface FolderFilterListener {

    void publishResults(List<FolderInfoHolder> filteredFolders);
}
