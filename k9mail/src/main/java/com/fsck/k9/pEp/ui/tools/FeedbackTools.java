package com.fsck.k9.pEp.ui.tools;

import com.fsck.k9.R;
import com.google.android.material.snackbar.Snackbar;

import android.content.res.Resources;
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

    public static void showLongFeedback(View rootView, String message, String actionText, View.OnClickListener actionListener) {
        if (rootView != null) {
            Resources resources = rootView.getResources();
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
            snackbar.setAction(actionText, actionListener);
            snackbar.setActionTextColor(resources.getColor(R.color.pep_yellow));
            snackbar.show();
        } else {
            Timber.e(message);
        }
    }
}
