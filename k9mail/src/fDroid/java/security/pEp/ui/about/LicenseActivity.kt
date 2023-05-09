package security.pEp.ui.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.fsck.k9.R
import com.fsck.k9.pEp.PepActivity
import security.planck.ui.toolbar.ToolBarCustomizer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
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
        findViewById<TextView>(R.id.licenseText).text =
            HtmlCompat.fromHtml(getLicenseTextFromResources(), HtmlCompat.FROM_HTML_MODE_LEGACY)
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

    private fun getLicenseTextFromResources(): String {
        val inputStream: InputStream = resources.openRawResource(R.raw.gplv3license)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        bufferedReader.use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                line = line.replace("<", "&lt;").replace(">", "&gt;")
                sb.append(line)
                sb.append("<br/>")
                line = reader.readLine()
            }
        }
        return sb.toString()
    }
}


fun openLicenseActivity(context: Context) {
    context.startActivity(Intent(context, LicenseActivity::class.java))
}
