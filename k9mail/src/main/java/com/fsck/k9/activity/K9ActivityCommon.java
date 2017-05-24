package com.fsck.k9.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.misc.SwipeGestureDetector;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;

import org.pEp.jniadapter.Identity;

import java.util.Locale;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import org.pEp.jniadapter.Identity;

import java.util.Locale;


/**
 * This class implements functionality common to most activities used in K-9 Mail.
 *
 * @see K9Activity
 * @see K9ListActivity
 */
public class K9ActivityCommon {
    /**
     * Creates a new instance of {@link K9ActivityCommon} bound to the specified activity.
     *
     * @param activity
     *         The {@link Activity} the returned {@code K9ActivityCommon} instance will be bound to.
     *
     * @return The {@link K9ActivityCommon} instance that will provide the base functionality of the
     *         "K9" activities.
     */
    public static K9ActivityCommon newInstance(Activity activity) {
        return new K9ActivityCommon(activity);
    }

    public static void setLanguage(Context context, String language) {
        Locale locale;
        if (TextUtils.isEmpty(language)) {
            locale = Locale.getDefault();
        } else if (language.length() == 5 && language.charAt(2) == '_') {
            // language is in the form: en_US
            locale = new Locale(language.substring(0, 2), language.substring(3));
        } else {
            locale = new Locale(language);
        }

        Configuration config = new Configuration();
        config.locale = locale;
        Resources resources = context.getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }


    /**
     * Base activities need to implement this interface.
     *
     * <p>The implementing class simply has to call through to the implementation of these methods
     * in {@link K9ActivityCommon}.</p>
     */
    public interface K9ActivityMagic {
        void setupGestureDetector(OnSwipeGestureListener listener);
    }


    private Activity mActivity;
    private GestureDetector mGestureDetector;

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private K9ActivityCommon(Activity activity) {
        mActivity = activity;
        setLanguage(mActivity, K9.getK9Language());
        mActivity.setTheme(K9.getK9ThemeResourceId());
        filter = new IntentFilter();
        filter.addAction("PRIVATE_KEY");
        filter.setPriority(1);
        receiver = new PrivateKeyReceiver();
        mActivity.registerReceiver(receiver, filter);
    }

    /**
     * Call this before calling {@code super.dispatchTouchEvent(MotionEvent)}.
     */
    public void preDispatchTouchEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
    }

    /**
     * Get the background color of the theme used for this activity.
     *
     * @return The background color of the current theme.
     */
    public int getThemeBackgroundColor() {
        TypedArray array = mActivity.getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.colorBackground });

        int backgroundColor = array.getColor(0, 0xFF00FF);

        array.recycle();

        return backgroundColor;
    }

    /**
     * Call this if you wish to use the swipe gesture detector.
     *
     * @param listener
     *         A listener that will be notified if a left to right or right to left swipe has been
     *         detected.
     */
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        mGestureDetector = new GestureDetector(mActivity,
                new SwipeGestureDetector(mActivity, listener));
    }

    private static void displayKeyImportAlert(final Context context, final String fpr, final String address, final String username, String from)
    {
        ContextThemeWrapper ctw = new ContextThemeWrapper(context, R.style.TextViewCustomFont);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctw);
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.import_key_dialog, null);
        String formatedFpr = PEpUtils.formatFpr(fpr);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(formatedFpr.substring(0, formatedFpr.length()/2-1))
                .append("\n")
                .append(formatedFpr.substring(formatedFpr.length()/2, formatedFpr.length()/2+ PEpProvider.HALF_FINGERPRINT_LENGTH));
        ((TextView) dialogView.findViewById(R.id.tvFpr)).setText(stringBuilder.toString());
        ((TextView) dialogView.findViewById(R.id.tvAddress)).setText(String.format(context.getString(R.string.pep_user_address_format), address));
        ((TextView) dialogView.findViewById(R.id.tvUsername)).setText(String.format(context.getString(R.string.pep_user_name_format), username));
        ((TextView) dialogView.findViewById(R.id.tvFrom)).setText(String.format(context.getString(R.string.pep_from_format), from));

        alertDialogBuilder.setView(dialogView)
//                .setTitle("Secret key replace")
//                .setMessage(detail)
                .setCancelable(false)
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FeedbackTools.showLongFeedback(getRootView((Activity) context), "Key replaced");
                        Identity id = PEpUtils.createIdentity(new Address(address, username), context);
                        id.fpr = fpr;
                        ((K9) context.getApplicationContext()).getpEpProvider().myself(id);
                    }
                }).setNegativeButton("Reject", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FeedbackTools.showLongFeedback(getRootView((Activity) context), "Key rejected");
            }
        });
        AlertDialog dialog = alertDialogBuilder.create();
//        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
//        Typeface face=Typeface.createFromAsset(getAssets(),"fonts/CourierNew.ttf");
//        textView.setTypeface(face);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
        dialog.show();
    }

    private static View getRootView(Activity context) {
        return context.getWindow().getDecorView().getRootView();
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
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_FPR),
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_ADDRESS),
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_USERNAME),
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_FROM));
        }
    }


    public void onDestroy() {
        mActivity.unregisterReceiver(receiver);
    }

}
