package security.pEp.ui.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import com.fsck.k9.R
import com.fsck.k9.pEp.PepActivity
import com.fsck.k9.view.MessageWebView
import security.pEp.ui.toolbar.ToolBarCustomizer
import javax.inject.Inject

const val GPL_LICENSE = "https://pep-security.lu/gitlab/android/pep/-/raw/develop/LICENSE"

class LicenseActivity : PepActivity() {

    @Inject
    lateinit var toolbarCustomizer: ToolBarCustomizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViews(R.layout.activity_license)
        setUpToolbar(true)

        toolbarCustomizer.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        initializeToolbar(true, getString(R.string.license))

        val webView = findViewById<MessageWebView>(R.id.license_webview)
        webView.blockNetworkData(false)
        webView.setTheme()

        webView.loadUrl(GPL_LICENSE)
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
