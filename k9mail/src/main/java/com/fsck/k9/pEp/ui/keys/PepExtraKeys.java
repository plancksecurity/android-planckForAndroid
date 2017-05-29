package com.fsck.k9.pEp.ui.keys;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PepActivity;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import java.util.List;

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
    private Preferences preferences;
    private List<String> keys;
    private String account;

    public static void actionShowBlacklist(Context context, Account account) {
        Intent i = new Intent(context, PepExtraKeys.class);
        i.putExtra(ACCOUNT_UUID, account.getUuid());
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

        preferences = Preferences.getPreferences(PepExtraKeys.this);
        account = getIntent().getStringExtra(ACCOUNT_UUID);
        keys = preferences.getMasterKeys(account);
        presenter.initialize(this, pEp, keys);
        initializeToolbar(true, R.string.master_key_management);
    }

    @Override
    public void inject() {
        getpEpComponent().inject(this);
    }

    @Override
    public void showKeys(List<KeyListItem> availableKeys) {
        keysAdapter = new KeyItemAdapter(availableKeys,(item, checked) -> {
            if (checked) {
                keys.add(item.getFpr());
                preferences.setMasterKeysFPRs(account, keys);
            } else {
                keys.remove(item.getFpr());
                preferences.setMasterKeysFPRs(account, keys);
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
