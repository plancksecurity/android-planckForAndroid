package com.fsck.k9.pEp.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fsck.k9.BuildConfig
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.pEp.ui.activities.provisioning.ProvisioningActivity


class SplashActivity : AppCompatActivity(), SplashScreen {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, ProvisioningActivity::class.java))
        finish()
    }
}
