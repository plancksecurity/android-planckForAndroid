package security.planck.ui.about

import android.content.Context
import android.content.Intent
import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.fsck.k9.R
import com.fsck.k9.pEp.PepActivity
import com.fsck.k9.pEp.ui.tools.ThemeManager
import security.planck.ui.toolbar.ToolBarCustomizer
import javax.inject.Inject


class LicenseActivity : PepActivity() {

    @Inject
    lateinit var toolbarCustomizer: ToolBarCustomizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViews(R.layout.activity_license)
        setUpToolbar(true)

        toolbarCustomizer.setToolbarColor(ThemeManager.getToolbarColor(this, ThemeManager.ToolbarType.DEFAULT))
        initializeToolbar(true, getString(R.string.license))
        val licenseText = findViewById<TextView>(R.id.licenseText);
        licenseText.text = HtmlCompat.fromHtml(getString(R.string.eula_license), HtmlCompat.FROM_HTML_MODE_LEGACY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            licenseText.justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
        }
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return true
    }
}


fun openLicenseActivity(context: Context) {
    context.startActivity(Intent(context, LicenseActivity::class.java))
}