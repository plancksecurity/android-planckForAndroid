package com.fsck.k9.pEp.ui.privacy.status

import com.fsck.k9.Account
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.renderers.pepstatus.*
import com.pedrogomez.renderers.Renderer
import com.pedrogomez.renderers.RendererBuilder
import foundation.pEp.jniadapter.Rating
import java.util.*

class PEpStatusRendererBuilder(
        accounts : List<Account>,
        private val resetClickListener: ResetClickListener,
        private val trustResetClickListener: TrustResetClickListener,
        private val myself: String
) : RendererBuilder<PEpIdentity>() {

    init {
        val prototypes = getVideoRendererPrototypes()
        initializeAddressesOnDevice(accounts)
        setPrototypes(prototypes)
    }

    private var addressesOnDevice: MutableList<String>? = null

    private fun initializeAddressesOnDevice(accounts: List<Account>) {
        addressesOnDevice = ArrayList(accounts.size)
        for (account in accounts) {
            val email = account.email
            addressesOnDevice!!.add(email)
        }
    }

    override fun getPrototypeClass(content: PEpIdentity): Class<*> {
        if(addressesOnDevice!!.contains(content.address)) {
            return PEpStatusMyselfRenderer::class.java
        }
        val rating = content.rating
        var prototypeClass : Class<*> = PEpStatusUnsecureRenderer::class.java
        if (rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value) {
            prototypeClass = PEpStatusUnsecureRenderer::class.java
        } else if (rating.value == Rating.pEpRatingMistrust.value) {
            prototypeClass = PEpStatusMistrustRenderer::class.java
        } else if (rating.value >= Rating.pEpRatingTrusted.value) {
            prototypeClass = PEpStatusTrustedRenderer::class.java
        } else if (rating.value == Rating.pEpRatingReliable.value) {
            prototypeClass = PEpStatusSecureRenderer::class.java
        }
        return prototypeClass
    }

    private fun getVideoRendererPrototypes(): List<Renderer<PEpIdentity>> {
        return listOf(
                PEpStatusMyselfRenderer(resetClickListener),
                PEpStatusMistrustRenderer(resetClickListener),
                PEpStatusTrustedRenderer(resetClickListener, trustResetClickListener),
                PEpStatusSecureRenderer(
                        resetClickListener,
                        myself
                        ),
                PEpStatusUnsecureRenderer(resetClickListener)
        )
    }

    interface ResetClickListener {
        fun keyReset(identity : PEpIdentity)
    }

    interface TrustResetClickListener {
        fun stopTrusting(identity: PEpIdentity)
    }
}