package com.fsck.k9.activity

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.Toast
import com.fsck.k9.Account
import com.fsck.k9.BaseAccount
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.planck.ui.activities.SplashActivity
import com.fsck.k9.search.SearchAccount
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LauncherShortcuts : AccountList() {
    override fun onCreate(icicle: Bundle?) {
        // finish() immediately if we aren't supposed to be here
        if (Intent.ACTION_CREATE_SHORTCUT != intent.action) {
            finish()
            return
        }
        super.onCreate(icicle)
    }

    override fun displaySpecialAccounts(): Boolean {
        return true
    }

    override fun onAccountSelected(account: BaseAccount) {
        val shortcutManager = getSystemService(
            ShortcutManager::class.java
        )
        if (shortcutManager.isRequestPinShortcutSupported) {
            val shortcutIntent: Intent
            if (Preferences.getPreferences(this).availableAccounts.isEmpty()) {
                shortcutIntent = Intent(this, SplashActivity::class.java)
                shortcutIntent.setAction(MessageList.ACTION_SHORTCUT)
            } else if (account is SearchAccount) {
                shortcutIntent = MessageList.shortcutIntent(this, account.id)
            } else {
                shortcutIntent =
                    FolderList.actionHandleAccountIntent(this, account as Account, true)
                shortcutIntent.setAction(MessageList.ACTION_SHORTCUT)
            }
            val description =
                if (account.description.isNullOrBlank()) {
                    account.email
                } else {
                    account.description
                }
            val shortcut = ShortcutInfo.Builder(this, account.uuid)
                .setShortLabel(description)
                .setLongLabel(description)
                .setIcon(Icon.createWithResource(this, R.mipmap.icon))
                .setIntent(shortcutIntent)
                .build()
            shortcutManager.requestPinShortcut(shortcut, null)
        } else {
            Toast.makeText(
                this,
                R.string.device_cannot_create_pinned_shortcut_feedback,
                Toast.LENGTH_SHORT
            ).show()
        }
        finish()
    }
}
