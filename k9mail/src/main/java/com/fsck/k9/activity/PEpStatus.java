package com.fsck.k9.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PePUIArtefactCache;

import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

public class PEpStatus extends K9Activity {

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String CURRENT_COLOR = "current_color";
    private Color m_pEpColor = Color.pEpRatingB0rken;

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

        PePUIArtefactCache ui = PePUIArtefactCache.getInstance(getResources());
        ImageView pEpIcon = (ImageView) findViewById(R.id.pEpIcon);
        TextView pEpShortDesc = (TextView) findViewById(R.id.pEpShortDesc);
        TextView pEpLongText = (TextView) findViewById(R.id.pEpLongText);
        Button trustwords = (Button) findViewById(R.id.pEp_trustwords);

        int pEpColor = ui.getColor(m_pEpColor);
        // getWindow().getDecorView().setBackgroundColor(pEpColor);
        // TODO: pEp: somehow, the icon does not show up. OK. Later. After I have real bitmaps...
        pEpIcon.setImageDrawable(ui.getIcon(m_pEpColor));
        pEpIcon.setBackgroundColor(pEpColor);
        pEpShortDesc.setText(ui.getTitle(m_pEpColor));
        // pEpShortDesc.setTextColor(0xffffffff - pEpColor);       // simply invert the background. Might work :-}
        pEpLongText.setText(ui.getDescription(m_pEpColor));
        // pEpLongText.setTextColor(0xffffffff - pEpColor);

        if(m_pEpColor == Color.pEpRatingReliable) {
            trustwords.setVisibility(View.VISIBLE);
            trustwords.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PEpTrustwords.actionShowTrustwords(getApplicationContext(), new Identity(), new Identity());
                }
            });
        }
    }
}
