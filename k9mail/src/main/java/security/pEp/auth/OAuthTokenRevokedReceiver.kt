package security.pEp.auth

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import timber.log.Timber

const val OAUTH_TOKEN_REVOKED_ACTION = "OAUTH_TOKEN_REVOKED"
const val EXTRA_ACCOUNT_UUID = "ACCOUNT_UUID"
const val EXTRA_INCOMING = "INCOMING"

class OAuthTokenRevokedReceiver: BroadcastReceiver() {
    private var listener: OAuthTokenRevokedListener? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == OAUTH_TOKEN_REVOKED_ACTION) {
            val incoming = intent.getBooleanExtra(EXTRA_INCOMING, false)
            val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID)
            accountUuid?.let { listener?.onTokenRevoked(accountUuid, incoming) }
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
            incoming: Boolean,
        ) {
            Intent().apply {
                putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
                putExtra(EXTRA_INCOMING, incoming)
                action = OAUTH_TOKEN_REVOKED_ACTION
            }.also { context.sendBroadcast(it) }
        }
    }
}