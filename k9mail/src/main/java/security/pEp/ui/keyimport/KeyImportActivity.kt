package security.pEp.ui.keyimport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
        if (isValidKeyImportIntent(intent)) {
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

    fun onAccept() {
        presenter.onAccept()
    }

    fun onReject() {
        presenter.onReject()
    }

    private fun isValidKeyImportIntent(intent: Intent): Boolean = when {
        intent.hasExtra(ACCOUNT_EXTRA) -> true
        else -> throw IllegalArgumentException("The provided intent does not contain the required extras")

    }
}

fun Activity.showImportKeyDialog(account: String) {
    val intent = Intent(this, KeyImportActivity::class.java)
    intent.putExtra(ACCOUNT_EXTRA, account)
    startActivity(intent)
}

