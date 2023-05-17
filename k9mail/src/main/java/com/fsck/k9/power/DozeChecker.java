package com.fsck.k9.power;


import android.content.Context;
import android.os.PowerManager;


public class DozeChecker {
    private final PowerManager powerManager;
    private final String packageName;


    public DozeChecker(Context context) {
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        packageName = context.getPackageName();
    }

    public boolean isAppWhitelisted() {
        return powerManager.isIgnoringBatteryOptimizations(packageName);
    }
}
