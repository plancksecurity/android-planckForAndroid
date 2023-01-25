package com.fsck.k9.autodiscovery.thunderbird

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.InputStream

class ThunderbirdAutoconfigFetcher(private val okHttpClient: OkHttpClient) {

    fun fetchAutoconfigFile(url: HttpUrl): InputStream? {
        return kotlin.runCatching {
            val request = Request.Builder().url(url).build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.byteStream()
            } else {
                null
            }
        }.onFailure { Timber.e(it) }.getOrNull()
    }
}
