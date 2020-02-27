package com.fsck.k9.pEp.ui.privacy.status

import android.content.Context
import com.fsck.k9.K9
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.ui.HandshakeData
import foundation.pEp.jniadapter.Identity
import java.util.*

class PEpStatusTrustwordsPresenter(
        myselfAddress: String, context: Context,
        private val trustwordsView: PEpStatusTrustwordsView
) {

    private val myself: Identity = PEpUtils.createIdentity(Address(myselfAddress), context)
    private val pep: PEpProvider = (context.applicationContext as K9).getpEpProvider()
    var areTrustwordsShort: Boolean = true
        private set
    private var currentLanguage: String = getLanguageForTrustwords()
    private lateinit var localesMap: Map<String, String>

    companion object {
        const val PEP_DEFAULT_LANGUAGE = "en"
    }

    private fun getLocalesMapFromPep() : Map<String, String> {
        return pep.obtainLanguages().asSequence().associate { Pair(it.value.language, it.value.locale) }
    }

    fun getLanguageList() : List<String> {
        //localesMap = if(localesMap.isNotEmpty()) localesMap else getLocalesMapFromPep()
        return localesMap.keys.toList()
    }

    private fun isLanguageInPEPLanguages(language: String): Boolean {
        localesMap = if(::localesMap.isInitialized) localesMap else getLocalesMapFromPep()
        return localesMap.values.contains(language)
    }

    private fun getLanguageForTrustwords() : String {
        val language = if (K9.getK9Language().isEmpty()) Locale.getDefault().language else K9.getK9Language()
        return if (isLanguageInPEPLanguages(language)) { language } else PEP_DEFAULT_LANGUAGE
    }

    fun loadTrustwords(partner: Identity) {
        trustwordsView.enableButtons(false)
        retrieveTrustwords(partner, null, null)
    }

    fun changeTrustwordsSize(partner: Identity, areShort: Boolean) {
        if(areShort != areTrustwordsShort) {
            trustwordsView.enableButtons(false)
            retrieveTrustwords(partner, areShort, null)
        }
    }

    fun changeTrustwordsLanguage(partner: Identity, languageKey: String) {
        retrieveTrustwords(partner, null, localesMap[languageKey])
    }

    private fun retrieveTrustwords(partner: Identity, areShort: Boolean?, language: String?) {
        areTrustwordsShort = areShort?:areTrustwordsShort
        currentLanguage = language ?: currentLanguage
        pep.obtainTrustwords(myself, partner, currentLanguage,
            false,
            object : PEpProvider.ResultCallback<HandshakeData> {
                override fun onLoaded(handshakeData: HandshakeData) {
                    showTrustwords(handshakeData)
                }

                override fun onError(throwable: Throwable) {
                    trustwordsView.reportError(throwable.message)
                }
            })
    }

    private fun showTrustwords(handshakeData: HandshakeData) {
        val fullTrustwords = handshakeData.fullTrustwords
        val shortTrustwords = handshakeData.shortTrustwords
        if (areTrustwordsShort) {
            trustwordsView.setShortTrustwords(changeSpacesToTabs(shortTrustwords))

        } else {
            trustwordsView.setLongTrustwords(changeSpacesToTabs(fullTrustwords))
        }
    }

    fun rejectTrustwords(partner: Identity) {
        trustwordsView.enableButtons(false)
        pep.keyMistrusted(partner)
        pep.getRating(partner)
    }

    fun confirmTrustwords(partner: Identity) {
        trustwordsView.enableButtons(false)
        var newpartner = partner
        if (partner.user_id == null || partner.user_id.isEmpty()) {
            val tempFpr = partner.fpr
            newpartner = pep.updateIdentity(partner)
            newpartner.fpr = tempFpr
        }
        pep.trustPersonaKey(newpartner)
    }

    private fun changeSpacesToTabs(text: String) : String {
        return text.split(" ").joinToString("    ")
    }

    interface PEpStatusTrustwordsView {
        fun setLongTrustwords(newTrustwords: String)
        fun setShortTrustwords(newTrustwords: String)
        fun reportError(errorMessage: String?)
        fun enableButtons(enabled: Boolean)
    }

}