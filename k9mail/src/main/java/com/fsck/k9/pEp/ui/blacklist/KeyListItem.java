package com.fsck.k9.pEp.ui.blacklist;

/**
 * Created by huss on 15/09/16.
 */

public class KeyListItem {
    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getFpr() {
        return fpr;
    }

    public String getGpgUid() {
        return gpgUid;
    }

    final String gpgUid;
    final String fpr;
    boolean selected;

    public KeyListItem(String fpr, String gpgUid) {
        this.gpgUid = gpgUid;
        this.fpr = fpr;
        selected = false;
    }

    public KeyListItem(String fpr, String gpgUid, boolean selected) {
        this.gpgUid = gpgUid;
        this.fpr = fpr;
        this.selected = selected;
    }
}
