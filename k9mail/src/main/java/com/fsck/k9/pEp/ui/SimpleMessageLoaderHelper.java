package com.fsck.k9.pEp.ui;

import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;

import com.fsck.k9.activity.MessageLoaderHelper;
import com.fsck.k9.activity.MessageReference;

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

    public void asyncStartOrResumeLoadingMessage(MessageReference messageReference, MessageLoaderHelper.MessageLoaderCallbacks callbacks) {
        MessageLoaderHelper messageLoaderHelper = new MessageLoaderHelper(context, loaderManager, fragmentManager, callbacks);
        messageLoaderHelper.asyncStartOrResumeLoadingMessage(messageReference, null);
    }
}
