package security.planck.network

import android.util.Patterns
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class UrlChecker @Inject constructor() {

    fun isValidUrl(url: String?): Boolean =
        url?.let { Patterns.WEB_URL.matcher(url).matches() } ?: false

    fun isUrlReachable(urlString: String?): Boolean {
        return kotlin.runCatching {
            val url = URL(urlString)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.responseCode == HttpURLConnection.HTTP_OK
        }.getOrDefault(false)
    }
}
