package com.fsck.k9.pEp.ui.activities;

import android.app.Instrumentation;
import android.support.annotation.Nullable;
import android.support.test.espresso.IdlingResource;
import android.widget.ListView;

import timber.log.Timber;

public class ListViewIdlingResource implements IdlingResource {

    private ResourceCallback callback;

    private ListView listView;


    public ListViewIdlingResource(Instrumentation instrumentation, ListView view) {
        listView = view;
    }

    @Override
    public boolean isIdleNow() {
        Timber.i("Entra en el bucle isIdleNow");
        if (listView != null && listView.getAdapter().getCount() > 0) {
            if (callback != null) {
                Timber.i("Entra en callbak");
                callback.onTransitionToIdle();
            }
            return true;
        }
        return false;
    }


    @Override
    public void registerIdleTransitionCallback(@Nullable final ResourceCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getName() {
        return "Recycler idling resource";
    }
}