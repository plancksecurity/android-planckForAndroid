package com.fsck.k9.pEp.ui.tools;

import android.support.design.widget.Snackbar;
import android.view.View;

import timber.log.Timber;

public class FeedbackTools {

    public static void showShortFeedback(View rootView, String message) {
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Timber.e(message);
        }
    }

    public static void showLongFeedback(View rootView, String message) {
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
        } else {
            Timber.e(message);
        }
    }
}
