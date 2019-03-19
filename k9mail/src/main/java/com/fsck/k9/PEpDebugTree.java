package com.fsck.k9;

import timber.log.Timber;

class PEpDebugTree extends Timber.DebugTree {

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        super.log(priority, "pEp", message, t);
    }
}
