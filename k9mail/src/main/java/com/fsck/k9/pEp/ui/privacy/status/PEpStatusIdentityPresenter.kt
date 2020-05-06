package com.fsck.k9.pEp.ui.privacy.status

import android.content.Context
import com.fsck.k9.K9
import com.fsck.k9.helper.ContactPicture
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.PePUIArtefactCache
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.HandshakeData
import com.fsck.k9.pEp.ui.PEpContactBadge
import foundation.pEp.jniadapter.Identity
import security.pEp.permissions.PermissionChecker
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class PEpStatusIdentityPresenter @Inject constructor(
        val permissionChecker: PermissionChecker,
        @Named("MainUI") val pep: PEpProvider,
        @Named("AppContext") val context: Context
) {
    private lateinit var identityView: PEpStatusIdentityView
    private lateinit var handshakeView: PEpStatusHandshakeView
    private lateinit var myself: Identity
    private var areTrustwordsShort: Boolean = true
    private var currentLanguage: String = getLanguageForTrustwords()
    private lateinit var localesMap: Map<String, String>

    companion object {
        const val PEP_DEFAULT_LANGUAGE = "en"
    }

    fun initialize(partner: PEpIdentity, badge: PEpContactBadge, identityView: PEpStatusIdentityView) {
        this.identityView = identityView
        displayPartnerRating(partner)
        preparePartnerBadge(partner, badge)
    }

    private fun preparePartnerBadge(identity: PEpIdentity, badge: PEpContactBadge) {
        val realAddress = Address(identity.address, identity.username)
        if (K9.showContactPicture()) {

            val mContactsPictureLoader = ContactPicture.getContactPictureLoader(context)
            Utility.setContactForBadge(badge, realAddress)
            mContactsPictureLoader.loadContactPicture(realAddress, badge)
            badge.setPepRating(identity.rating, true)
        }
        val contacts = getContactsIfPossible()
        showContact(realAddress, contacts)
    }

    private fun showContact(realAddress: Address, contacts: Contacts?) {
        val partnerInfoToShow = MessageHelper.toFriendly(realAddress, contacts)
        identityView.showPartnerIdentity(partnerInfoToShow)
    }

    private fun displayPartnerRating(partner: PEpIdentity) {
        val artefactCache = PePUIArtefactCache.getInstance(context)
        identityView.showRatingStatus(artefactCache.getTitle(partner.rating))
        identityView.showSuggestion(artefactCache.getSuggestion(partner.rating))
    }

    fun initializeHandshakeView(myselfAddress: String, handshakeView: PEpStatusHandshakeView) {
        myself = PEpUtils.createIdentity(Address(myselfAddress), context)
        this.handshakeView = handshakeView
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

    fun loadHandshakeData(partner: Identity) {
        handshakeView.enableButtons(false)
        retrieveTrustwords(partner, null, null)
    }

    fun changeTrustwordsSize(partner: Identity, areShort: Boolean) {
        if(areShort != areTrustwordsShort) {
            handshakeView.enableButtons(false)
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
                    showHandshake(handshakeData)
                }

                override fun onError(throwable: Throwable) {
                    handshakeView.reportError(throwable.message)
                }
            })
    }

    private fun showHandshake(handshakeData: HandshakeData) {
        if(handshakeView is PEpStatusPEpIdentityView) {
            val fullTrustwords = handshakeData.fullTrustwords
            val shortTrustwords = handshakeData.shortTrustwords
            if (areTrustwordsShort) {
                (identityView as PEpStatusPEpIdentityView).setShortTrustwords(shortTrustwords)

            } else {
                (identityView as PEpStatusPEpIdentityView).setLongTrustwords(fullTrustwords)
            }
        }
        else if(handshakeView is PEpStatusPGPIdentityView) {
            myself = handshakeData.myself
            val partner = handshakeData.partner
            val contacts = getContactsIfPossible()
            val myselfLabelText = getToFriendly(myself, contacts)
            val partnerLabelText = getToFriendly(partner, contacts)
            (identityView as PEpStatusPGPIdentityView).setLabelTexts(myself.address, myselfLabelText, partner.address, partnerLabelText)
            (identityView as PEpStatusPGPIdentityView).setFingerPrintTexts(PEpUtils.formatFpr(myself.fpr), PEpUtils.formatFpr(partner.fpr))
        }
        handshakeView.enableButtons(true)
    }

    private fun getContactsIfPossible() = if (permissionChecker.hasContactsPermission() &&
            K9.showContactName()) Contacts.getInstance(context) else null

    private fun getToFriendly(identity: Identity, contacts: Contacts?) : CharSequence {
        val realAddress = Address(identity.address, identity.username)
        return MessageHelper.toFriendly(realAddress, contacts)
    }

    fun rejectHandshake(partner: Identity) {
        handshakeView.enableButtons(false)
        pep.keyMistrusted(partner)
    }

    fun confirmHandshake(partner: Identity) {
        handshakeView.enableButtons(false)
        var newpartner = partner
        if (partner.user_id == null || partner.user_id.isEmpty()) {
            val tempFpr = partner.fpr
            newpartner = pep.updateIdentity(partner)
            newpartner.fpr = tempFpr
        }
        pep.trustPersonaKey(newpartner)
    }
}