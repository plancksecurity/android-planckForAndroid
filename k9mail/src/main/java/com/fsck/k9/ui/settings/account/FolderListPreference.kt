package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.ListPreference
import android.util.AttributeSet
import com.fsck.k9.K9
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.ui.folders.FolderNameFormatter
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * A [ListPreference] that allows selecting one of an account's folders.
 */
@SuppressLint("RestrictedApi")
class FolderListPreference
@JvmOverloads
constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = TypedArrayUtils.getAttr(context, androidx.preference.R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle),
        defStyleRes: Int = 0
) : ListPreference(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
    private val folderNameFormatter: FolderNameFormatter by inject()

    var folders: List<Folder>
        get() = throw UnsupportedOperationException()
        set(folders) {
            entries = (listOf(K9.FOLDER_NONE) + folders.map { folderNameFormatter.displayName(it) }).toTypedArray()
            entryValues = (listOf(K9.FOLDER_NONE) + folders.map { it.serverId }).toTypedArray()

            isEnabled = true
            if (!entryValues.contains(value)) {
                setValueIndex(0)
            }
        }


    init {
        entries = emptyArray()
        entryValues = emptyArray()
        isEnabled = false
    }

    override fun getSummary(): CharSequence {
        // While folders are being loaded the summary returned by ListPreference will be empty. This leads to the
        // summary view being hidden. Once folders are loaded the summary updates and the list height changes. This
        // adds quite a bit of visual clutter. We avoid that by always returning a non-empty summary value.
        val summary = super.getSummary()
        return summary?.let { it.ifEmpty { PLACEHOLDER_SUMMARY } } ?: PLACEHOLDER_SUMMARY
    }


    companion object {
        private const val PLACEHOLDER_SUMMARY = " "
    }
}
