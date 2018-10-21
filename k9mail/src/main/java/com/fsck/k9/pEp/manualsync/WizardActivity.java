package com.fsck.k9.pEp.manualsync;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.WindowManager;

import com.fsck.k9.R;
import com.fsck.k9.pEp.PepActivity;

public abstract class WizardActivity extends PepActivity {

    public void setUpFloatingWindow() {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.key_import_floating_width);
        params.height = getResources().getDimensionPixelSize(R.dimen.key_import_floating_height);
        params.alpha = 1;
        params.dimAmount = 0.4f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }
}
