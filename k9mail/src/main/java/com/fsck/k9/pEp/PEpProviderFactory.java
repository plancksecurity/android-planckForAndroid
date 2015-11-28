package com.fsck.k9.pEp;

import android.content.Context;

/**
 * Factory for real providers
 */
public class PEpProviderFactory {
    // FIXME: a little ugly, pep engine needs ctx once but I don't have it everywhere. Hopefully the following will work...
    // (I cache the context, but am not complely sure, wether it becomes invalid once...)

    static public PEpProvider createAndSetupProvider(Context ctx) {
        PEpProvider rv = createProvider();
        rv.setup(ctx);
        return rv;
    }

    static public PEpProvider createProvider() {
        return new PEpProviderImpl();
    }
}
