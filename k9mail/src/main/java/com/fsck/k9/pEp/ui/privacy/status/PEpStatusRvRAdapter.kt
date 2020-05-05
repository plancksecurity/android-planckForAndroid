package com.fsck.k9.pEp.ui.privacy.status

import android.view.ViewGroup
import com.fsck.k9.pEp.models.PEpIdentity
import com.fsck.k9.pEp.ui.renderers.pepstatus.*
import com.pedrogomez.renderers.AdapteeCollection
import com.pedrogomez.renderers.RVRendererAdapter
import com.pedrogomez.renderers.Renderer
import com.pedrogomez.renderers.RendererViewHolder
import com.pedrogomez.renderers.exception.NullRendererBuiltException

class PEpStatusRvRAdapter(
        rendererBuilder: PEpStatusRendererBuilder,
        identityList: AdapteeCollection<PEpIdentity>,
        var resetClickListener: PEpStatusRendererBuilder.ResetClickListener,
        var handshakeResultListener: PEpStatusRendererBuilder.HandshakeResultListener,
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
            is PEpStatusPGPIdentityRenderer -> {
                renderer.myself = myself
                renderer.resetClickListener = resetClickListener
                renderer.handshakeResultListener = handshakeResultListener
            }
            is PEpStatusSecureRenderer-> {
                renderer.myself = myself
                renderer.resetClickListener = resetClickListener
                renderer.handshakeResultListener = handshakeResultListener
            }
            is PEpStatusUnsecureRenderer -> {
                renderer.resetClickListener = resetClickListener
            }
            is PEpStatusTrustedRenderer -> {
                renderer.resetClickListener = resetClickListener
            }
            else -> throw(IllegalArgumentException("Wrong Renderer class in adapter: ${renderer.javaClass.simpleName}"))
        }
    }
}