package com.fsck.k9.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.widget.Toast;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import org.pEp.jniadapter.Identity;
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

    private static void displayKeyImportAlert(final Context context, String detail, final String fpr, final String address, final String username)
    {
        ContextThemeWrapper ctw = new ContextThemeWrapper(context, R.style.TextViewCustomFont);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctw);
        alertDialogBuilder.setTitle("Secret key replace")
                .setMessage(detail)
                .setCancelable(false)
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(context, "Key replaced", Toast.LENGTH_LONG).show();
                        Identity id = PEpUtils.createIdentity(new Address(address, username), context);
                        id.fpr = fpr;
                        ((K9) context.getApplicationContext()).getpEpProvider().myself(id);
                    }
                }).setNegativeButton("Reject", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, "Key rejected", Toast.LENGTH_LONG).show();
            }
        });
        AlertDialog dialog = alertDialogBuilder.create();
//        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
//        Typeface face=Typeface.createFromAsset(getAssets(),"fonts/CourierNew.ttf");
//        textView.setTypeface(face);

        dialog.show();
    }

    public static class PrivateKeyReceiver extends BroadcastReceiver {
        public PrivateKeyReceiver() {
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            Log.w("dec", "onReceive: ");
            abortBroadcast();
            displayKeyImportAlert(
                    context,
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_DETAIL),
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_FPR),
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_ADDRESS),
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_USERNAME));
        }
    }
}
