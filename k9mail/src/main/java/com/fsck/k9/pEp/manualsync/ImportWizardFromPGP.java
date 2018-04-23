package com.fsck.k9.pEp.manualsync;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.fsck.k9.R;
import com.fsck.k9.pEp.PepActivity;

public class ImportWizardFromPGP extends PepActivity implements ImportWizardFromPGPView {

    public static void actionStartImportPgpKey(Context context) {
        Intent intent = new Intent(context, ImportWizardFromPGP.class);
        context.startActivity(intent);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_wizzard_from_pgp);
    }

    @Override
    public void inject() {
        getpEpComponent().inject(this);
    }
}
