package com.fsck.k9.planck.infrastructure.threading;

public interface ThreadExecutor {
    void execute(final Runnable runnable);
}
