package com.fsck.k9.pEp.manualsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.fsck.k9.K9;
import com.fsck.k9.R;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.SyncHandshakeSignal;
import security.pEp.sync.SyncState;
import security.pEp.ui.resources.ResourcesProvider;
import timber.log.Timber;

public class ImportWizardFrompEp extends WizardActivity implements ImportWizardFromPGPView {

    public static final String IS_FORMING_GROUP = "isFormingGroup";
    public static final String SYNC_SIGNAL_KEY = "syncSignal";
    public static final String MYSELF_KEY = "myself";
    public static final String PARTNER_KEY = "partner";
    private static final String PEP_SYNC_SIGNAL_ACTION = "PEP_SYNC_SIGNAL";

    @Inject
    ImportWizardPresenter presenter;
    @Inject
    ResourcesProvider resourcesProvider;

    @Bind(R.id.loading)
    ProgressBar loading;
    @Bind(R.id.trustwordsContainer)
    ViewGroup trustwordsContainer;
    @Bind(R.id.description)
    TextView description;
    @Bind(R.id.afirmativeActionButton)
    Button action;
    @Bind(R.id.dissmissActionButton)
    Button cancel;
    @Bind(R.id.negativeActionButton)
    Button reject;
    @Bind(R.id.currentState)
    ImageView currentState;
    @Bind(R.id.trustwords)
    TextView tvTrustwords;
    @Bind(R.id.show_long_trustwords)
    ImageView showLongTrustwords;


    private SyncDialogReceiver receiver;

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("trustwords", tvTrustwords.getText().toString());
        outState.putSerializable("state", presenter.getState());
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        SyncState state = (SyncState) savedInstanceState.getSerializable("state");
        String trustwords = savedInstanceState.getString("trustwords", "");

        presenter.restoreState(state, trustwords);
    }

    public static void actionStartKeySync(Context context,
                                          Identity myself,
                                          Identity partner,
                                          SyncHandshakeSignal signal,
                                          boolean isFormingGroup) {

        Intent intent = createActionStartKeySyncIntent(context, myself, partner, signal, isFormingGroup);
        context.startActivity(intent);
    }

    @NotNull
    public static Intent createActionStartKeySyncIntent(Context context, Identity myself, Identity partner, SyncHandshakeSignal signal, boolean isFormingGroup) {
        Intent intent = new Intent(context, ImportWizardFrompEp.class);
        intent.putExtra(MYSELF_KEY, myself);
        intent.putExtra(PARTNER_KEY, partner);
        intent.putExtra(SYNC_SIGNAL_KEY, signal);
        intent.putExtra(IS_FORMING_GROUP, isFormingGroup);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initBroadcastReceiver();
        setContentView(R.layout.activity_import_wizzard_from_pgp);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (intent.hasExtra(MYSELF_KEY)
                && intent.hasExtra(PARTNER_KEY)
                && intent.hasExtra(IS_FORMING_GROUP)
                && intent.hasExtra(SYNC_SIGNAL_KEY)) {
            boolean isFormingGroup = intent.getBooleanExtra(IS_FORMING_GROUP, false);
            Identity myself = (Identity) intent.getSerializableExtra(MYSELF_KEY);
            Identity partner = (Identity) intent.getSerializableExtra(PARTNER_KEY);
            SyncHandshakeSignal signal = (SyncHandshakeSignal) intent.getSerializableExtra(SYNC_SIGNAL_KEY);
            presenter.init(this, myself, partner, signal, isFormingGroup);

        } else {
            finish();
        }

        setUpToolbar(false);
        setUpFloatingWindow();

    }

    private void initBroadcastReceiver() {
        receiver = new SyncDialogReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(PEP_SYNC_SIGNAL_ACTION);
        filter.setPriority(1);
        registerReceiver(receiver, filter);
    }


    @Override
    public void inject() {
        getpEpComponent().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @OnClick((R.id.dissmissActionButton))
    void onCancel() {
        presenter.cancel();
    }

    @Override
    public void renderpEpCreateDeviceGroupRequest() {
        // this.toolbarpEpTitle.setText(R.string.pep);
        description.setText((R.string.keysync_wizard_create_group_first_message));
        action.setOnClickListener(view -> presenter.next());
        currentState.setImageResource(R.drawable.ic_sync_2nd_device);
    }


    @Override
    public void renderpEpAddToExistingDeviceGroupRequest() {
        description.setText((R.string.keysync_wizard_add_device_to_existing_group_message));
        action.setOnClickListener(view -> presenter.next());
        currentState.setImageResource(R.drawable.ic_sync_3rd_device);
    }


    @Override
    public void close() {
        ((K9) getApplication()).setShowingKeyimportDialog(false);
        finish();
    }

    @Override
    public void cancel() {
        close();
        Timber.d("Keysync canceled");
    }

    @Override
    public void showHandshake(String trustwords) {
        //TODO: Show handshake
        invalidateOptionsMenu();
        showLangIcon();

        description.setText(getText(R.string.keysync_wizard_handshake_message));
        action.setText(R.string.key_import_accept);
        action.setOnClickListener(v -> presenter.acceptHandshake());
        trustwordsContainer.setVisibility(View.VISIBLE);
        tvTrustwords.setText(trustwords);
        currentState.setVisibility(View.GONE);
        reject.setVisibility(View.VISIBLE);
        reject.setOnClickListener( v -> presenter.rejectHandshake());

    }

    private void showLangIcon() {
        int resource = resourcesProvider.getAttributeResource(R.attr.iconLanguageGray);
        assert getToolbar() != null;
        getToolbar().setOverflowIcon(ContextCompat.getDrawable(this, resource));
    }

    @Override
    public void showWaitingForSync() {
        invalidateOptionsMenu();
        description.setText(R.string.keysync_wizard_waiting_message);
        trustwordsContainer.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        action.setVisibility(View.GONE);
        reject.setVisibility(View.GONE);
        cancel.setOnClickListener(v -> presenter.cancel());

    }

    @Override
    public void showGroupCreated() {
        description.setText(R.string.keysync_wizard_group_creation_done_message);
        currentState.setImageResource(R.drawable.ic_sync_2nd_device_synced);

        showKeySyncDone();
    }

    @Override
    public void showJoinedGroup() {
        description.setText(R.string.keysync_wizard_group_joining_done_message);
        currentState.setImageResource(R.drawable.ic_sync_3rd_device_synced);
        showKeySyncDone();
    }

    private void showKeySyncDone() {
        loading.setVisibility(View.GONE);
        cancel.setTextColor(resourcesProvider.getColorFromAttributeResource(R.attr.grayScaleColorOnBackground3));
        cancel.setText(R.string.keysync_wizard_action_leave);
        cancel.setOnClickListener(v -> presenter.leaveDeviceGroup());
        action.setVisibility(View.VISIBLE);

        action.setTextColor(resourcesProvider.getColorFromAttributeResource(R.attr.defaultColorOnBackground));
        action.setOnClickListener(v -> finish());
        currentState.setVisibility(View.VISIBLE);
    }

    @Override
    public void showSomethingWentWrong() {
        description.setText(R.string.keysync_wizard_error_message);
        cancel.setVisibility(View.VISIBLE);
        cancel.setTextColor(resourcesProvider.getColorFromAttributeResource(R.attr.grayScaleColorOnBackground3));
        cancel.setText(R.string.cancel_action);
        cancel.setOnClickListener(v -> presenter.leaveDeviceGroup());

        action.setVisibility(View.VISIBLE);
        action.setTextColor(resourcesProvider.getColorFromAttributeResource(R.attr.defaultColorOnBackground));
        action.setOnClickListener(v -> presenter.cancel());

        reject.setVisibility(View.GONE);
        trustwordsContainer.setVisibility(View.GONE);
        currentState.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
    }

    @Override
    public void disableSync() {
        this.getK9().persistentShutDown();
        finish();
    }

    @Override
    public void leaveDeviceGroup() {
        this.getK9().setpEpSyncEnabled(false);
        finish();
    }

    @Override
    public void showLongTrustwordsIndicator() {
        showLongTrustwords.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLongTrustwordsIndicator() {
        showLongTrustwords.setVisibility(View.INVISIBLE);
    }

    @Override
    public void prepareGroupCreationLoading() {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.add_second_device);
        loading.setIndeterminateDrawable(drawable);
    }

    @Override
    public void prepareGroupJoiningLoading() {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.add_device_to_group);
        loading.setIndeterminateDrawable(drawable);
    }

    public static void notifyNewSignal(Context context, SyncHandshakeSignal signal) {
        Intent intent = new Intent(PEP_SYNC_SIGNAL_ACTION);
        intent.putExtra(SYNC_SIGNAL_KEY, signal);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.sendOrderedBroadcast(intent, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (presenter.isHandshaking()) {
            getMenuInflater().inflate(R.menu.menu_add_device, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //TODO: USE ENGINE LANGUAGES
            case R.id.catalan:
                return presenter.changeTrustwordsLanguage(0);
            case R.id.german:
                return presenter.changeTrustwordsLanguage(1);
            case R.id.spanish:
                return presenter.changeTrustwordsLanguage(2);
            case R.id.french:
                return presenter.changeTrustwordsLanguage(3);
            case R.id.turkish:
                return presenter.changeTrustwordsLanguage(4);
            case R.id.english:
                return presenter.changeTrustwordsLanguage(5);
            case R.id.nederlands:
                return presenter.changeTrustwordsLanguage(6);
        }
        return true;
    }

    @OnClick(R.id.show_long_trustwords)
    public void onClickShowLongTrustwords() {
        showLongTrustwords.setVisibility(View.GONE);
        presenter.switchTrustwordsLength();
    }

    @OnLongClick(R.id.trustwords)
    public boolean onTrustwordsLongClick() {
        presenter.switchTrustwordsLength();
        return true;
    }

    public static class SyncDialogReceiver extends BroadcastReceiver {

        private ImportWizardFrompEp activity;

        public SyncDialogReceiver(ImportWizardFrompEp activity) {
            this.activity = activity;
        }
        public SyncDialogReceiver() {
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            if (activity != null) {
                if (intent.hasExtra(SYNC_SIGNAL_KEY)) {
                    SyncHandshakeSignal signal =
                            (SyncHandshakeSignal) Objects.requireNonNull(intent.getSerializableExtra(SYNC_SIGNAL_KEY));
                    activity.presenter.processSignal(signal);
                }
            }
        }
    }
}
