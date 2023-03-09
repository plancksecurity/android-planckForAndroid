package com.fsck.k9.pEp;

import android.content.Context;

import com.fsck.k9.K9;
import com.fsck.k9.pEp.infrastructure.threading.EngineThreadLocalAutoClose;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.UIThread;
import com.fsck.k9.pEp.infrastructure.threading.EngineThreadLocal;

import foundation.pEp.jniadapter.Engine;

/**
 * Factory for real providers
 */
public class PEpProviderFactory {
    // FIXME: a little ugly, pep engine needs ctx once but I don't have it everywhere. Hopefully the following will work...
    // (I cache the context, but am not complely sure, wether it becomes invalid once...)

    static public PEpProvider createProvider(Context context) {
        PostExecutionThread postExecutionThread = new UIThread();
        if (Engine.CLOSE_ON_FINALIZE_MODE) {
            EngineThreadLocal engineThreadLocal = EngineThreadLocal.getInstance((K9) context.getApplicationContext());
            return new PEpProviderImplKotlin(postExecutionThread, context, engineThreadLocal);
        } else {
            EngineThreadLocalAutoClose engineThreadLocal = EngineThreadLocalAutoClose.getInstance((K9) context.getApplicationContext());
            return new PEpProviderImplKotlinAutoClose(postExecutionThread, context, engineThreadLocal);
        }
    }
}
