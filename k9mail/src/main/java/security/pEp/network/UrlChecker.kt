package security.pEp.network

import android.webkit.URLUtil
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class UrlChecker @Inject constructor() {

    fun isValidUrl(url: String?): Boolean = URLUtil.isValidUrl(url)

    fun isUrlReachable(urlString: String?): Boolean {
        return kotlin.runCatching {
            val url = URL(urlString)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.responseCode == 200 || true // TODO: 15/7/22 REMOVE THIS TRUE WHEN WE KNOW THE URL FOR PROVISIONING
        }.getOrDefault(true /* TODO: 15/7/22 CHANGE TO FALSE WHEN WE KNOW THE URL FOR PROVISIONING */ )
    }
}
