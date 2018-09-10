package com.fsck.k9.pEp.manualsync;

import android.content.Context;

import com.fsck.k9.K9;
import com.fsck.k9.pEp.PEpProvider;

public class ImportKeyControllerFactory {
    private static ImportKeyControllerFactory INSTANCE = null;
    private ImportKeyController importKeyController = null;

    private ImportKeyControllerFactory() {

    }

    public static ImportKeyControllerFactory getInstance() {
        if (INSTANCE == null) {
            synchronized (ImportKeyControllerFactory.class) {
                INSTANCE = new ImportKeyControllerFactory();
            }
        }
        return INSTANCE;
    }

    public ImportKeyController getImportKeyController(Context context, PEpProvider pEp) {
        if (importKeyController == null) {
            synchronized (ImportKeyControllerFactory.class) {
                importKeyController = new ImportKeyController(((K9) context.getApplicationContext()), pEp);
            }
        }
        return importKeyController;
    }
}
