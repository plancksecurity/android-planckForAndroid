package com.fsck.k9.pEp.models;

import com.fsck.k9.Account;
import com.fsck.k9.mailstore.LocalFolder;

import org.jetbrains.annotations.NotNull;

import security.pEp.animatedlevellist.model.PlainItem;

public class FolderModel extends PlainItem {
    private LocalFolder localFolder;
    private Account account;
    private int unreadCount;
    private String itemName;

    public LocalFolder getLocalFolder() {
        return localFolder;
    }

    public void setLocalFolder(LocalFolder localFolder) {
        this.localFolder = localFolder;
        this.itemName = localFolder.getName();
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

    @NotNull
    @Override
    public String getItemName() {
        return itemName;
    }

    @Override
    public void setItemName(@NotNull String itemName) {
        this.itemName = itemName;
    }

    @Override
    public boolean isSameContentToShow(@NotNull PlainItem other) {
        FolderModel otherModel = (FolderModel) other;
        return itemName.equals(otherModel.itemName) &&
                account.getUuid().equals(otherModel.account.getUuid());
    }
}
