package com.fsck.k9.pEp.manualsync;

interface ImportWizardFromPGPView {
    void showDescription(String description);

    void setImportTitle(String type);

    void renderpEpInitialScreen();

    void renderPGPInitialScreen();

    void renderWaitingForHandshake();

    void renderWaitingForPGPHandshake();

    void renderpEpSecondlScreen();

    void notifyAcceptedHandshakeAndWaitingForPrivateKey();

    void notifyKeySent();

    void finishImportSuccefully();

    void close();

    void cancel();
}
