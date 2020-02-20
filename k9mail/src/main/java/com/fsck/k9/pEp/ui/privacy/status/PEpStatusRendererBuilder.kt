package com.fsck.k9.pEp.ui.privacy.status

import android.view.View
import com.fsck.k9.Account
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusMistrustRenderer
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusSecureRenderer
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusTrustedRenderer
import com.fsck.k9.pEp.ui.renderers.pepstatus.PEpStatusUnsecureRenderer
import com.pedrogomez.renderers.Renderer
import com.pedrogomez.renderers.RendererBuilder
import foundation.pEp.jniadapter.Rating
import java.util.*

class PEpStatusRendererBuilder(
        /*val accounts : List<Account>,
        val onResetGreenClick : View.OnClickListener,
        val onResetRedClick : View.OnClickListener ,
        val onHandshakeClick : View.OnClickListener*/
) : RendererBuilder<PEpIdentity>() {

    init {
        val prototypes = getVideoRendererPrototypes()
        setPrototypes(prototypes)
    }

    /**
     * Method to declare Video-VideoRenderer mapping.
     * Favorite videos will be rendered using FavoriteVideoRenderer.
     * Live videos will be rendered using LiveVideoRenderer.
     * Liked videos will be rendered using LikeVideoRenderer.
     *
     * @param content used to map object-renderers.
     * @return VideoRenderer subtype class.
     */
    override fun getPrototypeClass(content: PEpIdentity): Class<*> {
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

    /**
     * Create a list of prototypes to configure RendererBuilder.
     * The list of Renderer<Video> that contains all the possible renderers that our RendererBuilder
     * is going to use.
     *
     * @return Renderer<Video> prototypes for RendererBuilder.
    </Video></Video> */
    private fun getVideoRendererPrototypes(): List<Renderer<PEpIdentity>> {
        return listOf(
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