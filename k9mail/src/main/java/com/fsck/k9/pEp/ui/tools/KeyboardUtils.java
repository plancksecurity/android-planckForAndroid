package com.fsck.k9.pEp.ui.tools;


import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import timber.log.Timber;

public class KeyboardUtils {

    public static void hideKeyboard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (RuntimeException e) {
            Timber.e( "Error on hideKeyboard()");
        }
    }    public static void hideKeyboard(Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            View decorView = activity.getWindow().getDecorView();
            if (imm != null) {
                imm.hideSoftInputFromWindow(decorView.getApplicationWindowToken(), 0);
            }
        } catch (RuntimeException e) {
            Timber.e( "Error on hideKeyboard()");
        }
    }
}
