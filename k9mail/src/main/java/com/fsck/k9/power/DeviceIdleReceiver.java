package com.fsck.k9.power;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.fsck.k9.service.MailServiceLegacy;

import timber.log.Timber;


class DeviceIdleReceiver extends BroadcastReceiver {
    private final PowerManager powerManager;


    DeviceIdleReceiver(PowerManager powerManager) {
        this.powerManager = powerManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean deviceInIdleMode = powerManager.isDeviceIdleMode();
        Timber.v("Device idle mode changed. Idle: %b", deviceInIdleMode);

        if (!deviceInIdleMode) {
            MailServiceLegacy.actionReset(context, null);
        }
    }
}
