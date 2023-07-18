package com.fsck.k9.planck.ui.keysync;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.planck.PlanckActivity;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.ui.adapters.IdentitiesAdapter;
import com.fsck.k9.planck.ui.tools.FeedbackTools;

import dagger.hilt.android.AndroidEntryPoint;
import foundation.pEp.jniadapter.Identity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

@AndroidEntryPoint
public class KeysyncManagement extends PlanckActivity implements KeysyncManagementView {

    @Inject
    KeysyncManagerPresenter presenter;

    @Bind(R.id.advanced_options_key_list)
    RecyclerView identitiesList;

    public static Intent getKeysyncManager(Context context) {
        return new Intent(context, KeysyncManagement.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keysync_management);
        ButterKnife.bind(this);
        initPep();
        initializePresenter();
        initializeToolbar(true, R.string.manage_identities_title);
    }

    private void initializePresenter() {
        presenter.initialize(this);
    }

    @Override
    public void showIdentities(ArrayList<Identity> identities) {
        identitiesList.setVisibility(View.VISIBLE);
        IdentitiesAdapter identityAdapter = new IdentitiesAdapter(identities, (identity, check) -> presenter.identityCheckStatusChanged(identity, check));
        identitiesList.setLayoutManager(new LinearLayoutManager(this));
        identitiesList.setAdapter(identityAdapter);
    }

    @Override
    public void showError() {
        FeedbackTools.showShortFeedback(getRootView() ,getResources().getString(R.string.status_loading_error));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
}
