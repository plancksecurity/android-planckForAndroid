package com.fsck.k9.pEp;

import android.content.Context;

/**
 * Created by dietz on 14.07.15.
 */
public class PEpProviderFactory {
    static public PEpProvider createProvider(Context ctx) {
        PEpProvider rv = new PEpProviderImpl();
        rv.setup(ctx);
        return rv;
    }
}
