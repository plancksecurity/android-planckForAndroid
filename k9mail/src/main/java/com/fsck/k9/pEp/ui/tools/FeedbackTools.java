package com.fsck.k9.pEp.ui.tools;

import com.fsck.k9.R;
import com.google.android.material.snackbar.Snackbar;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

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
            snackbar.setActionTextColor(resources.getColor(R.color.planck_yellow));
            snackbar.show();
        } else {
            Timber.e(message);
        }
    }

    public static Feedback createIndefiniteFeedback(View rootView, String message, String actionText, View.OnClickListener actionListener) {
        if (rootView != null) {
            Resources resources = rootView.getResources();
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(actionText, actionListener);
            snackbar.setActionTextColor(resources.getColor(R.color.yellow));
            TextView snackTextView = snackbar.getView().findViewById(R.id.snackbar_text);
            snackTextView.setMaxLines(10);
            snackbar.show();
            return new SnackbarFeedback(snackbar);
        } else {
            Timber.e(message);
            return null;
        }
    }

    public static Feedback createIndefiniteFeedback(
            View rootView,
            String message,
            @ColorInt int backgroundColor,
            @ColorInt int textColor
    ) {
        if (rootView != null) {
            Resources resources = rootView.getResources();
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE);
            View snackbarView = snackbar.getView();
            int padding = Math.round(resources.getDimension(R.dimen.custom_snackbar_margin));
            snackbarView.setPadding(padding, 0, padding,padding);
            snackbarView.setBackgroundColor(
                    ContextCompat.getColor(rootView.getContext(), android.R.color.transparent));

            TextView snackTextView = snackbarView.findViewById(R.id.snackbar_text);
            snackTextView.setMaxLines(10);
            snackTextView.setBackgroundResource(R.drawable.custom_snackbar);
            ColorStateList backgroundColorStateList = new ColorStateList(new int[][]{
                    new int[] {}
            }, new int[]{backgroundColor});
            snackTextView.setBackgroundTintList(backgroundColorStateList);
            snackTextView.setTextColor(textColor);

            snackbar.show();
            return new SnackbarFeedback(snackbar);
        } else {
            Timber.e(message);
            return null;
        }
    }

    public interface Feedback {
        void show();

        void dismiss();

        boolean isShown();
    }

    private static class SnackbarFeedback implements Feedback {
        private final Snackbar snackbar;

        SnackbarFeedback(Snackbar snackbar) {
            this.snackbar = snackbar;
        }

        @Override
        public void show() {
            snackbar.show();
        }

        @Override
        public void dismiss() {
            snackbar.dismiss();
        }

        @Override
        public boolean isShown() {
            return snackbar.isShown();
        }
    }
}
