/*
 * Warden Browser
 * Copyright (c) 2026 [Onur Karatas]
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use, copy, modify, and distribute this software for
 * NONCOMMERCIAL purposes only. Commercial use, including sale,
 * resale, or paid distribution, is NOT permitted.
 *
 * Full license text: https://github.com/Darkstone-Studio/WardenBrowser/blob/main/LICENSE
 * Or: https://polyformproject.org/licenses/noncommercial/1.0.0
 */

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
            val httpsOnlyEnabled = prefs.getBoolean("https_only_enabled", true)

            val builder = GeckoRuntimeSettings.Builder()
                .contentBlocking(
                    ContentBlocking.Settings.Builder()
                        .antiTracking(if (trackerBlockingEnabled) ContentBlocking.AntiTracking.DEFAULT else ContentBlocking.AntiTracking.NONE)
                        .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                        .safeBrowsing(if (safeBrowsingEnabled) ContentBlocking.SafeBrowsing.DEFAULT else ContentBlocking.SafeBrowsing.NONE)
                        .build()
                )
                .allowInsecureConnections(
                    if (httpsOnlyEnabled) GeckoRuntimeSettings.HTTPS_ONLY else GeckoRuntimeSettings.ALLOW_ALL
                )

            // Düşük RAM'li telefonlarda site izolasyonunu (fission) kapat: her site ayrı işlem açtığı için
            // bellek yetmeyip sistem içerik işlemini öldürüyor ve sayfa baştan yükleniyordu.
            // Yeterli belleği olan telefonlarda motorun varsayılanı (tam koruma) aynen kalır.
            if (isLowMemoryDevice()) {
                builder.fissionEnabled(false)
            }

            geckoRuntime = GeckoRuntime.create(this, builder.build())
        }
    }

    /** Toplam RAM 4,5 GB altındaysa (pazarlamada "4 GB ve altı" telefonlar) true. */
    private fun isLowMemoryDevice(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem < LOW_MEMORY_THRESHOLD_BYTES
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

    private companion object {
        const val LOW_MEMORY_THRESHOLD_BYTES = 4_831_838_208L // 4,5 GiB
    }
}
