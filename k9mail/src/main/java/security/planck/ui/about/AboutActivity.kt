package security.planck.ui.about

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
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
        if (BuildConfig.IS_ENTERPRISE) {
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

        val librariesString = buildLibrariesHtml()
        binding.librariesText.movementMethod = LinkMovementMethod.getInstance()
        binding.librariesText.text = HtmlCompat.fromHtml(librariesString, HtmlCompat.FROM_HTML_MODE_LEGACY)

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
                .append(String.format(getString(R.string.app_copyright_fmt), year, year))

        return html.toString()
    }

    private fun buildLibrariesHtml(): String {
        val libs = StringBuilder()
        USED_LIBRARIES
                .forEach { entry ->
                    libs.append("<p>&emsp;<a href=\"${entry.value}\"><b>${entry.key}</b></a></p>")
                }

        val html = StringBuilder()
                .append("<p>${getString(R.string.app_libraries)}</p>")
                .append(libs)
        return html.toString()
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

        val USED_LIBRARIES = mapOf(
            "planckCoreSequoiaBackend" to "https://git.planck.security/foundation/planckCoreSequoiaBackend",
            "Sequoia PGP" to "https://gitlab.com/sequoia-pgp/sequoia",
            "planck core Version 3" to "https://git.planck.security/foundation/planckCoreV3",
            "planck JNI Wrapper" to "https://git.planck.security/foundation/planckJNIWrapper",
            "libetpan" to "https://git.planck.security/foundation/libetpan",
            "libpEpCxx11" to "https://git.planck.security/foundation/libpEpCxx11",
            "libPlanckWrapper" to "https://git.planck.security/foundation/libPlanckWrapper",
            "libPlanckTransport" to "https://git.planck.security/foundation/libPlanckTransport",
            "yml2" to "https://git.planck.security/foundation/yml2",

            "Kotlin" to "https://github.com/JetBrains/kotlin.git",
            "Coroutines" to "https://github.com/Kotlin/kotlinx.coroutines",
            "Serialization" to "https://www.bouncycastle.org/java.html",
            "Bouncy Castle" to "https://github.com/Kotlin/kotlinx.serialization",
            "Androidx libraries" to "https://github.com/androidx/androidx",

            "AppAuth" to "https://github.com/openid/AppAuth-Android.git",
            "JWTDecode" to "https://github.com/auth0/JWTDecode.Android.git",
            "Okio" to "https://github.com/square/okio.git",
            "Commons IO" to "https://github.com/apache/commons-io.git",
            "Ant" to "https://github.com/apache/ant",
            "James" to "https://github.com/apache/james-mime4j/tree/master",
            "Java Concurrency In Practice" to "https://jcip.net/",
            "Moshi" to "https://github.com/square/moshi.git",
            "TokenAutocomplete" to "https://github.com/splitwise/TokenAutoComplete.git",
            "ShowCaseView" to "https://github.com/amlcurran/ShowcaseView.git",
            "Timber" to "https://github.com/JakeWharton/timber.git",
            "Butterknife" to "https://github.com/JakeWharton/butterknife.git",
            "Renderers" to "https://github.com/pedrovgs/Renderers.git",
            "Glide" to "https://github.com/bumptech/glide.git",
            "AppIntro" to "https://github.com/AppIntro/AppIntro.git",
            "SwipeLayout" to "https://github.com/daimajia/AndroidSwipeLayout.git",
            "Dexter" to "https://github.com/Karumi/Dexter.git",
            "SafeContentResolver" to "https://github.com/cketti/SafeContentResolver.git",
            "CircleImageView" to "https://github.com/hdodenhof/CircleImageView.git",
            "Jsoup" to "https://github.com/jhy/jsoup.git",
            "Preferencex" to "https://github.com/takisoft/preferencex-android.git",
            "Groupie" to "https://github.com/lisawray/groupie.git",
            "Biweekly" to "https://github.com/mangstadt/biweekly.git",

            "MiniDNS" to "https://github.com/MiniDNS/minidns.git",
            "jdom" to "https://github.com/hunterhacker/jdom.git",
            "Dagger Hilt" to "https://github.com/google/dagger/tree/master/java/dagger/hilt",
            "Bouncy Castle" to "https://www.bouncycastle.org/java.html",

            "Truth" to "https://github.com/google/truth.git",
            "Spoon" to "https://github.com/square/spoon",
            "Mockito" to "https://github.com/mockito/mockito.git",
            "Mockito Kotlin" to "https://github.com/mockito/mockito-kotlin.git",
            "Mockk" to "https://github.com/mockk/mockk.git",
            "Cucumber Android" to "https://github.com/cucumber/cucumber-android.git",
            "Cucumber Picocontainer" to "https://github.com/cucumber/cucumber-jvm/tree/main/cucumber-picocontainer",
            "Robolectric" to "https://robolectric.org/",
            "JUnit" to "https://junit.org/junit4/",
            "Barista" to "https://github.com/AdevintaSpain/Barista.git",
        )
    }
}
