package com.fsck.k9.pEp.ui.keysync;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;
import com.fsck.k9.pEp.ui.PepColoredActivity;
import com.fsck.k9.pEp.ui.adapters.IdentitiesAdapter;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;

import org.pEp.jniadapter.Identity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class KeysyncManagement extends PepColoredActivity implements KeysyncManagementView {

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
    }

    private void initializePresenter() {
        List<Account> accounts = Preferences.getPreferences(KeysyncManagement.this).getAccounts();
        presenter.initialize(this, getpEp(), accounts);
    }

    @Override
    protected void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        DaggerPEpComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(this))
                .pEpModule(new PEpModule(this, getLoaderManager(), getFragmentManager()))
                .build()
                .inject(this);
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
        FeedbackTools.showShortFeedback(getRootView() ,getResources().getString(R.string.openpgp_unknown_error));
    }
}
