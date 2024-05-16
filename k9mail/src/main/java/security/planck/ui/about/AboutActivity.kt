package security.planck.ui.about

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.TableRow
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.databinding.ActivityAboutBinding
import com.fsck.k9.planck.infrastructure.extensions.showTermsAndConditions
import com.fsck.k9.planck.infrastructure.extensions.showUserManual
import dagger.hilt.android.AndroidEntryPoint
import security.planck.ui.mdm.MdmSettingsFeedbackActivity
import security.planck.ui.toolbar.ToolBarCustomizer
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class AboutActivity : K9Activity() {
    private lateinit var binding: ActivityAboutBinding

    @Inject
    lateinit var toolbarCustomizer: ToolBarCustomizer
    private var iconClickCount = 0

    private val versionNumber: String = BuildConfig.BASE_VERSION
        .substringAfter('v')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpToolbar(true)
        if (BuildConfig.IS_OFFICIAL) {
            binding.icon.setOnClickListener {
                if (++iconClickCount >= UNLOCK_SETTINGS_SCREEN_CLICK_COUNT) {
                    iconClickCount = 0
                    MdmSettingsFeedbackActivity.start(this)
                }
            }
        }

        toolbarCustomizer.setDefaultToolbarColor()
        val about = getString(R.string.about_action) + " " + getString(R.string.app_name)
        initializeToolbar(true, about)

        val aboutString = buildAboutString()
        binding.aboutText.movementMethod = LinkMovementMethod.getInstance()
        binding.aboutText.text = HtmlCompat.fromHtml(aboutString, HtmlCompat.FROM_HTML_MODE_LEGACY)

        binding.documentationButton.setOnClickListener {
            showUserManual()
        }
        binding.documentationButton.paintFlags = binding.documentationButton.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        buildLibrariesTable()

        binding.termsAndConditions.text = HtmlCompat.fromHtml(
            "<a href=\"#\">Terms and Conditions</a>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        binding.termsAndConditions.setOnClickListener {
            showTermsAndConditions()
        }
    }

    override fun onResume() {
        super.onResume()
        iconClickCount = 0
    }

    private fun buildAboutString(): String {
        val appName = getString(R.string.app_name)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val html = StringBuilder()
                .append("<p>$appName ${String.format(getString(R.string.debug_version_fmt), versionNumber)}</p>")
                .append("<p>${String.format(getString(R.string.app_authors_fmt), getString(R.string.app_authors))}</p>")
                .append(String.format(getString(R.string.app_copyright_fmt), year))

        return html.toString()
    }

    private fun buildLibrariesTable() {
        binding.librariesTable.addView(getLibsHeader())
        USED_LIBRARIES
            .forEach { entry ->
                binding.librariesTable.addView(entry.toTableRow(this))
            }
    }

    private fun getLibsHeader(): TableRow {
        val tableRow = layoutInflater.inflate(R.layout.about_library_row, null) as TableRow
        val nameTextView = tableRow.getChildAt(0) as TextView
        val licenseTextView = tableRow.getChildAt(1) as TextView
        nameTextView.text = "Library name"
        licenseTextView.text = "License"
        nameTextView.setTypeface(null, Typeface.BOLD)
        licenseTextView.setTypeface(null, Typeface.BOLD)

        return tableRow
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

        private const val UNLOCK_SETTINGS_SCREEN_CLICK_COUNT = 5

        fun onAbout(context: Context) {
            context.startActivity(Intent(context, AboutActivity::class.java))
        }

        private val USED_LIBRARIES = listOf(
            Library("planckCoreSequoiaBackend", LicenseType.GPLv3, "https://github.com/plancksecurity/foundation-planckCoreSequoiaBackend"),
            Library("Sequoia PGP", LicenseType.GPLv3, "https://gitlab.com/sequoia-pgp/sequoia"),
            Library("planck core Version 3", LicenseType.GPLv3, "https://github.com/plancksecurity/foundation-planckCoreV3"),
            Library("planck JNI Wrapper", LicenseType.AGPL, "https://github.com/plancksecurity/foundation-planckJNIWrapper"),
            Library("libetpan", LicenseType.MISSING, "https://github.com/plancksecurity/foundation-libetpan"),
            Library("libPlanckCxx11", LicenseType.GPLv3, "https://github.com/plancksecurity/foundation-libPlanckCxx11"),
            Library("libPlanckWrapper", LicenseType.GPLv3, "https://github.com/plancksecurity/foundation-libPlanckWrapper"),
            Library("libPlanckTransport", LicenseType.GPLv3, "https://github.com/plancksecurity/foundation-libPlanckTransport"),
            Library("yml2", LicenseType.GPLv2, "https://git.planck.security/foundation/yml2"),

            Library("Kotlin", LicenseType.APACHE2_0, "https://github.com/JetBrains/kotlin"),
            Library("Coroutines", LicenseType.APACHE2_0, "https://github.com/Kotlin/kotlinx.coroutines"),
            Library("Serialization", LicenseType.APACHE2_0, "https://github.com/Kotlin/kotlinx.serialization"),
            Library("Bouncy Castle", LicenseType.MIT, "https://github.com/bcgit/bc-java"),
            Library("Androidx libraries", LicenseType.APACHE2_0, "https://github.com/androidx/androidx"),
            Library("AppAuth", LicenseType.APACHE2_0, "https://github.com/openid/AppAuth-Android"),
            Library("JWTDecode", LicenseType.MIT, "https://github.com/auth0/JWTDecode.Android"),
            Library("Okio", LicenseType.APACHE2_0, "https://github.com/square/okio"),
            Library("Commons IO", LicenseType.APACHE2_0, "https://github.com/apache/commons-io"),
            Library("Ant", LicenseType.APACHE2_0, "https://github.com/apache/ant"),
            Library("James", LicenseType.APACHE2_0, "https://github.com/apache/james-mime4j/tree/master"),
            Library("Moshi", LicenseType.APACHE2_0, "https://github.com/square/moshi"),
            Library("TokenAutocomplete", LicenseType.APACHE2_0, "https://github.com/splitwise/TokenAutoComplete"),
            Library("ShowCaseView", LicenseType.APACHE2_0, "https://github.com/amlcurran/ShowcaseView"),
            Library("Timber", LicenseType.APACHE2_0, "https://github.com/JakeWharton/timber"),
            Library("Butterknife", LicenseType.APACHE2_0, "https://github.com/JakeWharton/butterknife"),
            Library("Renderers", LicenseType.APACHE2_0, "https://github.com/pedrovgs/Renderers"),
            Library("Glide", LicenseType.APACHE2_0, "https://github.com/bumptech/glide"),
            Library("AppIntro", LicenseType.APACHE2_0, "https://github.com/AppIntro/AppIntro"),
            Library("SwipeLayout", LicenseType.MIT, "https://github.com/daimajia/AndroidSwipeLayout"),
            Library("Dexter", LicenseType.APACHE2_0, "https://github.com/Karumi/Dexter"),
            Library("SafeContentResolver", LicenseType.APACHE2_0, "https://github.com/cketti/SafeContentResolver"),
            Library("CircleImageView", LicenseType.APACHE2_0, "https://github.com/hdodenhof/CircleImageView"),
            Library("Jsoup", LicenseType.MIT, "https://github.com/jhy/jsoup"),
            Library("Preferencex", LicenseType.APACHE2_0, "https://github.com/takisoft/preferencex-android"),
            Library("Groupie", LicenseType.MIT, "https://github.com/lisawray/groupie"),
            Library("Biweekly", LicenseType.BSD2_CLAUSE_SIMPLIFIED, "https://github.com/mangstadt/biweekly"),
            Library("MiniDNS", LicenseType.APACHE2_0, "https://github.com/MiniDNS/minidns"),
            Library("jdom", LicenseType.JDOM, "https://github.com/hunterhacker/jdom"),
            Library("Dagger Hilt", LicenseType.APACHE2_0, "https://github.com/google/dagger/tree/master/java/dagger/hilt"),
            Library("Truth", LicenseType.APACHE2_0, "https://github.com/google/truth"),
            Library("Spoon", LicenseType.APACHE2_0, "https://github.com/square/spoon"),
            Library("Mockito", LicenseType.MIT, "https://github.com/mockito/mockito"),
            Library("Mockito Kotlin", LicenseType.MIT, "https://github.com/mockito/mockito-kotlin"),
            Library("Mockk", LicenseType.APACHE2_0, "https://github.com/mockk/mockk"),
            Library("Cucumber Android", LicenseType.MIT, "https://github.com/cucumber/cucumber-android"),
            Library("Cucumber Picocontainer", LicenseType.MIT, "https://github.com/cucumber/cucumber-jvm/tree/main/cucumber-picocontainer"),
            Library("Robolectric", LicenseType.MIT, "https://github.com/robolectric/robolectric"),
            Library("JUnit", LicenseType.ECLIPSE_PUBLIC, "https://github.com/junit-team/junit4"),
            Library("Barista", LicenseType.APACHE2_0, "https://github.com/AdevintaSpain/Barista"),
        )
    }

    private data class Library(
        val name: String,
        val licenseType: LicenseType,
        val url: String,
    ) {

        fun toTableRow(context: Activity): TableRow {
            val tableRow = context.layoutInflater.inflate(R.layout.about_library_row, null) as TableRow
            val nameTextView = tableRow.getChildAt(0) as TextView
            nameTextView.text = HtmlCompat.fromHtml("<a href=\"$url\">$name</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
            nameTextView.setOnClickListener {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            (tableRow.getChildAt(1) as TextView).text = licenseType.toText()
            return tableRow
        }
    }

    private enum class LicenseType {
        APACHE2_0,
        MIT,
        GPLv3,
        GPLv2,
        AGPL,
        BSD2_CLAUSE_SIMPLIFIED,
        ECLIPSE_PUBLIC,
        JDOM,
        MISSING;

        fun toText(): String = when(this) {
            APACHE2_0 -> "Apache 2.0"
            MIT -> "MIT"
            GPLv2 -> "GPL v2"
            GPLv3 -> "GPL v3"
            AGPL -> "AGPL v3.0"
            BSD2_CLAUSE_SIMPLIFIED -> "BSD"
            ECLIPSE_PUBLIC -> "Eclipse 1.0"
            JDOM -> "Custom license"
            MISSING -> "libetpan license"
        }
    }
}
