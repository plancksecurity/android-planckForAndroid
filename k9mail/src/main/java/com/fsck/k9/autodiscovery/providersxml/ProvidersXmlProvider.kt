package com.fsck.k9.autodiscovery.providersxml

import android.content.Context
import android.content.res.XmlResourceParser
import com.fsck.k9.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ProvidersXmlProvider @Inject constructor(@ApplicationContext private val context: Context) {
    fun getXml(): XmlResourceParser {
        return context.resources.getXml(R.xml.providers)
    }
}
