package com.fsck.k9

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class AppExitDetectService: Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AppExitDetectService", "Service Started")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AppExitDetectService", "Service Destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.e("AppExitDetectService", "END")
        (K9.app as K9).onAppExited()
        stopSelf()
    }
}