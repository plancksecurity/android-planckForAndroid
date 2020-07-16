package com.fsck.k9.ui.folders

import android.content.res.Resources
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderType
import java.util.*

class FolderNameFormatter(private val resources: Resources) {
    init {
        val configuration = resources.configuration
        configuration.setLocale(Locale(K9.getK9CurrentLanguage()))
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun displayName(folder: Folder): String = when (folder.type) {
        FolderType.INBOX -> resources.getString(R.string.special_mailbox_name_inbox)
        else -> folder.name
    }
}
