package com.fsck.k9.pEp.manualsync;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.WindowManager;

import androidx.annotation.DimenRes;

import com.fsck.k9.R;
import com.fsck.k9.pEp.PepActivity;

public abstract class WizardActivity extends PepActivity {

    public void setUpFloatingWindow(@DimenRes int widthDimen, @DimenRes int heightDimen) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(widthDimen);
        params.height = getResources().getDimensionPixelSize(heightDimen);
        params.alpha = 1;
        params.dimAmount = 0.4f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }

    public void setUpFloatingWindow() {
        setUpFloatingWindow(R.dimen.key_import_floating_width, R.dimen.key_import_floating_height);
    }

    public void setUpFloatingWindow(@DimenRes int heightDimen) {
        setUpFloatingWindow(R.dimen.key_import_floating_width, heightDimen);
    }

}
