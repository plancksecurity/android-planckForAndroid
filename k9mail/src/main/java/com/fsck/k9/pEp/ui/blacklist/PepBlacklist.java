package com.fsck.k9.pEp.ui.blacklist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SearchView;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.ui.keys.KeyItemAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PepBlacklist extends AppCompatActivity implements SearchView.OnQueryTextListener {

    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;
    @Bind(R.id.toolbar)
    Toolbar toolbar;


    KeyItemAdapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;

    PEpProvider pEp;
    private List<KeyListItem> keys;


    public static void actionShowBlacklist(Context context) {
        Intent i = new Intent(context, PepBlacklist.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pep_blacklist);
        ButterKnife.bind(PepBlacklist.this);
        pEp = ((K9) getApplication()).getpEpProvider();
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        recipientsView.setVisibility(View.VISIBLE);
        keys = pEp.getAvailableKey();
        initializeKeysView();
        setSupportActionBar(toolbar);
    }

    private void initializeKeysView() {
        recipientsAdapter = new KeyItemAdapter(keys, (item, checked) -> {
            if (checked) {
                pEp.addToBlacklist(item.fpr);
            } else {
                pEp.deleteFromBlacklist(item.fpr);
            }
        });
        recipientsView.setAdapter(recipientsAdapter);
        recipientsAdapter.notifyDataSetChanged();
        setSupportActionBar(toolbar);
    }


    @Override
    public boolean onQueryTextSubmit(String query) {
        final List<KeyListItem> filteredModelList = filter(keys, query);
        recipientsAdapter.replaceAll(filteredModelList);
        recipientsView.scrollToPosition(0);
        return true;
    }

    private static List<KeyListItem> filter(List<KeyListItem> models, String query) {
        final String lowerCaseQuery = query.toLowerCase();

        final List<KeyListItem> filteredModelList = new ArrayList<>();
        for (KeyListItem model : models) {
            final String text = model.getGpgUid().toLowerCase();
            if (text.contains(lowerCaseQuery)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pep_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_add_fpr:
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.fpr_dialog, null);
                dialogBuilder.setView(dialogView);
                final EditText fpr = (EditText) dialogView.findViewById(R.id.fpr_text);
                dialogBuilder.setTitle("Add FPR");
                dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
                    addFingerprintToBlacklist(fpr);
                });
                dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> {
                });
                AlertDialog b = dialogBuilder.create();
                b.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addFingerprintToBlacklist(EditText fpr) {
        String fingerprint = fpr.getText().toString().toUpperCase().replaceAll(" ", "");
        Pattern pattern = Pattern.compile("^[0-9A-F]+$");
        Matcher matcher = pattern.matcher(fingerprint);
        if (matcher.find() && fingerprint.length() >= 40) {
            for (KeyListItem key : keys) {
                if (key.fpr.equals(fingerprint)) {
                    pEp.addToBlacklist(fingerprint);
                }
            }
            keys = pEp.getAvailableKey();
            initializeKeysView();
        } else {
            //FeedbackTools.showShortFeedback(container, getString(R.string.error_parsing_fingerprint));
        }
    }

}
