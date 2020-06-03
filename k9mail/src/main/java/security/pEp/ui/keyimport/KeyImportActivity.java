package security.pEp.ui.keyimport;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PepActivity;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class KeyImportActivity extends PepActivity implements KeyImportView {

    @Inject
    KeyImportPresenter presenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_key_dialog);
        ButterKnife.bind(this);
        setupFloatingWindow();
        Intent intent = getIntent();
        if (isValidIntent(intent)) {
            //noinspection ConstantConditions
            presenter.initialize(this, intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_FPR),
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_ADDRESS),
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_USERNAME),
                    intent.getExtras().getString(PEpProvider.PEP_PRIVATE_KEY_FROM));
        }
    }

    @Override
    public void search(String query) {
        //NOP
    }

    @Override
    public void inject() {
        getpEpComponent().inject(this);
    }

    @Override
    public void showPositiveFeedback() {
    }

    @Override
    public void renderDialog(PEpProvider.KeyDetail keyDetail, String from) {
    }

    @Override
    public void showNegativeFeedback() {
    }

    private void setupFloatingWindow() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.floating_width);
        params.height = getResources().getDimensionPixelSize(R.dimen.floating_height);
        params.alpha = 1;
        params.dimAmount = 0.4f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }

    @OnClick(R.id.bAccept)
    void onAccept() {
        presenter.onAccept(this.getApplicationContext());
    }

    @OnClick(R.id.bReject)
    void onReject() {
        presenter.onReject();
    }

    public static void actionShowImportDialog(Context context, Intent intent) {
        isValidIntent(intent);

        Intent dialogIntent = new Intent(context, KeyImportActivity.class);
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        dialogIntent.putExtras(intent);
        context.startActivity(dialogIntent);
    }

    private static boolean isValidIntent(Intent intent) {
        if (!intent.hasExtra(PEpProvider.PEP_PRIVATE_KEY_FPR) ||
                !intent.hasExtra(PEpProvider.PEP_PRIVATE_KEY_ADDRESS) ||
                !intent.hasExtra(PEpProvider.PEP_PRIVATE_KEY_USERNAME) ||
                !intent.hasExtra(PEpProvider.PEP_PRIVATE_KEY_FROM)) {
            throw new IllegalStateException("The provided intent does not contain the required extras");
        }
        return true;
    }
}
