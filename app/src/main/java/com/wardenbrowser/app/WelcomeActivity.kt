package com.wardenbrowser.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alreadySeen = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("has_seen_welcome", false)
        if (alreadySeen) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)
        animateHeading()

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGetStarted).setOnClickListener {
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean("has_seen_welcome", true)
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun animateHeading() {
        val container = findViewById<android.widget.LinearLayout>(R.id.welcomeHeadingContainer)
        val heading = getString(R.string.welcome_heading)
        val density = resources.displayMetrics.density

        heading.forEach { char ->
            val charView = android.widget.TextView(this).apply {
                text = char.toString()
                textSize = 42f
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                alpha = 0f
                translationY = 40f * density
            }
            container.addView(charView)
        }

        container.post {
            for (i in 0 until container.childCount) {
                val charView = container.getChildAt(i)
                charView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay((i * 45).toLong())
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        }
    }
}