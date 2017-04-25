package com.fsck.k9.pEp.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PepActivity;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;

import java.util.Calendar;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.fsck.k9.activity.Accounts.USED_LIBRARIES;


public class About extends PepActivity {

    @Bind(R.id.about_text) WebView aboutText;

    public static Intent onAbout(Context context) {
        return new Intent(context, About.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
        setUpToolbar(true);
        PEpUtils.colorToolbar(getToolbar(), getResources().getColor(R.color.pep_green));
        String about = getString(R.string.about_action) + " " + getString(R.string.app_name);
        initializeToolbar(true, about);
        onAbout();
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

    private void onAbout() {
        String appName = getString(R.string.app_name);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        StringBuilder html = new StringBuilder()
                .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />")
                .append("<img src=\"file:///android_asset/icon.png\" alt=\"").append(appName).append("\"/>")
                .append("<h1>")
                .append("</a>")
                .append("</h1><p>")
                .append(appName)
                .append(" ")
                .append(String.format(getString(R.string.debug_version_fmt), getVersionNumber()))
                .append("</p><p>")
                .append(String.format(getString(R.string.app_authors_fmt),
                        getString(R.string.app_authors)))
                .append("</p><p>")
//        .append(String.format(getString(R.string.app_revision_fmt),
//                              "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
//                              getString(R.string.app_revision_url) +
//                              "</a>"))
                .append("</p><hr/><p>")
                .append(String.format(getString(R.string.app_copyright_fmt), year, year))
                .append("</p><hr/><p>")
                .append(getString(R.string.pep_app_license))
                .append("</p><hr/><p>")
// Credits
                .append("p≡p Team in alphabetical order:<br /><br />")
                .append("Volker Birk, Simon Witts, Sandro Köchli,Sabrina Schleifer, Robert Goldmann, Rena Tangens, Patricia Bednar, Patrick Meier, padeluun, Nana Karlstetter, Meinhard Starostik, Mathijs de Haan, Martin Vojcik, Markus Schaber, Lix, Leonard Marquitan, Leon Schumacher, Lars Rohwedder, Krista Grothoff, Kinga Prettenhoffer, Hussein Kasem, Hernâni Marques, Edouard Tisserant, Dolça Moreno, Dirk Zimmermann Dietz Proepper, Detlev Sieber, Dean, Daniel Sosa, be, Berna Alp, Bart Polot, Andy Weber, Ana Rebollo")
                .append("</p><hr/><p>");

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (String[] library : USED_LIBRARIES) {
            libs.append("<li><a href=\"").append(library[1]).append("\">").append(library[0]).append("</a></li>");
        }
        libs.append("</ul>");

        html.append(String.format(getString(R.string.app_libraries), libs.toString()))
                .append("</p><hr/><p>")
                .append(String.format(getString(R.string.app_emoji_icons),
                        "<div>TypePad \u7d75\u6587\u5b57\u30a2\u30a4\u30b3\u30f3\u753b\u50cf " +
                                "(<a href=\"http://typepad.jp/\">Six Apart Ltd</a>) / " +
                                "<a href=\"http://creativecommons.org/licenses/by/2.1/jp/\">CC BY 2.1</a></div>"))
                .append("</p><hr/><p>")
                .append(getString(R.string.app_htmlcleaner_license));


        aboutText.loadDataWithBaseURL("file:///android_res/drawable/", html.toString(), "text/html", "utf-8", null);
    }

    private String getVersionNumber() {
        String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            //Log.e(TAG, "Package name not found", e);
        }
        return version;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }
}
