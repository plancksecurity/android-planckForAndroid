package com.fsck.k9.pEp;

import android.content.Context;

import com.fsck.k9.pEp.infrastructure.threading.JobExecutor;
import com.fsck.k9.pEp.infrastructure.threading.PostExecutionThread;
import com.fsck.k9.pEp.infrastructure.threading.ThreadExecutor;
import com.fsck.k9.pEp.infrastructure.threading.UIThread;

/**
 * Factory for real providers
 */
public class PEpProviderFactory {
    // FIXME: a little ugly, pep engine needs ctx once but I don't have it everywhere. Hopefully the following will work...
    // (I cache the context, but am not complely sure, wether it becomes invalid once...)

    static public PEpProvider createAndSetupProvider(Context ctx) {
        PEpProvider rv = createProvider(ctx);
        rv.setup(ctx);
        return rv;
    }

    static public PEpProvider createProvider(Context context) {
        ThreadExecutor threadExecutor = new JobExecutor();
        PostExecutionThread postExecutionThread = new UIThread();
        return new PEpProviderImpl(threadExecutor, postExecutionThread, context);
    }
}
