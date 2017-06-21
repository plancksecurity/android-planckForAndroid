package com.fsck.k9.pEp.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.components.PEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;

import org.pEp.jniadapter.Rating;

public abstract class PepColoredActivity extends K9Activity {
    public static final String CURRENT_RATING = "current_color";
    public static final String PEP_COLOR_RATING_DETAIL_MESSAGE = "Cannot retrieve pEpRating";
    protected Rating pEpRating = Rating.pEpRatingUndefined;
    PePUIArtefactCache uiCache;
    private PEpProvider pEp;
    private PEpComponent pEpComponent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInjector(getApplicationComponent());
        inject();
    }

    protected abstract void inject();

    @Override
    public void search(String query) {

    }

    protected void colorActionBar() {
        if (getToolbar() != null) {
            PEpUtils.colorToolbar(uiCache, getToolbar(), pEpRating);
            setStatusBarPepColor();
        }
    }

    public void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            setTitle(title);
        }
    }
    protected void loadPepRating() {
        final Intent intent = getIntent();
        String ratingString;
        if (intent.hasExtra(CURRENT_RATING)) {
            ratingString = intent.getStringExtra(CURRENT_RATING);
            Log.d(K9.LOG_TAG, "Got color:" + ratingString);
            pEpRating = PEpUtils.stringToRating(ratingString);
        } else {
            throw new RuntimeException(PEP_COLOR_RATING_DETAIL_MESSAGE);
        }
    }

    public void setpEpRating(Rating pEpRating) {
        this.pEpRating = pEpRating;
    }

    public Rating getpEpRating() {
        return pEpRating;
    }

    protected void initPep() {
        uiCache = PePUIArtefactCache.getInstance(getApplicationContext());
        pEp = ((K9) getApplication()).getpEpProvider();
    }

    public PEpProvider getpEp() {
        return pEp;
    }

    public PePUIArtefactCache getUiCache() {
        return uiCache;
    }

    private ApplicationComponent getApplicationComponent() {
        return getAndroidApplication().getComponent();
    }

    public K9 getAndroidApplication() {
        return (K9) getApplication();
    }

    private void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        pEpComponent = DaggerPEpComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(this))
                .pEpModule(new PEpModule(this, getLoaderManager(), getFragmentManager()))
                .build();
    }

    public void setStatusBarPepColor() {
        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int color = (uiCache.getColor(pEpRating) & 0x00FFFFFF);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            float[] hsv = new float[3];
            Color.RGBToHSV(red, green, blue, hsv);
            hsv[2] = hsv[2]*0.9f;
            color = Color.HSVToColor(hsv);
            window.setStatusBarColor(color);
        }
    }

    public PEpComponent getpEpComponent() {
        return pEpComponent;
    }
}
