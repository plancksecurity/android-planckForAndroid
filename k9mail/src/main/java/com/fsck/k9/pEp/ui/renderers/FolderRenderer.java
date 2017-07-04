package com.fsck.k9.pEp.ui.renderers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.pEp.models.FolderModel;
import com.fsck.k9.pEp.ui.listeners.OnFolderClickListener;
import com.pedrogomez.renderers.Renderer;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FolderRenderer extends Renderer<FolderModel> {

    @Bind(R.id.folder_name) TextView folderName;
    @Bind(R.id.folder_new_messages) TextView folderNewMessages;
    private OnFolderClickListener onFolderClickListener;

    @Override
    protected void setUpView(View rootView) {

    }

    @Override
    protected void hookListeners(View rootView) {

    }

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        View inflatedView = inflater.inflate(R.layout.folder_navigation_list_item, parent, false);
        ButterKnife.bind(this, inflatedView);
        return inflatedView;
    }

    @Override
    public void render() {
        FolderModel folder = getContent();
        folderName.setText(FolderInfoHolder.getDisplayName(getContext(),
                folder.getAccount(),
                folder.getLocalFolder().getName()));
        try {
            int unreadMessageCount = folder.getLocalFolder().getUnreadMessageCount();
            renderUnreadMessages(unreadMessageCount);
        } catch (MessagingException e) {
            //TODO do a proper log
            e.printStackTrace();
        }

    }

    private void renderUnreadMessages(int unreadMessageCount) {
        if (unreadMessageCount > 0) {
            folderNewMessages.setVisibility(View.VISIBLE);
            folderNewMessages.setText(String.valueOf(unreadMessageCount));
        } else {
            folderNewMessages.setVisibility(View.GONE);
        }
    }

    public void setFolderClickListener(OnFolderClickListener onFolderClickListener) {
        this.onFolderClickListener = onFolderClickListener;
    }

    @OnClick(R.id.folder_layout) void onFolderClicked() {
        FolderModel folder = getContent();
        onFolderClickListener.onClick(folder.getLocalFolder());
    }
}
