package com.fsck.k9.pEp.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fsck.k9.activity.SettingsActivity


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsActivity.actionBasicStart(this)
        finish()
    }
}
