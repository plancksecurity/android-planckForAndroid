package security.pEp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.net.NetworkRequest

class ConnectionMonitor(context: Context, private val callback: ConnectionMonitorCallback) {
    private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val isConnected: Boolean
        get() {
            val netInfo = connectivityManager.activeNetworkInfo
            return netInfo != null && netInfo.state == NetworkInfo.State.CONNECTED
        }

    init {
        registerCallbackForNetworkConnectivity()
    }

    private fun registerCallbackForNetworkConnectivity() {
        val callback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {
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
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }
}

open class ConnectionMonitorCallback {
    open fun onConnectivityAvailable(wasConnected: Boolean) {}

    open fun onConnectivityLost() {}
}