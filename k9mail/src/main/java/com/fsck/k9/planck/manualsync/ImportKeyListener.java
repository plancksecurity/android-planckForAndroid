package com.fsck.k9.planck.manualsync;

public interface ImportKeyListener {
    void onStart();
    void onNext();
    void onFinish();
    void onCancel();
}
