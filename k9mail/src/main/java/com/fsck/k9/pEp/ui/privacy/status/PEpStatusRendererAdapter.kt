package com.fsck.k9.pEp.ui.privacy.status

import android.view.ViewGroup
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.renderers.pepstatus.*
import com.pedrogomez.renderers.AdapteeCollection
import com.pedrogomez.renderers.RVRendererAdapter
import com.pedrogomez.renderers.Renderer
import com.pedrogomez.renderers.RendererViewHolder
import com.pedrogomez.renderers.exception.NullRendererBuiltException

class PEpStatusRendererAdapter(
        rendererBuilder: PEpStatusRendererBuilder,
        identityList: AdapteeCollection<PEpIdentity>,
        var resetClickListener: ResetClickListener,
        var handshakeResultListener: HandshakeResultListener,
        var myself: String
): RVRendererAdapter<PEpIdentity>(rendererBuilder, identityList) {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RendererViewHolder {
        val viewHolder = super.onCreateViewHolder(viewGroup, viewType)
        val renderer = viewHolder.renderer
        setRendererParameters(renderer)
        return viewHolder
    }

    private fun setRendererParameters(renderer: Renderer<*>) {
        when(renderer) {
            is PEpStatusHandshakeRenderer -> {
                renderer.initialize(myself, resetClickListener, handshakeResultListener)
            }
            is PEpStatusUnsecureRenderer -> {
                renderer.initialize(resetClickListener)
            }
            is PEpStatusTrustedRenderer -> {
                renderer.initialize(resetClickListener)
            }
            else -> throw(IllegalArgumentException("Wrong Renderer class in adapter: ${renderer.javaClass.simpleName}"))
        }
    }

    interface ResetClickListener {
        fun keyReset(identity: PEpIdentity)
    }

    interface HandshakeResultListener {
        fun onHandshakeResult(id: PEpIdentity, trust: Boolean)
    }
}