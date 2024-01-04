package com.fsck.k9.planck.infrastructure.extensions

import android.content.Context
import com.fsck.k9.R

fun String?.isAllMessagesFolder(context: Context) =
    context.getString(R.string.search_all_messages_title) == this

fun String?.isUnifiedInboxFolder(context: Context) =
    context.getString(R.string.integrated_inbox_title) == this