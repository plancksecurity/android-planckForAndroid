package com.fsck.k9.planck.infrastructure.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import com.fsck.k9.BuildConfig
import com.fsck.k9.R

val formattedVersionForUserManual: String =
    BuildConfig.BASE_VERSION.replace('.', '-')


fun Activity.showUserManual() {
    showDocsForCurrentVersion(R.string.planck_documentation_url, useVersion = true)
}

fun Activity.showTermsAndConditions() {
    showDocsForCurrentVersion(R.string.planck_terms_and_conditions_url)
}

private fun Activity.showDocsForCurrentVersion(@StringRes urlResource: Int, useVersion: Boolean = false) {
    val browserIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(
            if (useVersion) {
                getString(urlResource, formattedVersionForUserManual)
            } else {
                getString(urlResource)
            }
        )
    )
    startActivity(browserIntent)
}