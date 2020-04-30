package com.fsck.k9.activity.folderlist;

import android.view.View;

import com.fsck.k9.activity.MessageList;
import com.fsck.k9.search.LocalSearch;

public class FolderClickListener implements View.OnClickListener {

        private final LocalSearch search;

        FolderClickListener(LocalSearch search) {
            this.search = search;
        }

        @Override
        public void onClick(View v) {
            MessageList.actionDisplaySearch(v.getContext(), search, true, false, true);
        }
    }
