package security.planck.auth

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import javax.inject.Inject

const val OAUTH_TOKEN_REVOKED_ACTION = "OAUTH_TOKEN_REVOKED"
const val EXTRA_ACCOUNT_UUID = "ACCOUNT_UUID"

class OAuthTokenRevokedReceiver @Inject constructor(): BroadcastReceiver() {
    private var listener: OAuthTokenRevokedListener? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == OAUTH_TOKEN_REVOKED_ACTION) {
            val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID)
            accountUuid?.let { listener?.onTokenRevoked(accountUuid) }
        }
    }

    fun register (activity: Activity) {
        activity.registerReceiver(this, IntentFilter(OAUTH_TOKEN_REVOKED_ACTION))
        this.listener = activity as OAuthTokenRevokedListener
    }

    fun unregister(activity: Activity) {
        activity.unregisterReceiver(this)
        this.listener = null
    }

    companion object {
        @JvmStatic
        fun sendOAuthTokenRevokedBroadcast(
            context: Context,
            accountUuid: String,
        ) {
            Intent().apply {
                putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
                action = OAUTH_TOKEN_REVOKED_ACTION
            }.also { context.sendBroadcast(it) }
        }
    }
}
