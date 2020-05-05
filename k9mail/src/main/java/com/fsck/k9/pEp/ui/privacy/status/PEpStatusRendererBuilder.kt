package com.fsck.k9.pEp.ui.privacy.status

import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusPGPIdentityRenderer
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusSecureRenderer
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusTrustedRenderer
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusUnsecureRenderer
import com.pedrogomez.renderers.Renderer
import com.pedrogomez.renderers.RendererBuilder
import foundation.pEp.jniadapter.Rating
import javax.inject.Inject

class PEpStatusRendererBuilder @Inject constructor(
        var pEpStatusSecureRenderer: PEpStatusSecureRenderer,
        var pEpStatusPGPIdentityRenderer: PEpStatusPGPIdentityRenderer,
        var pEpStatusUnsecureRenderer: PEpStatusUnsecureRenderer,
        var pEpStatusTrustedRenderer: PEpStatusTrustedRenderer
): RendererBuilder<PEpIdentity>() {

    init {
        val prototypes = getPepIdentityRendererTypes()
        setPrototypes(prototypes)
    }

    // 24/02/2020: Since values for red color are never returned from engine, we do not need a renderer for red communication channels.
    override fun getPrototypeClass(content: PEpIdentity): Class<*> {
        val rating = content.rating
        var prototypeClass: Class<*> = PEpStatusUnsecureRenderer::class.java
        if (rating.value == Rating.pEpRatingReliable.value) {
            prototypeClass = if(!PEpUtils.isPEpUser(content)) {
                PEpStatusPGPIdentityRenderer::class.java
            } else {
                PEpStatusSecureRenderer::class.java
            }
        }
        else if (rating.value != Rating.pEpRatingMistrust.value && rating.value < Rating.pEpRatingReliable.value) {
            prototypeClass = PEpStatusUnsecureRenderer::class.java
        } /*else if (rating.value == Rating.pEpRatingMistrust.value) {
            prototypeClass = PEpStatusMistrustRenderer::class.java
        }*/ else if (rating.value >= Rating.pEpRatingTrusted.value) {
            prototypeClass = PEpStatusTrustedRenderer::class.java
        }
        return prototypeClass
    }

    private fun getPepIdentityRendererTypes(): List<Renderer<PEpIdentity>> {
        return listOf(
                pEpStatusPGPIdentityRenderer,
                pEpStatusTrustedRenderer,
                pEpStatusSecureRenderer,
                pEpStatusUnsecureRenderer
        )
    }
}