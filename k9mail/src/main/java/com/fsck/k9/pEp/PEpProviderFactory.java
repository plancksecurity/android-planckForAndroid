package com.fsck.k9.pEp;

/**
 * Created by dietz on 14.07.15.
 */
public class PEpProviderFactory {
    static public PEpProvider createProvider() {
        return new DummyPepProviderImpl();
    }
}
