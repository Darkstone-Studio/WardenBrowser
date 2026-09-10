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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.View

class TabManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnNewTab: ExtendedFloatingActionButton
    private lateinit var btnCloseAllTabs: FloatingActionButton
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
        btnCloseAllTabs = findViewById(R.id.btnCloseAllTabs)

        val tabActionsContainer = findViewById<View>(R.id.tabActionsContainer)
        ViewCompat.setOnApplyWindowInsetsListener(tabActionsContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.bottomMargin = (32 * resources.displayMetrics.density).toInt() + systemBars.bottom
            v.layoutParams = params
            insets
        }

        btnNewTab.setOnClickListener {
            finishWithResult("new")
        }

        btnCloseAllTabs.setOnClickListener {
            showCloseAllConfirmation()
        }

        loadTabs()
    }

    private fun showCloseAllConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_close_all_title)
            .setMessage(R.string.dialog_close_all_message)
            .setNegativeButton(R.string.dialog_close_all_cancel, null)
            .setPositiveButton(R.string.dialog_close_all_confirm) { _, _ ->
                closeAllTabs()
            }
            .show()
    }

    private fun closeAllTabs() {
        val allTabs = tabManager.allTabs()
        allTabs.forEach { tab ->
            tabManager.closeTab(tab.id)
        }
        // Notify result "new" to open a fresh tab in MainActivity
        finishWithResult("new")
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
