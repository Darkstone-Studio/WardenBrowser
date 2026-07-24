package com.wardenbrowser.app

import android.app.Application
import android.app.ActivityManager
import android.os.Build
import android.os.Process
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class WardenApp : Application() {
    lateinit var geckoRuntime: GeckoRuntime
    
    override fun onCreate() {
        super.onCreate()
        
        if (isMainProcess()) {
            val settings = GeckoRuntimeSettings.Builder()
                .contentBlocking(
                    ContentBlocking.Settings.Builder()
                        .antiTracking(ContentBlocking.AntiTracking.DEFAULT)
                        .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                        .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                        .build()
                )
                .build()
            geckoRuntime = GeckoRuntime.create(this, settings)
        }
    }

    private fun isMainProcess(): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= 28) {
            getProcessName()
        } else {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val processes = am.runningAppProcesses
            processes?.find { it.pid == Process.myPid() }?.processName
        }
        return processName == null || processName == packageName
    }
}
