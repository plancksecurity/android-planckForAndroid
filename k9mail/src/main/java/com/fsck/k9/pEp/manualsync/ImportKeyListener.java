package com.fsck.k9.pEp.manualsync;

public interface ImportKeyListener {
    void onStart();
    void onNext();
    void onFinish();
    void onCancel();
}
