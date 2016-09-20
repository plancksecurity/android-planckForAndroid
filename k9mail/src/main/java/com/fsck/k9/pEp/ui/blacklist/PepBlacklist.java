package com.fsck.k9.pEp.ui.blacklist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by huss on 31/08/16.
 */

public class PepBlacklist extends AppCompatActivity implements SearchView.OnQueryTextListener {

    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;
    @Bind(R.id.toolbar)
    Toolbar toolbar;



    KeysAdapter recipientsAdapter;
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
        recipientsAdapter = new KeysAdapter(this, keys);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsAdapter.notifyDataSetChanged();
        setSupportActionBar(toolbar);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pep_search, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);

        return true;
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


}
