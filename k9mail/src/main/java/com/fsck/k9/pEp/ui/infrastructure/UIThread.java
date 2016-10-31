/**
 * Copyright (C) 2014 android10.org. All rights reserved.
 */
package com.fsck.k9.pEp.ui.infrastructure;

import android.os.Handler;
import android.os.Looper;

import com.fsck.k9.pEp.data.executor.PostExecutionThread;

import javax.inject.Singleton;

@Singleton
public class UIThread implements PostExecutionThread {

  private final Handler handler;

  public UIThread() {
    this.handler = new Handler(Looper.getMainLooper());
  }

  @Override
  public void post(Runnable runnable) {
    handler.post(runnable);
  }
}
