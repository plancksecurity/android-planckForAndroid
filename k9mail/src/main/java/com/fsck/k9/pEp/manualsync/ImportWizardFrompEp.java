package com.fsck.k9.pEp.manualsync;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.keysync.PEpAddDevice;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ImportWizardFrompEp extends WizardActivity implements ImportWizardFromPGPView {

    public static final String ACCOUNT_UUID_KEY = "accountUuid";
    public static final String IS_STARTER_KEY = "isStarter";
    public static final String KEY_TYPE_KEY = "keyType";
    public static final String HANDSHAKE_INTENT_KEY = "handshakeInfo";
    public static final String IMPORTED_KEY = "imported";
    public static final String IS_PEP = "ispEp";

    @Inject
    ImportWizardPresenter presenter;

    @Bind(R.id.loading)
    ProgressBar loading;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
         outState.putString("currentAction", currentAction.getText().toString());
         outState.putString("description", description.getText().toString());
         outState.putString("toolbarpEpTitle", toolbarpEpTitle.getText().toString());
         outState.putString("action", action.getText().toString());
         outState.putInt("loadingVisibility" , loading.getVisibility());
         outState.putSerializable("state", presenter.getState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentAction.setText(savedInstanceState.getString("currentAction"));
        description.setText(savedInstanceState.getString("description"));
        toolbarpEpTitle.setText(savedInstanceState.getString("toolbarpEpTitle"));
        action.setText(savedInstanceState.getString("action"));
        loading.setVisibility(savedInstanceState.getInt("loadingVisibility"));
        presenter.setState(((ImportKeyWizardState) savedInstanceState.getSerializable("state")));

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(HANDSHAKE_INTENT_KEY)) {
            startActivityForResult((intent.getParcelableExtra(HANDSHAKE_INTENT_KEY)),
                    PEpAddDevice.REQUEST_ADD_DEVICE_HANDSHAKE);
        } else if (intent.hasExtra(IMPORTED_KEY)
                && intent.hasExtra(IS_PEP) && intent.getBooleanExtra(IS_PEP, true)) {
            if (intent.getBooleanExtra(IMPORTED_KEY, false)) {
                presenter.onPrivateKeyReceived();
            }
        } else if (intent.hasExtra(IMPORTED_KEY)
                && intent.hasExtra(IS_PEP) && !intent.getBooleanExtra(IS_PEP, true)) {

        } else if (intent.hasExtra(IS_PEP) && !intent.getBooleanExtra(IS_PEP, true)) {
            description.setText("Your PGP key was processed");
            showCloseButton();
        }
        Toast.makeText(getApplicationContext(), "Key import event produced", Toast.LENGTH_LONG).show();
    }

    @Bind(R.id.tvCurrentAction)
    TextView currentAction;
    @Bind(R.id.description)
    TextView description;
    @Bind(R.id.toolbarpEpTitle)
    TextView toolbarpEpTitle;
    @Bind(R.id.tvProtocol)
    TextView protocol;
    @Bind(R.id.startKeyImportButton)
    Button action;
    @Bind(R.id.cancelKeyImportButton)
    Button cancel;


    Account account;
    private Resources resources;

    public static void actionStartImportpEpKey(Context context, String uuid, boolean starter, KeySourceType sourceType, Intent handshakeIntent) {
        Intent intent = new Intent(context, ImportWizardFrompEp.class);
        intent.putExtra(ACCOUNT_UUID_KEY, uuid);
        intent.putExtra(IS_STARTER_KEY, starter);
        intent.putExtra(KEY_TYPE_KEY, sourceType);
        if (handshakeIntent != null) {
            intent.putExtra(HANDSHAKE_INTENT_KEY, handshakeIntent);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_import_wizzard_from_pgp);
        ButterKnife.bind(this);

        setUpFloatingWindow();
        Intent intent = getIntent();
        resources = getResources();
        if (intent.hasExtra(ACCOUNT_UUID_KEY) && intent.hasExtra(IS_STARTER_KEY)
                && intent.hasExtra(KEY_TYPE_KEY)) {
            Preferences preferences = Preferences.getPreferences(getApplicationContext());
            account = preferences.getAccount(getIntent().getStringExtra(ACCOUNT_UUID_KEY));
            presenter.init(this, account, intent.getBooleanExtra(IS_STARTER_KEY, false),
                    ((KeySourceType) intent.getSerializableExtra(KEY_TYPE_KEY)));

        } else {
            throw new IllegalStateException("Started with incomplete data");
        }

        //setTitle("p≡p key import wizard");
        setUpToolbar(false);
       // getToolbar().setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);

    }


    @Override
    public void inject() {
        getpEpComponent().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.pause();
    }


    @OnClick((R.id.cancelKeyImportButton)) void onCancel() {
        presenter.cancel();
    }
    @Override
    public void showDescription(String description) {
        this.description.setText(description);
    }

    @Override
    public void setImportTitle(String type) {
        this.toolbarpEpTitle.setText(type);
    }


    @Override
    public void renderpEpInitialScreen() {
        this.toolbarpEpTitle.setText(R.string.pep);
        description.setText((R.string.key_import_wizard_from_pep_initial_text));
        //action.setOnClickListener(view -> onStartClick(((Button) view)));
        action.setOnClickListener(view -> presenter.onStartClicked(account));
        protocol.setText(R.string.pep);
        protocol.setTextColor(resources.getColor(R.color.pep_green));
    }

    @Override
    public void renderPGPInitialScreen() {
        this.toolbarpEpTitle.setText(R.string.pgp);
        description.setText((R.string.key_import_wizard_from_pgp_initial_text));
        action.setOnClickListener(view -> presenter.onStartClicked(account));
        protocol.setText(R.string.open_pgp);
        protocol.setTextColor(resources.getColor(R.color.light_black));
    }

    @Override
    public void renderWaitingForHandshake() {
        new Handler(getMainLooper()).post(() -> {
            currentAction.setText("Waiting for handshake");
        });
    }

    @Override
    public void renderWaitingForPGPHandshake() {
        new Handler(getMainLooper()).post(() -> {
            description.setText(R.string.pgp_key_import_instructions);
            description.setText("\n\n" + description.getText());
            currentAction.setText("Waiting for PGP encrypted message");
        });
    }

    @Override
    public void renderpEpSecondlScreen() {
        String accountEmailBold = "<b>" + account.getEmail() + "</b>";
        String textWithEmail = String.format(resources.getString(R.string.key_import_wizard_pep_not_initial_explanation),
                accountEmailBold);
        description.setText((Html.fromHtml(textWithEmail)));
        action.setOnClickListener(button -> {
            action.setText(R.string.cancel_action);
            action.setOnClickListener(view -> presenter.cancel());
            description.setText("Waiting for a Handshake with both devices, a Handshake dialog will appear");
            startActivityForResult((getIntent().getParcelableExtra(HANDSHAKE_INTENT_KEY)), PEpAddDevice.REQUEST_ADD_DEVICE_HANDSHAKE);
        });

    }

    @Override
    public void notifyAcceptedHandshakeAndWaitingForPrivateKey() {
        description.setText("Handshake accepted we proceed to exchange keys.");
        currentAction.setText("Waiting for key");
    }

    @Override
    public void notifyKeySent() {
        new Handler(Looper.getMainLooper()).post(() -> {
            description.setText("Key successfully exported!");
            showCloseButton();
        });
    }

    @Override
    public void finishImportSuccefully() {
        description.setText("Key successfully imported!");
        showCloseButton();
    }

    @Override
    public void close() {
        ((K9) getApplication()).setShowingKeyimportDialog(false);
        finish();
    }

    @Override
    public void cancel() {
        close();
        Toast.makeText(getApplicationContext(), "Key import canceled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void setDialogEnabled() {
        ((K9) getApplication()).setShowingKeyimportDialog(true);
    }

    @Override
    public void notifyAcceptedHandshakeAndWaitingForPGPPrivateKey() {
        description.setText("Handshake accepted please send your private key encrypted and signed from your PGP client.");
        currentAction.setText("Waiting for PGP key");
    }

    @Override
    public void starSendKeyImportRequest() {
        new Handler(getMainLooper()).post(() -> {
            loading.setVisibility(View.VISIBLE);
            currentAction.setVisibility(View.VISIBLE);
            action.setText(R.string.cancel_action);
            currentAction.setText("Sending key import request");
            action.setOnClickListener(view -> presenter.cancel());
        });
    }

    @Override
    public void finishSendingKeyImport() {
        presenter.next();
    }

    @Override
    public void showSendError() {
        new Handler(getMainLooper()).post(() -> {
            description.setText("An error happened, we were not able to send key request. Please try again later");
            showCloseButton();
        });
    }

    @Override
    public void notifySendingOwnKey() {
        new Handler(Looper.getMainLooper()).post(() -> {
            loading.setVisibility(View.VISIBLE);
            currentAction.setVisibility(View.VISIBLE);
            action.setText(R.string.cancel_action);
            description.setText("We are sending your private key");
            currentAction.setText("Sending own key");
            action.setOnClickListener(view -> presenter.cancel());
        });
    }

    @Override
    public void renderPgpSendHandshakeFirstStep() {
        new Handler(getMainLooper()).post(() -> {
            description.setText(R.string.key_import_wizard_from_pgp_send_handshake_request);
            currentAction.setVisibility(View.INVISIBLE);
            loading.setVisibility(View.INVISIBLE);
            action.setText("Next");
            action.setOnClickListener(v -> {presenter.createdPgpReply();});
        });
    }

    @Override
    public void renderPgpSendHandshakeSecondStep() {
        new Handler(getMainLooper()).post(() -> {
            description.setText(R.string.key_import_wizard_from_pgp_send_handshake_request);
            currentAction.setVisibility(View.INVISIBLE);
            loading.setVisibility(View.VISIBLE);
            action.setText("Cancel");
            action.setOnClickListener(v -> {presenter.createdPgpReply();});
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            description.setText("Handshake was cancelled, if you want to import key, please start the process again");
            presenter.reset();
            showCloseButton();
        } else if (resultCode == RESULT_OK) {
            presenter.processHandshakeResult(((PEpAddDevice.Result) data.getSerializableExtra(PEpAddDevice.RESULT)));
        }
    }

    private void showCloseButton() {
        loading.setVisibility(View.INVISIBLE);
        currentAction.setVisibility(View.INVISIBLE);
        action.setText("close");
        action.setOnClickListener(view -> presenter.close());
    }

    public static void notifyPrivateKeyImported(Context context) {
        Intent intent = new Intent(context, ImportWizardFrompEp.class);
        intent.putExtra(IMPORTED_KEY, true);
        intent.putExtra(IS_PEP, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void notifyPrivatePGPKeyProcessed(Context context) {
        Intent intent = new Intent(context, ImportWizardFrompEp.class);
        intent.putExtra(IS_PEP, false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
