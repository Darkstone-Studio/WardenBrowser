package com.wardenbrowser.app

import android.app.Application
import android.app.ActivityManager
import android.os.Build
import android.os.Process
import androidx.preference.PreferenceManager
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class WardenApp : Application() {
    lateinit var geckoRuntime: GeckoRuntime
    val tabManager = TabManager()
    
    override fun onCreate() {
        super.onCreate()
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val savedLanguage = prefs.getString("app_language", "system")
        val localeList = when (savedLanguage) {
            "tr" -> androidx.core.os.LocaleListCompat.forLanguageTags("tr")
            "en" -> androidx.core.os.LocaleListCompat.forLanguageTags("en")
            else -> androidx.core.os.LocaleListCompat.getEmptyLocaleList()
        }
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)

        if (isMainProcess()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val trackerBlockingEnabled = prefs.getBoolean("tracker_blocking_enabled", true)
            val safeBrowsingEnabled = prefs.getBoolean("safe_browsing_enabled", true)

            val settings = GeckoRuntimeSettings.Builder()
                .contentBlocking(
                    ContentBlocking.Settings.Builder()
                        .antiTracking(if (trackerBlockingEnabled) ContentBlocking.AntiTracking.DEFAULT else ContentBlocking.AntiTracking.NONE)
                        .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                        .safeBrowsing(if (safeBrowsingEnabled) ContentBlocking.SafeBrowsing.DEFAULT else ContentBlocking.SafeBrowsing.NONE)
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
