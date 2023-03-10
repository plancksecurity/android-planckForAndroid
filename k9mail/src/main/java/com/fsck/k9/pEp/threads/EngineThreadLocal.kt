package com.fsck.k9.pEp.threads

import android.content.Context
import com.fsck.k9.K9
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.pEp.PEpProviderImplKotlin
import foundation.pEp.jniadapter.Engine
import foundation.pEp.jniadapter.exceptions.pEpException
import security.pEp.ui.PassphraseProvider
import timber.log.Timber
import java.util.ArrayList

class EngineThreadLocal(val context: Context) : ThreadLocal<Engine>() {

    override fun get(): Engine {
        if(super.get()==null){
            createEngineInstanceIfNeeded()
        }
        //TODO review this, in THEORY we never, EVER have it empty now.
        return super.get() ?: throw IllegalStateException("ENGINE IS NOT INITIALIZED HERE!!!")
    }

    private fun createEngineInstanceIfNeeded() {
        try {
            val engine = Engine()
            initEngineConfig(engine)
            this.set(engine)
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", TAG, "createIfNeeded " + Thread.currentThread().id)
        }
    }
    private fun initEngineConfig(engine: Engine) {

        engine.config_passive_mode(K9.getPEpPassiveMode())
        engine.config_unencrypted_subject(!K9.ispEpSubjectProtection())
        engine.config_passphrase_for_new_keys(K9.ispEpUsingPassphraseForNewKey(), K9.getpEpNewKeysPassphrase())
        engine.setMessageToSendCallback(MessagingController.getInstance(context))
        engine.setNotifyHandshakeCallback((context.applicationContext as K9).notifyHandshakeCallback)
        engine.setPassphraseRequiredCallback(PassphraseProvider.getPassphraseRequiredCallback(context))
        engine.config_enable_echo_protocol(K9.isEchoProtocolEnabled())
        if ((context.applicationContext as K9).isRunningOnWorkProfile) { // avoid in demo PEMA-74 / https://gitea.pep.foundation/pEp.foundation/pEpEngine/issues/85
            engine.config_media_keys(K9.getMediaKeys()?.map { it.toPair() }?.let { ArrayList(it) })
        }
    }

    companion object {
        private const val TAG = "pEpEngine-ThreadLocal-provider"
    }

}