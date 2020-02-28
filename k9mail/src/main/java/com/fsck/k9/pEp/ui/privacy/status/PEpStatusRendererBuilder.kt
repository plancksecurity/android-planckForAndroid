package com.fsck.k9.pEp.ui.privacy.status

import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusPGPIdentityRenderer
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusSecureRenderer
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusTrustedRenderer
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusUnsecureRenderer
import com.pedrogomez.renderers.Renderer
import com.pedrogomez.renderers.RendererBuilder
import foundation.pEp.jniadapter.CommType
import foundation.pEp.jniadapter.Rating
import timber.log.Timber

class PEpStatusRendererBuilder(
        private val resetClickListener: ResetClickListener,
        private val handshakeResultListener: HandshakeResultListener,
        private val myself: String
) : RendererBuilder<PEpIdentity>() {

    init {
        val prototypes = getPepIdentityRendererTypes()
        setPrototypes(prototypes)
    }

    // 24/02/2020: Since values for red color are never returned from engine, we do not need a renderer for red communication channels.
    override fun getPrototypeClass(content: PEpIdentity): Class<*> {
        Timber.e("==== commtype = ${content.comm_type}" )
        if(content.comm_type.equals(CommType.PEP_ct_unknown)) return PEpStatusPGPIdentityRenderer::class.java

        val rating = content.rating
        var prototypeClass: Class<*> = PEpStatusUnsecureRenderer::class.java
        if (rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value) {
            prototypeClass = PEpStatusUnsecureRenderer::class.java
        } /*else if (rating.value == Rating.pEpRatingMistrust.value) {
            prototypeClass = PEpStatusMistrustRenderer::class.java
        }*/ else if (rating.value >= Rating.pEpRatingTrusted.value) {
            prototypeClass = PEpStatusTrustedRenderer::class.java
        } else if (rating.value == Rating.pEpRatingReliable.value) {
            prototypeClass = PEpStatusSecureRenderer::class.java
        }
        return prototypeClass
    }

    private fun getPepIdentityRendererTypes(): List<Renderer<PEpIdentity>> {
        return listOf(
                PEpStatusPGPIdentityRenderer(
                        resetClickListener,
                        handshakeResultListener,
                        myself
                ),
                PEpStatusTrustedRenderer(resetClickListener),
                PEpStatusSecureRenderer(
                        resetClickListener,
                        handshakeResultListener,
                        myself
                ),
                PEpStatusUnsecureRenderer(resetClickListener)
        )
    }

    interface ResetClickListener {
        fun keyReset(identity: PEpIdentity)
    }

    interface HandshakeResultListener {
        fun onHandshakeResult(id: PEpIdentity, trust: Boolean)
    }
}