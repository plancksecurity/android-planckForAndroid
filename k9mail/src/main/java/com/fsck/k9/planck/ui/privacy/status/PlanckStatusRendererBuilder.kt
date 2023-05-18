package com.fsck.k9.planck.ui.privacy.status

import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.ui.renderers.pepstatus.PlanckStatusPGPIdentityRenderer
import com.fsck.k9.planck.ui.renderers.pepstatus.PlanckStatusSecureRenderer
import com.fsck.k9.planck.ui.renderers.pepstatus.PlanckStatusTrustedRenderer
import com.fsck.k9.planck.ui.renderers.pepstatus.PlanckStatusUnsecureRenderer
import com.pedrogomez.renderers.Renderer
import com.pedrogomez.renderers.RendererBuilder
import javax.inject.Inject

class PlanckStatusRendererBuilder @Inject constructor(
    private val pgpRenderer: PlanckStatusPGPIdentityRenderer,
    private val trustedRenderer: PlanckStatusTrustedRenderer,
    private val secureRenderer: PlanckStatusSecureRenderer,
    private val unsecureRenderer: PlanckStatusUnsecureRenderer
) : RendererBuilder<PlanckIdentity>() {

    lateinit var resetClickListener: ResetClickListener
    private lateinit var handshakeResultListener: HandshakeResultListener
    private lateinit var myself: String

    fun setUp(resetClickListener: ResetClickListener,
              handshakeResultListener: HandshakeResultListener,
              myself: String) {
        this.resetClickListener = resetClickListener
        this.myself = myself
        this.handshakeResultListener = handshakeResultListener

        val prototypes = getPlanckIdentityRendererTypes()
        setPrototypes(prototypes)
    }

    // 24/02/2020: Since values for red color are never returned from engine, we do not need a renderer for red communication channels.
    override fun getPrototypeClass(content: PlanckIdentity): Class<*> {
        val rating = content.rating
        var prototypeClass: Class<*> = PlanckStatusUnsecureRenderer::class.java
        if (PlanckUtils.isHandshakeRating(rating)) {
            prototypeClass = if(!PlanckUtils.isPEpUser(content)) {
                PlanckStatusPGPIdentityRenderer::class.java
            } else {
                PlanckStatusSecureRenderer::class.java
            }
        }
        else if (PlanckUtils.isRatingUnsecure(rating)) {
            prototypeClass = PlanckStatusUnsecureRenderer::class.java
        } /*else if (rating.value == Rating.pEpRatingMistrust.value) {
            prototypeClass = PEpStatusMistrustRenderer::class.java
        }*/ else if (PlanckUtils.isRatingTrusted(rating)) {
            prototypeClass = PlanckStatusTrustedRenderer::class.java
        }
        return prototypeClass
    }

    private fun getPlanckIdentityRendererTypes(): List<Renderer<PlanckIdentity>> {
        pgpRenderer.setUp(resetClickListener, handshakeResultListener, myself)
        secureRenderer.setUp(resetClickListener, handshakeResultListener, myself)
        unsecureRenderer.setUp(resetClickListener)
        trustedRenderer.setUp(resetClickListener)

        return listOf(
                pgpRenderer,
                trustedRenderer,
                secureRenderer,
                unsecureRenderer
        )
    }

    interface ResetClickListener {
        fun keyReset(identity: PlanckIdentity)
    }

    interface HandshakeResultListener {
        fun onHandshakeResult(id: PlanckIdentity, trust: Boolean)
    }
}