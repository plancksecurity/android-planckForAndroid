package security.pEp.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.fsck.k9.R

private const val PROGRESS_DIALOG_TAG = "progressDialog"

class PEpProgressDialog(
        private val title: String,
        private val message: String,
        private val indeterminate: Boolean) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.progress_dialog, null)
        view.findViewById<TextView>(R.id.title).text = title
        view.findViewById<TextView>(R.id.message).text = message
        view.findViewById<ProgressBar>(R.id.progressBar).isIndeterminate = indeterminate
        return view
    }

    fun closeDialog() {
        dismiss()
    }

}

fun AppCompatActivity.showProgressDialog(
        title: String,
        message: String,
        indeterminate: Boolean): PEpProgressDialog {
    val ft = supportFragmentManager.beginTransaction()
    val prev = supportFragmentManager.findFragmentByTag(PROGRESS_DIALOG_TAG)
    if (prev != null)
        ft.remove(prev)
    ft.addToBackStack(null)

    val dialogFragment = PEpProgressDialog(title, message, indeterminate)
    dialogFragment.show(ft, PROGRESS_DIALOG_TAG)
    return dialogFragment
}