package com.fsck.k9.activity;

import static com.fsck.k9.activity.MessageList.ACTION_SHORTCUT;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.planck.ui.activities.SplashActivity;
import com.fsck.k9.search.SearchAccount;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LauncherShortcuts extends AccountList {
    @Override
    public void onCreate(Bundle icicle) {
        // finish() immediately if we aren't supposed to be here
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            finish();
            return;
        }

        super.onCreate(icicle);
    }

    @Override
    protected boolean displaySpecialAccounts() {
        return true;
    }

    @Override
    protected void onAccountSelected(BaseAccount account) {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        if (shortcutManager.isRequestPinShortcutSupported()) {
            Intent shortcutIntent = null;
            if (Preferences.getPreferences(this).getAvailableAccounts().size() == 0) {
                shortcutIntent = new Intent(this, SplashActivity.class);
                shortcutIntent.setAction(ACTION_SHORTCUT);
            } else if (account instanceof SearchAccount) {
                SearchAccount searchAccount = (SearchAccount) account;
                shortcutIntent = MessageList.shortcutIntent(this, searchAccount.getId());
            } else {
                shortcutIntent = FolderList.actionHandleAccountIntent(this, (Account) account, true);
                shortcutIntent.setAction(ACTION_SHORTCUT);
            }

            String description = account.getDescription();
            if (description == null || description.isEmpty()) {
                description = account.getEmail();
            }

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, account.getUuid())
                    .setShortLabel(description)
                    .setLongLabel(description)
                    .setIcon(Icon.createWithResource(this, R.mipmap.icon))
                    .setIntent(shortcutIntent)
                    .build();

            shortcutManager.requestPinShortcut(shortcut, null);
        } else {
            Toast.makeText(this, R.string.device_cannot_create_pinned_shortcut_feedback, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
