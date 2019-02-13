package com.fsck.k9.pEp.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem

import com.fsck.k9.R
import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.PepActivity

import java.util.Calendar

import kotlinx.android.synthetic.main.activity_about.*


class AboutActivity : PepActivity() {

    private//Log.e(TAG, "Package name not found", e);
    val versionNumber: String
        get() {
            var version = "?"
            try {
                val pi = packageManager.getPackageInfo(packageName, 0)
                version = pi.versionName
            } catch (e: PackageManager.NameNotFoundException) {
            }

            return version
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setUpToolbar(true)
        PEpUtils.colorToolbar(toolbar, resources.getColor(R.color.pep_green))
        val about = getString(R.string.about_action) + " " + getString(R.string.app_name)
        initializeToolbar(true, about)
        onAbout()
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    private fun onAbout() {
        val appName = getString(R.string.app_name)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val html = StringBuilder()
                .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />")
                .append("<img src=\"file:///android_asset/icon.png\" alt=\"").append(appName).append("\"/>")
                .append("<h1>")
                .append("</a>")
                .append("</h1><p>")
                .append(appName)
                .append(" ")
                .append(String.format(getString(R.string.debug_version_fmt), versionNumber))
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
                .append("</p><hr/><p>")

        val libs = StringBuilder().append("<ul>")
        for ((library, url) in USED_LIBRARIES) {
            libs.append("<li><a href=\"").append(url).append("\">")
                    .append(library)
                    .append("</a></li>")
        }
        libs.append("</ul>")

        html.append(String.format(getString(R.string.app_libraries), libs.toString()))


        about_text!!.loadDataWithBaseURL("file:///android_res/drawable/", html.toString(), "text/html", "utf-8", null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        when (itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return true
    }

    companion object {

        fun onAbout(context: Context) {
            context.startActivity(Intent(context, AboutActivity::class.java))
        }

        val USED_LIBRARIES = mapOf(
                "Android Support Library" to "https://developer.android.com/topic/libraries/support-library/index.html",
                "jutf7" to "http://jutf7.sourceforge.net/",
                "JZlib" to "http://www.jcraft.com/jzlib/",
                "Commons IO" to "http://commons.apache.org/io/",
                "Mime4j" to "http://james.apache.org/mime4j/",
                "HoloColorPicker" to "https://github.com/LarsWerkman/HoloColorPicker",
                "Glide" to "https://github.com/bumptech/glide",
                "jsoup" to "https://jsoup.org/",
                "Moshi" to "https://github.com/square/moshi",
                "Okio" to "https://github.com/square/okio",
                "SafeContentResolver" to "https://github.com/cketti/SafeContentResolver",
                "ShowcaseView" to "https://github.com/amlcurran/ShowcaseView",
                "Timber" to "https://github.com/JakeWharton/timber",
                "TokenAutoComplete" to "https://github.com/splitwise/TokenAutoComplete/",
                "ButterKnife" to "https://github.com/JakeWharton/butterknife",
                "Calligraphy" to "https://github.com/chrisjenx/Calligraphy",
                "GPGME" to "https://www.gnupg.org/(en)/related_software/gpgme/index.html",
                "LibGPG-error" to "https://www.gnupg.org/(en)/related_software/libgpg-error/index.html",
                "Libcrypt" to "https://directory.fsf.org/wiki/Libgcrypt",
                "Libassuan" to "https://www.gnupg.org/(en)/related_software/libassuan/index.html",
                "Libksba" to "https://www.gnupg.org/(en)/related_software/libksba/index.html",
                "GNUPG" to "https://www.gnupg.org/",
                "Libcurl" to "https://curl.haxx.se/libcurl/",
                "Libiconv" to "https://www.gnu.org/software/libiconv/",
                "LibEtPan" to "https://www.etpan.org/libetpan.html")
    }
}
