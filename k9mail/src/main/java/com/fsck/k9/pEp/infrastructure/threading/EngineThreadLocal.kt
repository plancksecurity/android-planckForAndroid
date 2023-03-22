package com.fsck.k9.pEp.infrastructure.threading

import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.controller.MessagingController
import foundation.pEp.jniadapter.Engine
import foundation.pEp.jniadapter.exceptions.pEpException
import security.pEp.ui.PassphraseProvider
import timber.log.Timber

class EngineThreadLocal private constructor(
    private val k9: K9
) : ThreadLocal<Engine>(), AutoCloseable {

    override fun get(): Engine {
        return createEngineInstanceIfNeeded()
    }

    private fun createEngineInstanceIfNeeded(): Engine {
        return super.get() ?: let {
            ensureThread()
            return try {
                val engine = Engine()
                initEngineConfig(engine)
                this.set(engine)
                engine
            } catch (e: pEpException) {
                Timber.e(e, "%s %s", TAG, "createIfNeeded " + Thread.currentThread().id)
                throw e
            }
        }
    }

    private fun ensureThread() {
        if (BuildConfig.DEBUG) {
            val thread = Thread.currentThread()
            if (thread !is AutoCloseableEngineThread) {
                error("CURRENT THREAD IS NOOOOOT AUTOCLOSEABLE ENGINE THREAD, ON THREAD: ${thread.name}")
            }
        }
    }

    private fun initEngineConfig(engine: Engine) {

        engine.config_passive_mode(K9.getPEpPassiveMode())
        engine.config_unencrypted_subject(!K9.ispEpSubjectProtection())
        engine.config_passphrase_for_new_keys(
            K9.ispEpUsingPassphraseForNewKey(),
            K9.getpEpNewKeysPassphrase()
        )
        engine.setMessageToSendCallback(MessagingController.getInstance(k9))
        engine.setNotifyHandshakeCallback(k9.notifyHandshakeCallback)
        engine.setPassphraseRequiredCallback(PassphraseProvider.getPassphraseRequiredCallback(k9))
        engine.config_enable_echo_protocol(K9.isEchoProtocolEnabled())
        if (k9.isRunningOnWorkProfile) {
            engine.config_media_keys(K9.getMediaKeys()?.map { it.toPair() }?.let { ArrayList(it) })
        }
    }

    override fun close() {
        super.get()?.close()
        set(null)
    }

    fun isEmpty(): Boolean = super.get() == null

    companion object {
        private const val TAG = "pEpEngine-ThreadLocal-provider"

        private lateinit var instance: EngineThreadLocal

        @JvmStatic
        fun getInstance(k9: K9): EngineThreadLocal {
            synchronized(this) {
                if (!::instance.isInitialized) {
                    instance = EngineThreadLocal(k9)
                }
            }
            return instance
        }
    }

}