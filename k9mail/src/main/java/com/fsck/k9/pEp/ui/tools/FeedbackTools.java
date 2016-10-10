package com.fsck.k9.pEp.ui.tools;

import android.support.design.widget.Snackbar;
import android.view.View;

public class FeedbackTools {

    public static void showShortFeedback(View rootView, String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    public static void showLongFeedback(View rootView, String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }
}
