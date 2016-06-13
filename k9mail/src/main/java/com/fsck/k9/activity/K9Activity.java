package com.fsck.k9.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.widget.Toast;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class K9Activity extends Activity implements K9ActivityMagic {

    private K9ActivityCommon mBase;
    private BroadcastReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter();
        filter.addAction("PRIVATE_KEY");
        filter.setPriority(1);
        receiver = new PrivateKeyReceiver();
        registerReceiver(receiver, filter);
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
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void displayAlert()
    {
        ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.TextViewCustomFont);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctw);
        alertDialogBuilder.setTitle("Tit§le")
                .setMessage("Hola soy un mensaje and yout fpr is> AAAA AAAA AAAA AAAA AAAA BBBB BBBB BBBB BBBB BBBB < john@doe.com")
                .setCancelable(false)
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Accept", Toast.LENGTH_LONG).show();
                    }
                }).setNegativeButton("Reject", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Reject", Toast.LENGTH_LONG).show();
            }
        });
        AlertDialog dialog = alertDialogBuilder.create();
//        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
//        Typeface face=Typeface.createFromAsset(getAssets(),"fonts/CourierNew.ttf");
//        textView.setTypeface(face);

        dialog.show();
    }

    public class PrivateKeyReceiver extends BroadcastReceiver {
        public PrivateKeyReceiver() {
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            Log.w("dec", "onReceive: ");
            abortBroadcast();
            displayAlert();
        }
    }
}
