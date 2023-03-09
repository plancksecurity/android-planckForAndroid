package com.fsck.k9.pEp

import android.app.Activity
import android.content.Intent

fun Activity.launchIntent(intent: Intent): Boolean {
    return if (intent.resolveActivity(this.applicationContext.packageManager) != null) {
        this.startActivity(intent)
        true
    } else {
        false
    }
}
