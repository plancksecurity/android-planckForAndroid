package security.pEp.mdm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestrictionsReceiver(private val configurationManager: ConfigurationManager) :
        BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        configurationManager.loadConfigurations()
    }
}

