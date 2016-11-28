package com.fsck.k9.pEp.infrastructure;

import android.os.Handler;

public class Poller {

    private final Handler handler;
    private long intervalMilliseconds;
    private Runnable polledRunnable;
    private Runnable recursiveRunnable;
    private boolean isPolling;

    public Poller(Handler handler) {
        this.handler = handler;
    }

    public void init(long intervalMilliseconds, Runnable polledRunnable) {
        this.intervalMilliseconds = intervalMilliseconds;
        this.polledRunnable = polledRunnable;
        recursiveRunnable = new Runnable() {
            @Override public void run() {
                if (isPolling) {
                    pollNow();
                    scheduleNextPoll();
                }
            }
        };
    }

    public void scheduleNextPoll() {
        if (isPolling) {
            handler.postDelayed(recursiveRunnable, intervalMilliseconds);
        }
    }

    protected void pollNow() {
        polledRunnable.run();
    }

    public void startPolling() {
        if (!isPolling) {
            isPolling = true;
            scheduleNextPoll();
        }
    }

    public void stopPolling() {
        if (isPolling) {
            isPolling = false;
            handler.removeCallbacks(recursiveRunnable);
        }
    }

    public long getIntervalMilliseconds() {
        return intervalMilliseconds;
    }

    public boolean isPolling() {
        return isPolling;
    }
}
