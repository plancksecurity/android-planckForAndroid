package com.fsck.k9.pEp.ui.activities

import android.os.Bundle
import android.webkit.CookieManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fsck.k9.R
import com.fsck.k9.activity.SettingsActivity


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isWebViewEnabled) {
            Toast.makeText(this, getString(R.string.webview_not_installed), Toast.LENGTH_LONG)
                .show()
        } else {
            SettingsActivity.actionBasicStart(this)
        }
        finish()
    }

    val isWebViewEnabled: Boolean = kotlin.runCatching { CookieManager.getInstance() }.isSuccess
}
