package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;

import org.pEp.jniadapter.Rating;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Displays a welcome message when no accounts have been created yet.
 */
public class WelcomeMessage extends K9Activity implements OnClickListener{

    @Bind(R.id.welcome_app_version) TextView appDescription;

    public static void showWelcomeMessage(Context context) {
        Intent intent = new Intent(context, WelcomeMessage.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        bindViewsForLayout(R.layout.welcome_message);
        ButterKnife.bind(this);

        initializeToolbar(false, R.string.welcome_message_title);
        PEpUtils.colorToolbar(PePUIArtefactCache.getInstance(getApplicationContext()), getToolbar(), Rating.pEpRatingTrustedAndAnonymized);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String description = getString(R.string.app_k9_pep_name) + pInfo.versionName + "\n" + getString(R.string.pep_app_description);
            appDescription.setText(description);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView welcome = (TextView) findViewById(R.id.welcome_message);
        welcome.setText(HtmlConverter.htmlToSpanned(getString(R.string.accounts_welcome)));
        welcome.setMovementMethod(LinkMovementMethod.getInstance());

        findViewById(R.id.next).setOnClickListener(this);
        findViewById(R.id.import_settings).setOnClickListener(this);
    }

    @Override
    public void search(String query) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.next: {
                AccountSetupBasics.actionNewAccount(this);
                finish();
                break;
            }
            case R.id.import_settings: {
                Accounts.importSettings(this);
                finish();
                break;
            }
        }
    }
}
