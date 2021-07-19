package com.fsck.k9.pEp.ui.keys;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PepActivity;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PepExtraKeys extends PepActivity implements PepExtraKeysView {

    public static final String ACCOUNT_UUID = "accountUuid";
    @Inject
    PepExtraKeysPresenter presenter;
    @Bind(R.id.extra_keys_view)
    RecyclerView keysView;
    private PEpProvider pEp;
    private KeyItemAdapter keysAdapter;
    private LinearLayoutManager keysViewManager;

    public static void actionStart(Context context) {
        Intent i = new Intent(context, PepExtraKeys.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pep_extra_keys);
        ButterKnife.bind(PepExtraKeys.this);
        pEp = ((K9) getApplication()).getpEpProvider();

        keysViewManager = new LinearLayoutManager(this);
        keysViewManager.setOrientation(LinearLayoutManager.VERTICAL);
        keysView.setLayoutManager(keysViewManager);

        HashSet<String> keys = new HashSet<>(K9.getMasterKeys());
        presenter.initialize(this, pEp, keys, K9.ispEpExtraKeysNotSelectable());
        initializeToolbar(true, R.string.master_key_management);
    }

    @Override
    public void inject() {
        getpEpComponent().inject(this);
    }

    @Override
    public void showKeys(List<KeyListItem> availableKeys, boolean isClickLocked) {
        keysAdapter = new KeyItemAdapter(availableKeys,
                isClickLocked,
                (item, checked) -> {
                    if (checked) {
                        presenter.addKey(item);
                    } else {
                        presenter.removeKey(item);
                    }
                });
        keysView.setVisibility(View.VISIBLE);
        keysView.setAdapter(keysAdapter);
        keysAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
