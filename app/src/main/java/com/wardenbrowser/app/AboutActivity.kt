package com.wardenbrowser.app

import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.content.Intent
import android.graphics.Paint
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        
        setupStatusBar()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        
        findViewById<TextView>(R.id.versionText).text = "v${BuildConfig.VERSION_NAME}"
        
        val detailsText = StringBuilder()
            .append(getString(R.string.about_powered_by)).append("\n\n")
            .append(getString(R.string.about_developer)).append("\n")
            .append("GitHub: https://github.com/Darkstone-Studio/WardenBrowser\n\n")
            .append(getString(R.string.about_android_version, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)).append("\n")
            .append(getString(R.string.about_build_number, BuildConfig.VERSION_CODE))
            .toString()
            
        findViewById<TextView>(R.id.detailsText).text = detailsText

        val privacyPolicyText = findViewById<TextView>(R.id.privacyPolicyText)
        privacyPolicyText.paintFlags = privacyPolicyText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        privacyPolicyText.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://darkstone-studio.github.io/WardenBrowser/privacy-policy.html"))
            startActivity(intent)
        }
    }

    private fun setupStatusBar() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        windowInsetsController.isAppearanceLightStatusBars = !isNightMode
    }
}