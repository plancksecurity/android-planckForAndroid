package security.pEp.ui.toolbar

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import com.fsck.k9.R
import com.fsck.k9.activity.compose.RecipientPresenter
import com.fsck.k9.pEp.PEpProvider

class ToolbarStatusPopUpMenu(context: Context?, anchor: View?, private val recipientPresenter: RecipientPresenter) : PopupMenu(context, anchor) {
    init {

        menuInflater.inflate(R.menu.pep_security_badge_options_menu, menu)

        menu.findItem(R.id.force_unencrypted)
                .setTitle(if (!recipientPresenter.isForceUnencrypted) R.string.pep_force_unprotected else R.string.pep_force_protected)

        menu.findItem(R.id.is_always_secure).setTitle(
                if (recipientPresenter.isAlwaysSecure) R.string.is_not_always_secure else R.string.is_always_secure
        )

        setOnMenuItemClickListener { item: MenuItem -> onPEpStatusMenuItemClick(item) }
    }

    private fun onPEpStatusMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.force_unencrypted -> {
                if (!recipientPresenter.isForceUnencrypted) {
                    item.setTitle(R.string.pep_force_protected)
                } else {
                    item.setTitle(R.string.pep_force_unprotected)
                }
                forceUnencrypted()
            }
            R.id.is_always_secure -> {
                recipientPresenter.isAlwaysSecure = !recipientPresenter.isAlwaysSecure
                item.setTitle(
                        if (recipientPresenter.isAlwaysSecure) R.string.is_not_always_secure else R.string.is_always_secure
                )
            }
            else -> return false
        }
        return true
    }

    private fun forceUnencrypted() {
        recipientPresenter.switchPrivacyProtection(PEpProvider.ProtectionScope.MESSAGE)
    }
}