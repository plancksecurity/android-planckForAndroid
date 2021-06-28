package security.pEp.shortcuts

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.activity.compose.MessageActions
import javax.inject.Inject
import javax.inject.Named

class ShortcutManager @Inject constructor(
    @Named("ActivityContext") private val context: Context,
    private val preferences: Preferences
) {
    fun createComposeDynamicShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
            preferences.availableAccounts.isNotEmpty()) {
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

    fun removeComposeDynamicShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager.removeDynamicShortcuts(listOf(MessageCompose.SHORTCUT_COMPOSE))
        }
    }
}