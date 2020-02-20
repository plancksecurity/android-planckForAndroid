package com.fsck.k9.pEp.ui.privacy.status

import com.fsck.k9.Account
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.renderers.pepstatus.*
import com.pedrogomez.renderers.Renderer
import com.pedrogomez.renderers.RendererBuilder
import foundation.pEp.jniadapter.Rating
import java.util.*

class PEpStatusRendererBuilder(
        val accounts : List<Account>/*,
        val onResetGreenClick : View.OnClickListener,
        val onResetRedClick : View.OnClickListener ,
        val onHandshakeClick : View.OnClickListener*/
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
                PEpStatusMyselfRenderer(),
                PEpStatusMistrustRenderer(),
                PEpStatusTrustedRenderer(),
                PEpStatusSecureRenderer(),
                PEpStatusUnsecureRenderer()
        )
    }

    interface ResetClickListener {
        fun keyReset(position: Int)
    }
}