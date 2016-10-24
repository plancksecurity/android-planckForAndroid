package com.fsck.k9.pEp.ui.infrastructure;

import android.content.Context;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.K9;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;

public class Router {

    public static void onOpenAccount(Context context, BaseAccount account) {
        if (account instanceof SearchAccount) {
            SearchAccount searchAccount = (SearchAccount)account;
            MessageList.actionDisplaySearch(context, searchAccount.getRelatedSearch(), false, false);
        } else {
            Account realAccount = (Account)account;
            if (!realAccount.isEnabled()) {
                return;
            } else if (!realAccount.isAvailable(context)) {
                Log.i(K9.LOG_TAG, "refusing to open account that is not available");
                return;
            }
            if (K9.FOLDER_NONE.equals(realAccount.getAutoExpandFolderName())) {
                FolderList.actionHandleAccount(context, realAccount);
            } else {
                LocalSearch search = new LocalSearch(realAccount.getAutoExpandFolderName());
                search.addAllowedFolder(realAccount.getAutoExpandFolderName());
                search.addAccountUuid(realAccount.getUuid());
                MessageList.actionDisplaySearch(context, search, false, true);}
        }
    }
}
