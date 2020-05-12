package security.pEp.ui.about

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.fsck.k9.R
import com.fsck.k9.pEp.PepActivity
import kotlinx.android.synthetic.main.activity_about.*
import security.pEp.ui.toolbar.ToolBarCustomizer
import java.util.*
import javax.inject.Inject


class AboutActivity : PepActivity() {

    @Inject
    lateinit var toolbarCustomizer: ToolBarCustomizer

    private//Log.e(TAG, "Package name not found", e);
    val versionNumber: String
        get() {
            var version = "?"
            try {
                val pi = packageManager.getPackageInfo(packageName, 0)
                version = pi.versionName
            } catch (ignore: Exception) {
            }

            return version
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViews(R.layout.activity_about)
        setUpToolbar(true)

        toolbarCustomizer.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        val about = getString(R.string.about_action) + " " + getString(R.string.app_name)
        initializeToolbar(true, about)

        val aboutString = buildAboutString()
        aboutText.movementMethod = LinkMovementMethod.getInstance()
        aboutText.text = HtmlCompat.fromHtml(aboutString, HtmlCompat.FROM_HTML_MODE_LEGACY)

        license_button.setOnClickListener { openLicenseActivity(this) }
        license_button.paintFlags = license_button.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    private fun buildAboutString(): String {
        val appName = getString(R.string.app_name)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val html = StringBuilder()
                .append("<p>$appName ${String.format(getString(R.string.debug_version_fmt), versionNumber)}</p>")
                .append("<p>${String.format(getString(R.string.app_authors_fmt), getString(R.string.app_authors))}</p>")
                .append(String.format(getString(R.string.app_copyright_fmt), year, year))


        val libs = buildLibrariesHtml()
        html.append("<p>${getString(R.string.app_libraries)}</p>")
        html.append(libs)

        return html.toString()
    }

    private fun buildLibrariesHtml(): String {
        val libs = StringBuilder()
        USED_LIBRARIES
                .forEach { entry ->
                    libs.append("<p>&emsp;<a href=\"${entry.value}\"><b>${entry.key}</b></a></p>")
                }
        return libs.toString()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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

                "pEpEngine" to "https://pep.foundation/dev/repos/pEpEngine/",
                "pEpJNIAdapter" to "https://pep.foundation/dev/repos/pEpJNIAdapter/",
                "libpEpAdapter" to "https://pep.foundation/dev/repos/libpEpAdapter/",
                "Android X Library" to "https://developer.android.com/jetpack/androidx",
                "Android-Support-Preference-V7-Fix" to "https://github.com/Gericop/Android-Support-Preference-V7-Fix",
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
                "Libiconv" to "https://www.gnu.org/software/libiconv/",
                "LibEtPan" to "https://www.etpan.org/libetpan.html",
                "Sequoia-pgp" to "https://sequoia-pgp.org",
                "Libnettle" to "https://www.lysator.liu.se/~nisse/nettle",
                "libgmp" to "https://gmplib.org",
                "openssl" to "https://www.openssl.org",
                "Okio" to "https://github.com/square/okio",
                "jcip-annotations" to "https://github.com/stephenc/jcip-annotations",
                "Renderers" to "https://github.com/pedrovgs/Renderers",
                "AppIntro" to "https://github.com/AppIntro/AppIntro",
                "AndroidSwipeLayout" to "https://github.com/daimajia/AndroidSwipeLayout",
                "Dexter" to "https://github.com/Karumi/Dexter",
                "Acra" to "https://github.com/ACRA/acra",
                "CircleImageView" to "https://github.com/hdodenhof/CircleImageView",
                "Groupie" to "https://github.com/lisawray/groupie",
                "Robolectric" to "http://robolectric.org/",
                "Dagger" to "https://github.com/google/dagger",
                "Barista" to "https://github.com/AdevintaSpain/Barista",
                "Spoon" to "https://github.com/square/spoon",
                "Koin" to "https://github.com/InsertKoinIO/koin",
                "Cucumber" to "https://cucumber.io/"

        )
    }
}
