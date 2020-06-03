package security.pEp.ui.keyimport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.fsck.k9.R
import com.fsck.k9.pEp.PepActivity
import javax.inject.Inject

const val ACCOUNT_EXTRA = "ACCOUNT_EXTRA"


class KeyImportActivity : PepActivity(), KeyImportView {


    @Inject
    internal lateinit var presenter: KeyImportPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_key_dialog)
        setupFloatingWindow()
        val intent = intent
        if (isValidIntent(intent)) {
            val account: String = intent.getStringExtra(ACCOUNT_EXTRA) ?: ""
            presenter.initialize(this, account)
        }
    }

    override fun search(query: String) {
        //NOP
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun showPositiveFeedback() {}

    override fun renderDialog() {}

    override fun showNegativeFeedback() {}

    private fun setupFloatingWindow() {
        val params = window.attributes
        params.width = resources.getDimensionPixelSize(R.dimen.floating_width)
        params.height = resources.getDimensionPixelSize(R.dimen.floating_height)
        params.alpha = 1f
        params.dimAmount = 0.4f
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        window.attributes = params
    }

    fun onAccept() {
        presenter.onAccept()
    }

    fun onReject() {
        presenter.onReject()
    }

    companion object {
        fun showImportKeyDialog(context: Context, intent: Intent) {
            isValidIntent(intent)
            val dialogIntent = Intent(context, KeyImportActivity::class.java)
            dialogIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            dialogIntent.putExtras(intent)
            context.startActivity(dialogIntent)
        }

        private fun isValidIntent(intent: Intent): Boolean {
            check(!intent.hasExtra(ACCOUNT_EXTRA)) {
                "The provided intent does not contain the required extras"
            }
            return true
        }
    }
}