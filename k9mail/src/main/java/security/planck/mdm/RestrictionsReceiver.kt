package security.planck.mdm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import javax.inject.Inject

class RestrictionsReceiver @Inject constructor(
    private val configurationManager: ConfigurationManager
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        configurationManager.loadConfigurations()
    }
}

