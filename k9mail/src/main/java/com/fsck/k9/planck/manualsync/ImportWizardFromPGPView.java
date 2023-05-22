package com.fsck.k9.planck.manualsync;

interface ImportWizardFromPGPView {
    void renderpEpCreateDeviceGroupRequest();

    void renderpEpAddToExistingDeviceGroupRequest();

    void close();

    void cancel();

    void showHandshake(String trustwords);

    void showWaitingForSync();

    void showGroupCreated();

    void showJoinedGroup();

    void showSomethingWentWrong();

    void disableSync();

    void leaveDeviceGroup();

    void showLongTrustwordsIndicator();

    void hideLongTrustwordsIndicator();

    void prepareGroupCreationLoading();

    void prepareGroupJoiningLoading();

    void setFingerPrintTexts(String myselfFprText, String partnerFprText);
}
