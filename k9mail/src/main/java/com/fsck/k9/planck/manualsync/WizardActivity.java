package com.fsck.k9.planck.manualsync;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.WindowManager;

import androidx.annotation.DimenRes;

import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;

public abstract class WizardActivity extends K9Activity {

    protected void setUpFloatingWindow(@DimenRes int widthDimen, @DimenRes int heightDimen) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int width = getResources().getDimensionPixelSize(widthDimen);
        int height = getResources().getDimensionPixelSize(heightDimen);
        setUpFloatingWindowInternal(width, height);
    }

    protected void setUpFloatingWindowInternal(int width, int height) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = width;
        params.height = height;
        params.alpha = 1;
        params.dimAmount = 0.6f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }

    protected void setUpFloatingWindow() {
        setUpFloatingWindow(R.dimen.key_import_floating_width, R.dimen.key_import_floating_height);
    }

    protected void setUpFloatingWindow(@DimenRes int heightDimen) {
        setUpFloatingWindow(R.dimen.key_import_floating_width, heightDimen);
    }

    protected void setUpFloatingWindowWrapHeight() {
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        int width = getResources().getDimensionPixelSize(R.dimen.key_import_floating_width);
        setUpFloatingWindowInternal(width, height);
    }

}
