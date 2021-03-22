package com.fsck.k9.ui.messageview;


import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.mailstore.AttachmentViewInfo;

import timber.log.Timber;


public class AttachmentView extends ConstraintLayout {
    private AttachmentViewInfo attachment;
    private AttachmentViewCallback callback;

    private ImageView downloadButton;

    public AttachmentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AttachmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AttachmentView(Context context) {
        super(context);
    }

    private void init() {
        downloadButton = findViewById(R.id.download);
    }

    public AttachmentViewInfo getAttachment() {
        return attachment;
    }

    public void setAttachment(AttachmentViewInfo attachment) {
        this.attachment = attachment;
        displayAttachmentInformation();
    }

    public void enableButtons() {
        downloadButton.setEnabled(true);
    }

    public void disableButtons() {
        downloadButton.setEnabled(false);
    }

    private void displayAttachmentInformation() {
        init();

        if (attachment.size > K9.MAX_ATTACHMENT_DOWNLOAD_SIZE) {
            downloadButton.setVisibility(View.GONE);
        }

        setOnClickListener(v -> onViewClick());
        downloadButton.setOnClickListener(v -> onSaveButtonClick());

        TextView attachmentName = findViewById(R.id.attachment_name);
        attachmentName.setText(attachment.displayName);

        setAttachmentSize(attachment.size);

        refreshThumbnail();
    }

    private void setAttachmentSize(long size) {
        TextView attachmentSize = findViewById(R.id.attachment_info);
        if (size == AttachmentViewInfo.UNKNOWN_SIZE) {
            attachmentSize.setText("");
        } else {
            String text = SizeFormatter.formatSize(getContext(), size);
            attachmentSize.setText(text);
        }
    }

    private void onViewClick() {
        callback.onViewAttachment(attachment);
    }

    private void onSaveButtonClick() {
        callback.onSaveAttachment(attachment);
    }

    public void setCallback(AttachmentViewCallback callback) {
        this.callback = callback;
    }

    public void refreshThumbnail() {
        ImageView thumbnailView = findViewById(R.id.attachment_icon);
        Glide.with(getContext())
                .load(attachment.internalUri)
                .placeholder(ContextCompat.getDrawable(getContext(), R.drawable.ic_file_light))
                .centerCrop()
                .listener(new RequestListener<Uri, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
                        Timber.e(e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        thumbnailView.setPadding(0, 0, 0, 0);
                        return false;
                    }
                })
                .into(thumbnailView)
        ;
    }
}
