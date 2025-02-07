package com.android.imagic.files.util

import android.net.wifi.WifiManager
import android.os.PowerManager
import com.android.imagic.files.app.powerManager
import com.android.imagic.files.app.wifiManager

class WakeWifiLock(tag: String) {
    private val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
        .apply { setReferenceCounted(false) }
    private val wifiLock =
        wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, tag)
            .apply { setReferenceCounted(false) }

    var isAcquired: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            if (value) {
                wakeLock.acquire()
                wifiLock.acquire()
            } else {
                wifiLock.release()
                wakeLock.release()
            }
            field = value
        }
}
