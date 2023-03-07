package com.fsck.k9.pEp;

import android.content.Context;

import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.UIThread;
import com.fsck.k9.pEp.threads.EngineThreadLocal;

/**
 * Factory for real providers
 */
public class PEpProviderFactory {
    // FIXME: a little ugly, pep engine needs ctx once but I don't have it everywhere. Hopefully the following will work...
    // (I cache the context, but am not complely sure, wether it becomes invalid once...)

    static public PEpProvider createProvider(Context context) {
        PostExecutionThread postExecutionThread = new UIThread();
        EngineThreadLocal engineThreadLocal = new EngineThreadLocal(context);
        return new PEpProviderImplKotlin(postExecutionThread, context, engineThreadLocal);
    }
}
