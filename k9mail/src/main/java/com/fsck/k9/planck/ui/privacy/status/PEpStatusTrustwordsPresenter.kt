package com.fsck.k9.planck.ui.privacy.status

import android.content.Context
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.PEpProvider
import com.fsck.k9.planck.PEpUtils
import com.fsck.k9.planck.ui.HandshakeData
import foundation.pEp.jniadapter.Identity
import kotlinx.coroutines.*
import security.planck.permissions.PermissionChecker
import java.util.*

class PEpStatusTrustwordsPresenter(
        myselfAddress: String, private val context: Context,
        private val identityView: PEpStatusIdentityView,
        private val permissionChecker: PermissionChecker
) {

    private var myself: Identity = PEpUtils.createIdentity(Address(myselfAddress), context)
    private val pEp: PEpProvider = (context.applicationContext as K9).getpEpProvider()
    private var areTrustwordsShort: Boolean = true
    private var currentLanguage: String = getLanguageForTrustwords()
    private lateinit var localesMap: Map<String, String>

    companion object {
        const val PEP_DEFAULT_LANGUAGE = "en"
    }

    private fun getLocalesMapFromPep() : Map<String, String> {
        return pEp.obtainLanguages().asSequence().associate { Pair(it.value.language, it.value.locale) }
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

    fun loadHandshakeData(partner: Identity) {
        identityView.enableButtons(false)
        retrieveTrustwords(partner, null, null)
    }

    fun changeTrustwordsSize(partner: Identity, areShort: Boolean) {
        if(areShort != areTrustwordsShort) {
            identityView.enableButtons(false)
            retrieveTrustwords(partner, areShort, null)
        }
    }

    fun changeTrustwordsLanguage(partner: Identity, languageKey: String) {
        retrieveTrustwords(partner, null, localesMap[languageKey])
    }

    private fun retrieveTrustwords(partner: Identity, areShort: Boolean?, language: String?) {
        areTrustwordsShort = areShort?:areTrustwordsShort
        currentLanguage = language ?: currentLanguage
        pEp.obtainTrustwords(myself, partner, currentLanguage,
            false,
            object : PEpProvider.ResultCallback<HandshakeData> {
                override fun onLoaded(handshakeData: HandshakeData) {
                    showHandshake(handshakeData)
                }

                override fun onError(throwable: Throwable) {
                    identityView.reportError(throwable.message)
                }
            })
    }

    private fun showHandshake(handshakeData: HandshakeData) {
        if(identityView is PEpStatusPEpIdentityView) {
            val fullTrustwords = handshakeData.fullTrustwords
            val shortTrustwords = handshakeData.shortTrustwords
            if (areTrustwordsShort) {
                identityView.setShortTrustwords(shortTrustwords)

            } else {
                identityView.setLongTrustwords(fullTrustwords)
            }
        }
        myself = handshakeData.myself
        val partner = handshakeData.partner
        val contacts = if (permissionChecker.hasContactsPermission() &&
            K9.showContactName()) Contacts.getInstance(context) else null
        val myselfLabelText = getToFriendly(myself, contacts)
        val partnerLabelText = getToFriendly(partner, contacts)

        identityView.setLabelTexts(
            if(myselfLabelText == myself.address) {
                myself.address
            }
            else {
                String.format(context.getString(R.string.pep_complete_myself_format), myselfLabelText, myself.address)
            },

            if(partnerLabelText == partner.address) {
                partner.address
            }
            else {
                String.format(context.getString(R.string.pep_complete_partner_format), partnerLabelText, partner.address)
            }
        )

        identityView.setFingerPrintTexts(PEpUtils.formatFpr(myself.fpr), PEpUtils.formatFpr(partner.fpr))
        identityView.enableButtons(true)
    }

    private fun getToFriendly(identity: Identity, contacts: Contacts?) : CharSequence {
        val realAddress = Address(identity.address, identity.username)
        return MessageHelper.toFriendly(realAddress, contacts)
    }

    fun rejectHandshake(partner: Identity) {
        identityView.enableButtons(false)
        pEp.keyMistrusted(partner)
    }

    fun confirmHandshake(partner: Identity) {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        scope.launch {
            identityView.enableButtons(false)
            var newpartner = partner
            if (partner.user_id == null || partner.user_id.isEmpty()) {
                val tempFpr = partner.fpr
                withContext(Dispatchers.IO) { newpartner = pEp.updateIdentity(partner) }
                newpartner.fpr = tempFpr
            }
            withContext(Dispatchers.IO) { pEp.trustPersonaKey(newpartner) }
        }
    }
}