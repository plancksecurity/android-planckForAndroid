package org.openintents.openpgp.util;


import android.content.Intent;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowAsyncTask;


@Implements(OpenPgpApi.OpenPgpAsyncTask.class)
public class ShadowOpenPgpAsyncTask extends ShadowAsyncTask<Void, Integer, Intent> {
/*
    @RealObject
    private OpenPgpApi.OpenPgpAsyncTask realAsyncTask;

    @Implementation
    public AsyncTask<Void, Integer, Intent> executeOnExecutor(Executor executor, Void... params) {
        return super.execute(params);
    }
*/
}