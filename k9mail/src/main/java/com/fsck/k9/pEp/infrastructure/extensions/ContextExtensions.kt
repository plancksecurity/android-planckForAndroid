package com.fsck.k9.pEp

import android.content.Context
import android.content.Intent

fun Context.launchIntent(intent: Intent): Boolean {
    return if (intent.resolveActivity(this.applicationContext.packageManager) != null) {
        try {
            this.startActivity(intent)
        } catch (e: Exception) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
        }
        true
    } else {
        false
    }
}