package com.fsck.k9.planck.infrastructure.threading

import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.controller.MessagingController
import foundation.pEp.jniadapter.Engine
import foundation.pEp.jniadapter.exceptions.pEpException
import security.planck.ui.PassphraseProvider
import timber.log.Timber

class EngineThreadLocal private constructor(
    private val k9: K9
) : ThreadLocal<Engine>(), AutoCloseable {

    override fun get(): Engine {
        return createEngineInstanceIfNeeded().also {
            configureOnEveryCall(it)
        }
    }

    private fun configureOnEveryCall(engine: Engine) {

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

        engine.config_passive_mode(K9.getPlanckPassiveMode())
        engine.config_unencrypted_subject(!K9.isPlanckSubjectProtection())
        engine.setMessageToSendCallback(MessagingController.getInstance(k9))
        engine.setNotifyHandshakeCallback(k9.notifyHandshakeCallback)
        engine.setPassphraseRequiredCallback(PassphraseProvider.getPassphraseRequiredCallback(k9))

        if (k9.isRunningOnWorkProfile) {
            engine.config_media_keys(K9.getMediaKeys()?.map { it.toPair() }?.let { ArrayList(it) })
        }
    }

    override fun close() {
        super.get()?.close()
        remove()
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