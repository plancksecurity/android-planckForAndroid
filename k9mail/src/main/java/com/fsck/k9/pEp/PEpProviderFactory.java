package com.fsck.k9.pEp;

import android.content.Context;

/**
 * Created by dietz on 14.07.15.
 */
public class PEpProviderFactory {
    // FIXME: a little ugly, pep engine needs ctx once but I don't have it everywhere. Hopefully the follwowing will work...

    static public PEpProvider createAndSetupProvider(Context ctx) {
        PEpProvider rv = createProvider();
        rv.setup(ctx);
        return rv;
    }

    static public PEpProvider createProvider() {
        return new PEpProviderImpl();
    }
}
