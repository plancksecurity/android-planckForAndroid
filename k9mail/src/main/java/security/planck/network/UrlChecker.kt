package security.planck.network

import android.webkit.URLUtil
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class UrlChecker @Inject constructor() {

    fun isValidUrl(url: String?): Boolean = URLUtil.isValidUrl(URLUtil.guessUrl(url))

    fun isUrlReachable(urlString: String?): Boolean {
        return kotlin.runCatching {
            val url = URL(urlString)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.responseCode == HttpURLConnection.HTTP_OK
        }.getOrDefault(false)
    }
}
