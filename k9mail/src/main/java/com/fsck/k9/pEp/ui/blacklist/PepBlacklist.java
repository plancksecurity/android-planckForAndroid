package com.fsck.k9.pEp.ui.blacklist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.PEpProvider;

import org.pEp.jniadapter.Identity;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by huss on 31/08/16.
 */

public class PepBlacklist extends K9Activity {

    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;


    RecyclerView.Adapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;

    PEpProvider pEp;


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
        List<KeyListItem> recipient = pEp.getAvailableKey();
        recipientsAdapter = new KeysAdapter(this, recipient);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsAdapter.notifyDataSetChanged();
    }

    

}
