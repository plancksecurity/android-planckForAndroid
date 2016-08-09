/*
Created by Helm  01/07/16.
*/


package com.fsck.k9.pEp.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import org.pEp.jniadapter.Color;

public class PepColoredActivity extends K9Activity {
    public static final String CURRENT_COLOR = "current_color";
    public static final String PEP_COLOR_ERR0R_DETAIL_MESSAGE = "Cannot retrieve pEpColor";
    private Color m_pEpColor = Color.pEpRatingUndefined;
    PePUIArtefactCache uiCache;
    private PEpProvider pEp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void colorActionBar() {
        ActionBar actionBar = getActionBar() ;
        if (actionBar != null) {
            PEpUtils.colorActionBar(uiCache, actionBar, m_pEpColor);
        }
    }

    protected void loadPepColor() {
        final Intent intent = getIntent();
        String colorString;
        if (intent.hasExtra(CURRENT_COLOR)) {
            colorString = intent.getStringExtra(CURRENT_COLOR);
            Log.d(K9.LOG_TAG, "Got color:" + colorString);
            m_pEpColor = Color.valueOf(colorString);
        } else {
            throw new RuntimeException(PEP_COLOR_ERR0R_DETAIL_MESSAGE);
        }
    }

    public void setpEpColor(Color pEpColor) {
        this.m_pEpColor = pEpColor;
    }
    public Color getpEpColor() {
        return m_pEpColor;
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
}
