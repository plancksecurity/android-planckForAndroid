package com.fsck.k9.pEp.models;

import com.fsck.k9.Account;
import com.fsck.k9.mailstore.LocalFolder;

public class FolderModel {
    private LocalFolder localFolder;
    private Account account;
    private int unreadCount;

    public LocalFolder getLocalFolder() {
        return localFolder;
    }

    public void setLocalFolder(LocalFolder localFolder) {
        this.localFolder = localFolder;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
