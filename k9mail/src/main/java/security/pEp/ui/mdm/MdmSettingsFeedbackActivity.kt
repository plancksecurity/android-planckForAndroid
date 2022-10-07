package security.pEp.ui.mdm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.fsck.k9.R
import com.fsck.k9.pEp.PepActivity
import javax.inject.Inject

class MdmSettingsFeedbackActivity : PepActivity(), MdmSettingsFeedbackView {
    private var settingsTextView: TextView? = null

    @Inject
    lateinit var presenter: MdmSettingsFeedbackPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        presenter.initialize(this)
        presenter.displaySettings()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.mdm_settings_feedback_option, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share -> share()
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setupViews() {
        setContentView(R.layout.activity_mdm_settings_feedback)
        setUpToolbar(true)
        settingsTextView = findViewById(R.id.settingsText)
    }

    private fun share() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, getString(R.string.mdm_settings_feedback_title))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mdm_settings_feedback_title))
            putExtra(Intent.EXTRA_TEXT, settingsTextView?.text ?: "")
            type = "text/plain"
        }

        startActivity(Intent.createChooser(sendIntent, null))
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun displaySettings(settingsText: String) {
        settingsTextView?.text = HtmlCompat.fromHtml(
            settingsText,
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, MdmSettingsFeedbackActivity::class.java))
        }
    }
}
