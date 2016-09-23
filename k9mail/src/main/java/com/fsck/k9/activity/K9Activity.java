package com.fsck.k9.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.test.mock.MockApplication;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Sync;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class K9Activity extends Activity implements K9ActivityMagic, Sync.showHandshakeCallback {

    private K9ActivityCommon mBase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
        ((K9) getApplication()).pEpSyncProvider.setSyncHandshakeCallback(this);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mBase.preDispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        mBase.setupGestureDetector(listener);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));

    }

    @Override
    protected void onDestroy() {
        mBase.onDestroy();
        super.onDestroy();
    }

    @Override
    public void showHandshake(Identity myself, Identity partner) {
        Toast.makeText(getApplicationContext(), myself.fpr + "/n" + partner.fpr, Toast.LENGTH_LONG).show();
        Log.i("pEp", "showHandshake: " + myself.fpr + "/n" + partner.fpr);
    }
}
