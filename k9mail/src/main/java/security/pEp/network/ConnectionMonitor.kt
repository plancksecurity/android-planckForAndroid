package security.pEp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.net.NetworkRequest
import com.fsck.k9.K9
import timber.log.Timber
import java.lang.IllegalStateException

class ConnectionMonitor {
    private var connectivityManager: ConnectivityManager? = null
    private var callback: ConnectionMonitorCallback? = null

    private val isConnected: Boolean
        get() {
            val netInfo = connectivityManager?.activeNetworkInfo
            return netInfo != null && netInfo.state == NetworkInfo.State.CONNECTED
        }

    private val defaultK9Callback = object: ConnectionMonitorCallback() {
        override fun onConnectivityAvailable(wasConnected: Boolean) {
            if (!wasConnected) {
                K9.setServicesEnabled(K9.app)
            }
        }

        override fun onConnectivityLost() {
            Timber.e("Connectivity was lost")
        }
    }

    fun register(context: Context) = register(context, null)

    fun register(context: Context, callback: ConnectionMonitorCallback?) {
        connectivityManager?.let { throw IllegalStateException("ConnectionMonitor already registered") }
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerCallbackForNetworkConnectivity(callback?.also { this.callback = it } ?: defaultK9Callback)
    }

    private fun registerCallbackForNetworkConnectivity(callback: ConnectionMonitorCallback) {
        val managerCallback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {
            private var wasConnected = isConnected
            override fun onAvailable(network: Network) {
                callback.onConnectivityAvailable(wasConnected)
                wasConnected = true
            }

            override fun onLost(network: Network) {
                callback.onConnectivityLost()
                wasConnected = false
            }
        }
        connectivityManager?.registerNetworkCallback(NetworkRequest.Builder().build(), managerCallback)
    }
}

open class ConnectionMonitorCallback {
    open fun onConnectivityAvailable(wasConnected: Boolean) {}

    open fun onConnectivityLost() {}
}