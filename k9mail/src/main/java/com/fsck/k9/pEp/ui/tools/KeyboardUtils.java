package com.fsck.k9.pEp.ui.tools;


import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class KeyboardUtils {

    public static void hideKeyboard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (RuntimeException e) {
            Log.e(view.getTag().toString(), "Error on hideKeyboard()");
        }
    }
}
