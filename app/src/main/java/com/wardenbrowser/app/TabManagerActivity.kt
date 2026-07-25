package com.wardenbrowser.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class TabManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnNewTab: ExtendedFloatingActionButton
    private val tabManager get() = (application as WardenApp).tabManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_manager)
        
        setupStatusBar()
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        recyclerView = findViewById(R.id.tabsRecyclerView)
        btnNewTab = findViewById(R.id.btnNewTab)

        ViewCompat.setOnApplyWindowInsetsListener(btnNewTab) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.bottomMargin = (32 * resources.displayMetrics.density).toInt() + systemBars.bottom
            v.layoutParams = params
            insets
        }

        btnNewTab.setOnClickListener {
            finishWithResult("new")
        }

        loadTabs()
    }

    private fun setupStatusBar() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        windowInsetsController.isAppearanceLightStatusBars = !isNightMode
    }

    private fun loadTabs() {
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)
        recyclerView.adapter = TabAdapter(
            items = tabManager.allTabs(),
            currentTabId = tabManager.currentTab?.id,
            onTabClick = { tab -> 
                finishWithResult("switch", tab.id) 
            }
        ) { tab ->
            tabManager.closeTab(tab.id)
            loadTabs()
        }
    }

    private fun finishWithResult(action: String, tabId: String? = null) {
        val intent = Intent().apply {
            putExtra("action", action)
            tabId?.let { putExtra("tab_id", it) }
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}
