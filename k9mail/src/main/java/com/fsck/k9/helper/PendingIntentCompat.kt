package com.fsck.k9.helper

import android.app.PendingIntent
import android.os.Build

object PendingIntentCompat {
    const val FLAG_IMMUTABLE = PendingIntent.FLAG_IMMUTABLE

    @JvmField
    val FLAG_MUTABLE = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
}
