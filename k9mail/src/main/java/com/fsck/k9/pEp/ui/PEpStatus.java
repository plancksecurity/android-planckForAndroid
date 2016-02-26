package com.fsck.k9.pEp.ui;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.PePUIArtefactCache;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

public class PEpStatus extends K9Activity {

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String CURRENT_COLOR = "current_color";
    private Color m_pEpColor = Color.pEpRatingB0rken;
    PePUIArtefactCache ui;

    public static void actionShowStatus(Context context, Color currentColor) {
        Intent i = new Intent(context, PEpStatus.class);
        i.setAction(ACTION_SHOW_PEP_STATUS);
        i.putExtra(CURRENT_COLOR, currentColor.toString());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        String colorString = intent.getStringExtra(CURRENT_COLOR);
        Log.d(K9.LOG_TAG, "Got color:" + colorString);
        m_pEpColor = Color.valueOf(colorString);
        setContentView(R.layout.pep_status);
        ui = PePUIArtefactCache.getInstance(getResources());
        ColorDrawable colorDrawable = new ColorDrawable(ui.getColor(m_pEpColor));
        if (getActionBar() != null) {
            ActionBar actionBar = getActionBar();
            actionBar.setBackgroundDrawable(colorDrawable);
            actionBar.setTitle(getString(R.string.title_activity_pep_status));
            actionBar.setSubtitle(ui.getTitle(m_pEpColor));
        }


        ImageView pEpIcon = (ImageView) findViewById(R.id.pEpIcon);
        TextView pEpShortDesc = (TextView) findViewById(R.id.pEpShortDesc);
        TextView pEpLongText = (TextView) findViewById(R.id.pEpLongText);
        TextView actionBarTitle = (TextView) findViewById(getActionBarTitleId());
        if (ui.getColor(m_pEpColor) == android.graphics.Color.BLACK) {
            actionBarTitle.setTextColor(android.graphics.Color.WHITE);
        }
        TextView actionBarSubTitle = (TextView) findViewById(getActionBarSubTitleId());
        if (ui.getColor(m_pEpColor) == android.graphics.Color.BLACK) {
            actionBarSubTitle.setTextColor(actionBarSubTitle.getCurrentTextColor() + 0x00555555);
        }
        Button trustwords = (Button) findViewById(R.id.pEp_trustwords);
        setUpContactList();
        int pEpColor = ui.getColor(m_pEpColor);
        // getWindow().getDecorView().setBackgroundColor(pEpColor);
        // TODO: pEp: somehow, the icon does not show up. OK. Later. After I have real bitmaps...
        pEpIcon.setImageDrawable(ui.getIcon(m_pEpColor));
        pEpIcon.setBackgroundColor(pEpColor);
        pEpShortDesc.setText(ui.getTitle(m_pEpColor));
        // pEpShortDesc.setTextColor(0xffffffff - pEpColor);       // simply invert the background. Might work :-}
        pEpLongText.setText(ui.getDescription(m_pEpColor));
        setStatusBarPepColor();
        // pEpLongText.setTextColor(0xffffffff - pEpColor);

        if (m_pEpColor == Color.pEpRatingReliable) {
            trustwords.setVisibility(View.VISIBLE);
            trustwords.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PEpTrustwords.actionShowTrustwords(PEpStatus.this, new Identity(), new Identity());
                }
            });
        }
    }

    private void setUpContactList() {
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        ((RecyclerView) findViewById(R.id.my_recycler_view)).setLayoutManager(layoutManager);
    }

    private int getActionBarTitleId() {
        return getResources().getIdentifier("action_bar_title", "id", "android");
    }

    private int getActionBarSubTitleId() {
        return getResources().getIdentifier("action_bar_subtitle", "id", "android");
    }


    private void setStatusBarPepColor() {
        Window window = this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int color = (ui.getColor(m_pEpColor) & 0x00FFFFFF);
            window.setStatusBarColor(0xff111111);
        }
    }
}
