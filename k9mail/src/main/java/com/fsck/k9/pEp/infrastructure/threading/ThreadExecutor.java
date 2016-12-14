package com.fsck.k9.pEp.infrastructure.threading;

public interface ThreadExecutor {
    void execute(final Runnable runnable);
}
