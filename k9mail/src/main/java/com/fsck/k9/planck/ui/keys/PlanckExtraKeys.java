package com.fsck.k9.planck.ui.keys;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.planck.ui.blacklist.KeyListItem;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PlanckExtraKeys extends K9Activity implements PlanckExtraKeysView {

    @Inject
    PlanckExtraKeysPresenter presenter;
    @Bind(R.id.extra_keys_view)
    RecyclerView keysView;

    public static void actionStart(Context context) {
        Intent i = new Intent(context, PlanckExtraKeys.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pep_extra_keys);
        ButterKnife.bind(PlanckExtraKeys.this);

        LinearLayoutManager keysViewManager = new LinearLayoutManager(this);
        keysViewManager.setOrientation(LinearLayoutManager.VERTICAL);
        keysView.setLayoutManager(keysViewManager);
        presenter.initialize(this);
        initializeToolbar(true, R.string.master_key_management);
    }

    @Override
    public void showKeys(List<KeyListItem> availableKeys) {
        KeyItemAdapter keysAdapter = new KeyItemAdapter(availableKeys, (item, checked) -> {
            if (checked) {
                presenter.addMasterKey(item.getFpr());
            } else {
                presenter.removeMasterKey(item.getFpr());
            }
        });
        keysView.setVisibility(View.VISIBLE);
        keysView.setAdapter(keysAdapter);
        keysAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
