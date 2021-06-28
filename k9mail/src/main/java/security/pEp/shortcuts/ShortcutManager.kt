package security.pEp.shortcuts

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.fsck.k9.R
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.activity.compose.MessageActions

object ShortcutManager {
    fun createComposeDynamicShortcut(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val composeIntent = MessageActions.getDefaultComposeShortcutIntent(context)
            val composeShortcut = ShortcutInfo.Builder(context, MessageCompose.SHORTCUT_COMPOSE)
                .setShortLabel(context.getString(R.string.compose_action))
                .setLongLabel(context.getString(R.string.compose_action))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_compose))
                .setIntent(composeIntent)
                .build()
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager.dynamicShortcuts = listOf(composeShortcut)
        }
    }

    fun removeComposeDynamicShortcut(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager.removeDynamicShortcuts(listOf(MessageCompose.SHORTCUT_COMPOSE))
        }
    }
}