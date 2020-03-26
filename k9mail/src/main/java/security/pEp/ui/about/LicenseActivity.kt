package security.pEp.ui.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.fsck.k9.R
import com.fsck.k9.pEp.PepActivity
import kotlinx.android.synthetic.main.activity_license.*
import security.pEp.ui.toolbar.ToolBarCustomizer
import javax.inject.Inject


class LicenseActivity : PepActivity() {

    @Inject
    lateinit var toolbarCustomizer: ToolBarCustomizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViews(R.layout.activity_license)
        setUpToolbar(true)

        toolbarCustomizer.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        initializeToolbar(true, getString(R.string.license))
        licenseText.text = HtmlCompat.fromHtml(getString(R.string.gpl_license), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }
}

fun openLicenseActivity(context: Context) {
    context.startActivity(Intent(context, LicenseActivity::class.java))
}
