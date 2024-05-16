package security.planck.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.fsck.k9.databinding.PermanentlyDismissibleDialogBinding

private const val ARG_TAG = "dialogTag"
private const val ARG_TITLE = "title"
private const val ARG_MESSAGE = "message"
private const val ARG_CONFIRM_TEXT = "confirm"
private const val ARG_CANCEL_TEXT = "cancel"
private const val ARG_NEUTRAL_TEXT = "neutral"

class PermanentlyDismissibleDialog : DialogFragment(), DialogInterface.OnClickListener {
    private val dialogTag: String by lazy {
        arguments?.getString(ARG_TAG) ?: error("tag argument needed")
    }
    private var _binding: PermanentlyDismissibleDialogBinding? = null
    private val binding get() = _binding!!
    private val result = Bundle()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(
            requireContext()
        )
        builder.setTitle(arguments?.getCharSequence(ARG_TITLE))
        _binding = PermanentlyDismissibleDialogBinding.inflate(layoutInflater)
        builder.setView(binding.root)
        arguments?.getCharSequence(ARG_MESSAGE)?.let {
            binding.message.text = it
        }
        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            result.putBoolean(DISMISS_RESULT_KEY, isChecked)
        }
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
        setFragmentResult(dialogTag, result.apply { putInt(RESULT_KEY, CANCELLED) })
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        setFragmentResult(dialogTag, result.apply { putInt(RESULT_KEY, which) })
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_KEY = "security.planck.dialog.PermanentlyDismissibleDialog.result"
        const val DISMISS_RESULT_KEY =
            "security.planck.dialog.PermanentlyDismissibleDialog.dismissResult"
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
): PermanentlyDismissibleDialog = PermanentlyDismissibleDialog().apply {
    arguments = bundleOf(
        ARG_TAG to tag,
        ARG_TITLE to title,
        ARG_MESSAGE to message,
        ARG_CONFIRM_TEXT to confirmText,
        ARG_CANCEL_TEXT to cancelText,
        ARG_NEUTRAL_TEXT to neutralText
    )
}

private fun createAndShowDialog(
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

@JvmOverloads
fun AppCompatActivity.showPermanentlyDismissibleDialog(
    tag: String,
    title: CharSequence,
    message: CharSequence,
    confirmText: CharSequence? = null,
    cancelText: CharSequence? = null,
    neutralText: CharSequence? = null,
) {
    createAndShowDialog(
        supportFragmentManager,
        tag, title, message, confirmText, cancelText, neutralText
    )
}