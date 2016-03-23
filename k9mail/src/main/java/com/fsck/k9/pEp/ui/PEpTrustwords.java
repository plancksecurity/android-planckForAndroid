package com.fsck.k9.pEp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PePUIArtefactCache;
import org.pEp.jniadapter.Identity;

public class PEpTrustwords extends K9Activity {

    private static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    private final static String MY_IDENTITY="me";
    private final static String OTHER_IDENTITY="you";
    private static final String TRUSTWORDS = "trustwordsKey";
    public static final String PARTNER_POSITION = "partnerPositionKey";
    public static final int DEFAULT_POSITION = -1;
    public static final int HANDSHAKE_REQUEST = 1;

    private String myFingerprint;
    private String otherFingerprint;
    private Identity partner;
    private int partnerPosition;

    @Bind(R.id.trustwords)
    TextView tvTrustwords;
    private PEpProvider pEp;
    private PePUIArtefactCache uiCache;

    public static void actionRequestHandshake(Activity context, String trust, int partnerPosition) {
        Intent i = new Intent(context, PEpTrustwords.class);
        i.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        i.putExtra(TRUSTWORDS, trust);
        i.putExtra(PARTNER_POSITION, partnerPosition);
        context.startActivityForResult(i, HANDSHAKE_REQUEST);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        myFingerprint = intent.getStringExtra(MY_IDENTITY);
        otherFingerprint = intent.getStringExtra(OTHER_IDENTITY);


        setContentView(R.layout.pep_trustwords);
        ButterKnife.bind(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        pEp = ((K9) getApplication()).getpEpProvider();
        uiCache = PePUIArtefactCache.getInstance(getResources());

        if (getIntent() != null) {
            if (intent.hasExtra(TRUSTWORDS)) {
                tvTrustwords.setText(getIntent().getStringExtra(TRUSTWORDS));
            }

            if (intent.hasExtra(PARTNER_POSITION)) {
                partnerPosition = intent.getIntExtra(PARTNER_POSITION, DEFAULT_POSITION);
                partner = uiCache.getRecipients().get(partnerPosition);
            }

        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pep_trustwords, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }


    @OnClick(R.id.confirmTrustWords) public void confirmTrustwords() {
        pEp.trustPersonaKey(partner);
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PARTNER_POSITION, partnerPosition);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();

    }
}
