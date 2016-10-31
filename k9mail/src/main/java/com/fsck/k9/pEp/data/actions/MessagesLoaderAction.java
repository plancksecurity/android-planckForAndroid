package com.fsck.k9.pEp.data.actions;

import java.util.List;

public interface MessagesLoaderAction extends Runnable {
    void run();

    interface Callback<Result> {
        void onLoaded(List<Result> result);

        void onError();
    }
}
