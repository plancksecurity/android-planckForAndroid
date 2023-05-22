package com.fsck.k9.planck.ui.activities;

import android.app.Instrumentation;
import androidx.annotation.Nullable;
import androidx.test.espresso.IdlingResource;
import android.widget.ListView;

public class ListViewIdlingResource implements IdlingResource {

    private ResourceCallback callback;

    private ListView listView;


    public ListViewIdlingResource(Instrumentation instrumentation, ListView view) {
        listView = view;
    }

    @Override
    public boolean isIdleNow() {
        if (listView != null && listView.getAdapter().getCount() > 0) {
            if (callback != null) {
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
        return "ListView idling resource";
    }
}