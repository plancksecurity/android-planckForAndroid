package com.fsck.k9.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.fsck.k9.R;

import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

public class PEpTrustwords extends K9Activity {

    private static final String ACTION_SHOW_PEP_TRUSTWORDS = "com.fsck.k9.intent.action.SHOW_PEP_TRUSTWORDS";
    private final static String MY_IDENTITY="me";
    private final static String OTHER_IDENTITY="you";

    private String myFingerprint;
    private String otherFingerprint;

    public static void actionShowTrustwords(Context context, Identity me, Identity you) {
        Intent i = new Intent(context, PEpStatus.class);
        i.setAction(ACTION_SHOW_PEP_TRUSTWORDS);
        i.putExtra(MY_IDENTITY, me.fpr);
        i.putExtra(OTHER_IDENTITY, you.fpr);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        myFingerprint = intent.getStringExtra(MY_IDENTITY);
        otherFingerprint = intent.getStringExtra(OTHER_IDENTITY);

        setContentView(R.layout.pep_trustwords);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
