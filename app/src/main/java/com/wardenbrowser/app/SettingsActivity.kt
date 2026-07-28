package com.wardenbrowser.app

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.appbar.MaterialToolbar
import org.mozilla.geckoview.StorageController

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setupStatusBar()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    private fun setupStatusBar() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        windowInsetsController.isAppearanceLightStatusBars = !isNightMode
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        
        // Sürüm Bilgisi
        findPreference<Preference>("version")?.summary = "v${BuildConfig.VERSION_NAME}"

        // Geçmişi Temizle
        findPreference<Preference>("clear_history")?.setOnPreferenceClickListener {
            HistoryDbHelper.getInstance(requireContext()).clearHistory()
            Toast.makeText(context, getString(R.string.toast_history_cleared), Toast.LENGTH_SHORT).show()
            true
        }

        // Çerezleri Temizle
        findPreference<Preference>("clear_cookies")?.setOnPreferenceClickListener {
            val runtime = (requireActivity().application as WardenApp).geckoRuntime
            runtime.storageController.clearData(StorageController.ClearFlags.COOKIES)
            Toast.makeText(context, getString(R.string.toast_cookies_cleared), Toast.LENGTH_SHORT).show()
            true
        }

        // Önbelleği Temizle
        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener {
            val runtime = (requireActivity().application as WardenApp).geckoRuntime
            runtime.storageController.clearData(StorageController.ClearFlags.ALL_CACHES)
            Toast.makeText(context, getString(R.string.toast_cache_cleared), Toast.LENGTH_SHORT).show()
            true
        }

        // Lisanslar (AboutLibraries LibsActivity'ye yönlendir)
        findPreference<Preference>("licenses")?.setOnPreferenceClickListener {
            startActivity(Intent(context, com.mikepenz.aboutlibraries.ui.LibsActivity::class.java))
            true
        }

        // GitHub Bağlantısı
        findPreference<Preference>("github")?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mazyLeyn/WardenBrowser"))
            startActivity(intent)
            true
        }

        // Destek Ol
        findPreference<Preference>("support")?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/mazyLeyn"))
            startActivity(intent)
            true
        }

        // Takip Engelleme
        findPreference<SwitchPreferenceCompat>("tracker_blocking_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            val runtime = (requireActivity().application as WardenApp).geckoRuntime
            runtime.settings.contentBlocking.setAntiTracking(
                if (enabled) org.mozilla.geckoview.ContentBlocking.AntiTracking.DEFAULT
                else org.mozilla.geckoview.ContentBlocking.AntiTracking.NONE
            )
            true
        }

        // Güvenli Gezinme
        findPreference<SwitchPreferenceCompat>("safe_browsing_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            val runtime = (requireActivity().application as WardenApp).geckoRuntime
            runtime.settings.contentBlocking.setSafeBrowsing(
                if (enabled) org.mozilla.geckoview.ContentBlocking.SafeBrowsing.DEFAULT
                else org.mozilla.geckoview.ContentBlocking.SafeBrowsing.NONE
            )
            true
        }

        // Dil Değiştirme
        findPreference<androidx.preference.ListPreference>("app_language")?.setOnPreferenceChangeListener { _, newValue ->
            val localeTag = when (newValue as String) {
                "tr" -> "tr"
                "en" -> "en"
                else -> null // system default
            }
            val localeList = if (localeTag != null) {
                androidx.core.os.LocaleListCompat.forLanguageTags(localeTag)
            } else {
                androidx.core.os.LocaleListCompat.getEmptyLocaleList()
            }
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "theme" -> {
                val theme = sharedPreferences?.getString("theme", "dark")
                applyTheme(theme)
            }
        }
    }

    private fun applyTheme(theme: String?) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}