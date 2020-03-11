package security.pEp.utils

import java.net.InetAddress
import java.net.UnknownHostException

fun checkIfAddressIsLocal(uri: String?): Boolean {
    try {
        val inetAddress = InetAddress.getByName(uri)
        return inetAddress.isSiteLocalAddress
    } catch (e: UnknownHostException) {
        e.printStackTrace()
    }
    return false
}