package com.fsck.k9.pEp.ui;

import android.content.Context;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.message.html.DisplayHtml;

import javax.inject.Inject;

public class SimpleMessageLoaderHelper {

    private final Context context;
    private final LoaderManager loaderManager;
    private final FragmentManager fragmentManager;

    @Inject public SimpleMessageLoaderHelper(Context context, LoaderManager loaderManager, FragmentManager fragmentManager) {
        this.context = context;
        this.loaderManager = loaderManager;
        this.fragmentManager = fragmentManager;
    }

    public void asyncStartOrResumeLoadingMessage(MessageReference messageReference,
                                                 MessageLoaderHelper.MessageLoaderCallbacks callbacks,
                                                 DisplayHtml displayHtml) {
        MessageLoaderHelper messageLoaderHelper = new MessageLoaderHelper(context, loaderManager,
                fragmentManager, callbacks, displayHtml);
        messageLoaderHelper.asyncStartOrResumeLoadingMessage(messageReference, null);
    }
}
