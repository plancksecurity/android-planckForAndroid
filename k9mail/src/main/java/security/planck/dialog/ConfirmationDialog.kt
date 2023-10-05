package security.planck.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult

private const val ARG_TAG = "dialogTag"
private const val ARG_TITLE = "title"
private const val ARG_MESSAGE = "message"
private const val ARG_CONFIRM_TEXT = "confirm"
private const val ARG_CANCEL_TEXT = "cancel"
private const val ARG_NEUTRAL_TEXT = "neutral"

class ConfirmationDialog : DialogFragment(), DialogInterface.OnClickListener {
    private val dialogTag: String by lazy {
        arguments?.getString(ARG_TAG) ?: error("tag argument needed")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(
            requireContext()
        )
        builder.setTitle(arguments?.getCharSequence(ARG_TITLE))
        builder.setMessage(arguments?.getCharSequence(ARG_MESSAGE))
        arguments?.getCharSequence(ARG_CONFIRM_TEXT)?.let { confirmText ->
            builder.setPositiveButton(confirmText, this)
        }
        arguments?.getCharSequence(ARG_CANCEL_TEXT)?.let { cancelText ->
            builder.setNegativeButton(cancelText, this)
        }
        arguments?.getCharSequence(ARG_NEUTRAL_TEXT)?.let { neutralText ->
            builder.setNeutralButton(neutralText, this)
        }
        isCancelable = false
        return builder.create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        setFragmentResult(dialogTag, bundleOf(RESULT_KEY to CANCELLED))
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        setFragmentResult(dialogTag, bundleOf(RESULT_KEY to which))
    }

    companion object {
        const val RESULT_KEY = "security.planck.dialog.ConfirmationDialog"
        const val CANCELLED = 0
    }
}

private fun newInstance(
    tag: String,
    title: CharSequence,
    message: CharSequence,
    confirmText: CharSequence?,
    cancelText: CharSequence?,
    neutralText: CharSequence?
): ConfirmationDialog = ConfirmationDialog().apply {
    arguments = bundleOf(
        ARG_TAG to tag,
        ARG_TITLE to title,
        ARG_MESSAGE to message,
        ARG_CONFIRM_TEXT to confirmText,
        ARG_CANCEL_TEXT to cancelText,
        ARG_NEUTRAL_TEXT to neutralText
    )
}

private fun createAndShowConfirmationDialog(
    fragmentManager: FragmentManager,
    tag: String,
    title: CharSequence,
    message: CharSequence,
    confirmText: CharSequence? = null,
    cancelText: CharSequence? = null,
    neutralText: CharSequence? = null,
) {
    val fragment = newInstance(
        tag, title, message, confirmText, cancelText, neutralText
    )
    fragmentManager
        .beginTransaction()
        .add(fragment, tag)
        .commitAllowingStateLoss()
}

fun Fragment.showConfirmationDialog(
    tag: String,
    title: CharSequence,
    message: CharSequence,
    confirmText: CharSequence? = null,
    cancelText: CharSequence? = null,
    neutralText: CharSequence? = null,
) {
    createAndShowConfirmationDialog(
        parentFragmentManager,
        tag, title, message, confirmText, cancelText, neutralText
    )
}

@JvmOverloads
fun AppCompatActivity.showConfirmationDialog(
    tag: String,
    title: CharSequence,
    message: CharSequence,
    confirmText: CharSequence? = null,
    cancelText: CharSequence? = null,
    neutralText: CharSequence? = null,
) {
    createAndShowConfirmationDialog(
        supportFragmentManager,
        tag, title, message, confirmText, cancelText, neutralText
    )
}