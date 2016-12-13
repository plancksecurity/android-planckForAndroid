package com.fsck.k9.pEp.infrastructure.threading;

import android.os.Handler;
import android.os.Looper;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class UIThread implements PostExecutionThread {

    private final Handler handler;

    @Inject
    public UIThread() {
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void post(Runnable runnable) {
        handler.post(runnable);
    }
}
