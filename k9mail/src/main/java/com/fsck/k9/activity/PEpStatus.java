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

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PePUIArtefactCache;

import org.pEp.jniadapter.Color;

public class PEpStatus extends Activity {

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
    protected void onCreate(Bundle savedInstanceState) {
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

        getWindow().getDecorView().setBackgroundColor(ui.getColor(m_pEpColor));
        // somehow, the icon does not show up. OK. Later. After I have real bitmaps...
        pEpIcon.setImageDrawable(ui.getIcon(m_pEpColor));
        pEpIcon.setBackgroundColor(ui.getColor(m_pEpColor));
        pEpShortDesc.setText(ui.getTitle(m_pEpColor));
        pEpLongText.setText(ui.getDescription(m_pEpColor));

        if(m_pEpColor == Color.pEpRatingReliable) {
            trustwords.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pep_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
