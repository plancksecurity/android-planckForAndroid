package com.fsck.k9.planck.ui.privacy.status

import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.ui.renderers.pepstatus.PlanckStatusPGPIdentityRenderer
import com.fsck.k9.planck.ui.renderers.pepstatus.PlanckStatusSecureRenderer
import com.pedrogomez.renderers.Renderer
import com.pedrogomez.renderers.RendererBuilder
import javax.inject.Inject

class PlanckStatusRendererBuilder @Inject constructor(
    private val pgpRenderer: PlanckStatusPGPIdentityRenderer,
    private val secureRenderer: PlanckStatusSecureRenderer,
) : RendererBuilder<PlanckIdentity>() {

    private lateinit var handshakeResultListener: HandshakeResultListener
    private lateinit var myself: String

    fun setUp(
        handshakeResultListener: HandshakeResultListener,
        myself: String
    ) {
        this.myself = myself
        this.handshakeResultListener = handshakeResultListener

        val prototypes = getPlanckIdentityRendererTypes()
        setPrototypes(prototypes)
    }

    // 24/02/2020: Since values for red color are never returned from engine, we do not need a renderer for red communication channels.
    override fun getPrototypeClass(content: PlanckIdentity): Class<*> {
        val rating = content.rating
        return when {
            !PlanckUtils.isHandshakeRating(rating) -> error("Misusing: wrong rating: $rating")
            PlanckUtils.isPEpUser(content) -> PlanckStatusSecureRenderer::class.java
            else -> PlanckStatusPGPIdentityRenderer::class.java
        }
    }

    private fun getPlanckIdentityRendererTypes(): List<Renderer<PlanckIdentity>> {
        pgpRenderer.setUp(handshakeResultListener, myself)
        secureRenderer.setUp(handshakeResultListener, myself)

        return listOf(
                pgpRenderer,
                secureRenderer,
        )
    }

    interface ResetClickListener {
        fun keyReset(identity: PlanckIdentity)
    }

    interface HandshakeResultListener {
        fun onHandshakeResult(id: PlanckIdentity, trust: Boolean)
    }
}