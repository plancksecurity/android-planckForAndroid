package com.fsck.k9.planck;

import android.content.Context;

import com.fsck.k9.K9;
import com.fsck.k9.planck.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.planck.infrastructure.threading.UIThread;
import com.fsck.k9.planck.infrastructure.threading.EngineThreadLocal;

/**
 * Factory for real providers
 */
public class PlanckProviderFactory {
    // FIXME: a little ugly, pep engine needs ctx once but I don't have it everywhere. Hopefully the following will work...
    // (I cache the context, but am not complely sure, wether it becomes invalid once...)

    static public PlanckProvider createProvider(Context context) {
        PostExecutionThread postExecutionThread = new UIThread();
        EngineThreadLocal engineThreadLocal = EngineThreadLocal.getInstance((K9) context.getApplicationContext());
        return new PlanckProviderImplKotlin(postExecutionThread, context, engineThreadLocal);
    }
}
