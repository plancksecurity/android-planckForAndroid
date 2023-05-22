package com.fsck.k9.planck.infrastructure.threading;

public interface PostExecutionThread {
    void post(Runnable runnable);
}
