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

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebRequestError

class MainActivity : AppCompatActivity() {

    private lateinit var geckoView: GeckoView
    private lateinit var session: GeckoSession
    private lateinit var homepageContainer: RelativeLayout
    private lateinit var browserContainer: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var appBarLayout: View
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var toolbarSearchBar: EditText
    private lateinit var homeSearchBar: EditText
    private lateinit var btnTabs: android.widget.TextView
    private lateinit var btnPrivateMode: ImageButton
    private lateinit var speedDialRecyclerView: RecyclerView
    private lateinit var glowView: View
    private lateinit var addressBarContainer: View
    private lateinit var homeSearchBarContainer: View
    private lateinit var privateSafeText: android.widget.TextView

    private val tabManager get() = (application as WardenApp).tabManager

    private var pendingDownloadResponse: org.mozilla.geckoview.WebResponse? = null

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingDownloadResponse?.let { startDownload(it) }
        } else {
            Toast.makeText(this, getString(R.string.toast_download_permission_denied), Toast.LENGTH_SHORT).show()
        }
        pendingDownloadResponse = null
    }

    private val tabManagerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val action = result.data?.getStringExtra("action")
            when (action) {
                "switch" -> {
                    val tabId = result.data?.getStringExtra("tab_id")
                    val tab = tabManager.allTabs().find { it.id == tabId }
                    if (tab != null) switchToTab(tab)
                }
                "new" -> {
                    val wantPrivate = result.data?.getBooleanExtra("private", false) ?: false
                    val newSession = createSession(wantPrivate)
                    val newTab = tabManager.addTab(newSession, wantPrivate)
                    switchToTab(newTab)
                    showHomepage()
                }
            }
        }
    }

    private lateinit var dbHelper: HistoryDbHelper
    private var currentTitle: String? = null
    private var isPrivateMode: Boolean = false
    private var canGoBack: Boolean = false
    private var canGoForward: Boolean = false
    private var lastFailedUrl: String? = null
    private var isErrorPageShown: Boolean = false
    private var appliedDesktopMode: Boolean? = null
    // Veritabanı yazmaları arayüz thread'ini bekletmesin
    private val ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "history-io").apply { isDaemon = true }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isPrivateMode", isPrivateMode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        isPrivateMode = savedInstanceState?.getBoolean("isPrivateMode", false) ?: false
        
        dbHelper = HistoryDbHelper.getInstance(this)
        applySavedTheme()
        
        setContentView(R.layout.activity_main)

        initViews()
        setupSystemBars()
        applyPrivateModeUI()
        if (tabManager.currentTab != null) {
            reattachCurrentTab()
        } else {
            setupGeckoView()
        }
        refreshEngineBadge()
        setupListeners()
        setupSpeedDial()
        setupKeyboardAnimation()
        setupBackNavigation()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = intent?.getStringExtra("load_url")
        if (!url.isNullOrEmpty()) {
            performSearch(url)
        }
    }

    override fun onResume() {
        super.onResume()
        // Settings'den gelen değişiklikleri uygula
        applySettings()
        applySpeedDialVisibility()
        reconcileCurrentTab()
    }

    private fun applySpeedDialVisibility() {
        val enabled = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("speed_dial_enabled", true)
        speedDialRecyclerView.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    }

    private fun reconcileCurrentTab() {
        val current = tabManager.currentTab
        when {
            current == null -> {
                // All tabs were closed — create a fresh one
                val newSession = createSession(isPrivateMode)
                val newTab = tabManager.addTab(newSession, isPrivateMode)
                switchToTab(newTab)
                showHomepage()
            }
            current.session != session -> {
                // The tab we were viewing is gone (closed from Tab Manager) — follow TabManager's new current tab
                switchToTab(current)
            }
            else -> {
                // Still the same tab — just refresh the count in case tabs were closed/added elsewhere
                btnTabs.text = tabManager.tabCount.toString()
            }
        }
    }

    private fun initViews() {
        geckoView = findViewById(R.id.geckoview)
        progressBar = findViewById(R.id.progressBar)
        appBarLayout = findViewById(R.id.appBarLayout)
        homepageContainer = findViewById(R.id.homepageContainer)
        browserContainer = findViewById(R.id.browserContainer)
        homeSearchBar = findViewById(R.id.homeSearchBar)
        toolbarSearchBar = findViewById(R.id.toolbarSearchBar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Material 3 seçili öğe vurgusunu (oval arkaplan) kaldır
        bottomNavigation.isItemActiveIndicatorEnabled = false
        
        btnTabs = findViewById(R.id.btnTabs)
        btnPrivateMode = findViewById(R.id.btnPrivateMode)
        speedDialRecyclerView = findViewById(R.id.speedDialRecyclerView)
        glowView = findViewById(R.id.glowView)
        addressBarContainer = findViewById(R.id.addressBarContainer)
        homeSearchBarContainer = findViewById(R.id.homeSearchBarContainer)
        privateSafeText = findViewById(R.id.privateSafeText)
    }

    private fun applySavedTheme() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("theme", "dark")
        applyTheme(theme)
    }

    private fun applySettings(targetSession: GeckoSession? = null) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isDesktopMode = prefs.getBoolean("desktop_mode", false)
        val javascriptEnabled = prefs.getBoolean("javascript_enabled", true)

        // Hedef verilmediyse (ayarlardan dönüş) tüm sekmelere uygula, yalnızca açık olana değil
        val sessions = if (targetSession != null) {
            listOf(targetSession)
        } else {
            (tabManager.allTabs().map { it.session } + session).distinct()
        }

        sessions.forEach { s ->
            // JavaScript Ayarı
            s.settings.allowJavascript = javascriptEnabled

            // Masaüstü Modu: hem kullanıcı ajanı hem görünüm alanı (viewport) masaüstüne geçmeli,
            // aksi halde siteler telefon genişliğine göre mobil düzene döner
            s.settings.userAgentMode = if (isDesktopMode) {
                GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            }
            s.settings.viewportMode = if (isDesktopMode) {
                GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.VIEWPORT_MODE_MOBILE
            }
        }

        // Mod değiştiyse açık sayfayı yeni moda göre yeniden yükle
        if (targetSession == null) {
            val changed = appliedDesktopMode != null && appliedDesktopMode != isDesktopMode
            appliedDesktopMode = isDesktopMode
            if (changed && !isErrorPageShown && homepageContainer.visibility != View.VISIBLE) {
                session.reload(GeckoSession.LOAD_FLAGS_BYPASS_CACHE)
            }
        }
    }

    private fun recreateTabSession(tab: BrowserTab, showToast: Boolean) {
        val wasCurrent = tabManager.currentTab?.id == tab.id
        tabManager.closeTab(tab.id)
        val newSession = createSession(tab.isPrivate)
        val newTab = tabManager.addTab(newSession, tab.isPrivate)

        newTab.url = tab.url
        newTab.title = tab.title
        if (tab.url.isNotBlank() && tab.url != "about:blank") {
            newSession.loadUri(tab.url)
        }

        if (wasCurrent) {
            switchToTab(newTab)
        }
        if (showToast) {
            Toast.makeText(this, getString(R.string.toast_engine_refreshed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupGeckoView() {
        session = createSession(isPrivateMode)

        if (tabManager.currentTab != null) {
            tabManager.closeTab(tabManager.currentTab!!.id)
        }
        tabManager.addTab(session, isPrivateMode)
        btnTabs.text = tabManager.tabCount.toString()

        geckoView.setSession(session)
    }

    private fun reattachCurrentTab() {
        val tab = tabManager.currentTab ?: return
        session = tab.session
        attachDelegates(session)
        
        geckoView.setSession(session)
        isPrivateMode = tab.isPrivate
        applyPrivateModeUI()
        
        toolbarSearchBar.setText(tab.url)
        btnTabs.text = tabManager.tabCount.toString()
        
        if (tab.url.isNotBlank() && tab.url != "about:blank") {
            showBrowserView()
        } else {
            showHomepage()
        }
    }

    private fun createSession(isPrivate: Boolean): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate)
            .build()
        val newSession = GeckoSession(settings)
        val runtime = (application as WardenApp).geckoRuntime
        
        applySettings(newSession)
        newSession.open(runtime)
        attachDelegates(newSession)
        return newSession
    }

    private fun attachDelegates(target: GeckoSession) {
        target.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadError(session: GeckoSession, uri: String?, error: WebRequestError): GeckoResult<String>? {
                android.util.Log.d("GeckoError", "Category: ${error.category}, Code: ${error.code}")
                
                return try {
                    if (error.category == WebRequestError.ERROR_CATEGORY_NETWORK || 
                        error.category == WebRequestError.ERROR_CATEGORY_UNKNOWN ||
                        error.code == 37) {
                        
                        if (uri != null && !uri.startsWith("data:")) {
                            lastFailedUrl = uri
                        }
                        
                        GeckoResult.fromValue(loadOfflineErrorPage())
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.toast_load_error, error.code.toString()), Toast.LENGTH_SHORT).show()
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GeckoError", "Error loading custom page", e)
                    null
                }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                this@MainActivity.canGoBack = canGoBack
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                this@MainActivity.canGoForward = canGoForward
            }

            override fun onLocationChange(session: GeckoSession, url: String?, permissions: MutableList<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) {
                val isErrorPage = url?.startsWith("data:text/html") == true
                if (target == this@MainActivity.session) {
                    isErrorPageShown = isErrorPage
                }
                val shownUrl = if (isErrorPage) lastFailedUrl else url
                shownUrl?.let {
                    if (it != "about:blank") {
                        toolbarSearchBar.setText(it)
                        if (!isPrivateMode && !isErrorPage) {
                            val historyTitle = currentTitle ?: it
                            ioExecutor.execute { dbHelper.addHistory(historyTitle, it) }
                        }
                    }
                }
                shownUrl?.let {
                    tabManager.allTabs().find { t -> t.session == target }?.url = it
                }
            }
        }

        target.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                currentTitle = title
                title?.let {
                    tabManager.allTabs().find { t -> t.session == target }?.title = it
                }
            }

            override fun onCrash(session: GeckoSession) {
                val crashedTab = tabManager.allTabs().find { it.session == target }
                if (crashedTab != null) {
                    val isVisible = tabManager.currentTab?.id == crashedTab.id
                    if (isVisible) {
                        recreateTabSession(crashedTab, true)
                    } else {
                        crashedTab.needsReload = true
                    }
                }
            }

            override fun onKill(session: GeckoSession) {
                val crashedTab = tabManager.allTabs().find { it.session == target }
                if (crashedTab != null) {
                    val isVisible = tabManager.currentTab?.id == crashedTab.id
                    if (isVisible) {
                        recreateTabSession(crashedTab, true)
                    } else {
                        crashedTab.needsReload = true
                    }
                }
            }

            override fun onExternalResponse(session: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
                if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
                    androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    pendingDownloadResponse = response
                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    startDownload(response)
                }
            }
        }

        target.scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                browserContainer.isEnabled = (scrollY <= 0)
                if (session !== this@MainActivity.session) return
                handleBarsOnScroll(scrollY)
            }
        }

        target.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                progressBar.progress = progress
                progressBar.visibility = if (progress < 100) View.VISIBLE else View.GONE
            }
        }
    }

    private var barsHidden = false
    private var lastScrollY = 0
    private var scrollAccum = 0
    private var lastBarsToggle = 0L

    /** Aşağı kaydırınca üst/alt çubuğu gizler, yukarı kaydırınca geri getirir. */
    private fun handleBarsOnScroll(scrollY: Int) {
        val dy = scrollY - lastScrollY
        lastScrollY = scrollY
        if (scrollY <= 0) {
            setBarsHidden(false)
            return
        }
        // Çubuk boyutu değişince sayfa yeniden boyutlanır; o sırada gelen kaymaları yok say
        if (android.os.SystemClock.uptimeMillis() - lastBarsToggle < 400) return
        val threshold = (12 * resources.displayMetrics.density).toInt()
        // Olaylar kare başına küçük adımlarla gelir; aynı yöndeki kaymaları biriktir
        if (dy == 0) return
        if ((dy > 0) != (scrollAccum > 0)) scrollAccum = 0
        scrollAccum += dy
        if (scrollAccum > threshold) {
            scrollAccum = 0
            setBarsHidden(true)
        } else if (scrollAccum < -threshold) {
            scrollAccum = 0
            setBarsHidden(false)
        }
    }

    private fun setBarsHidden(hidden: Boolean) {
        if (barsHidden == hidden) return
        barsHidden = hidden
        lastBarsToggle = android.os.SystemClock.uptimeMillis()
        (appBarLayout as com.google.android.material.appbar.AppBarLayout).setExpanded(!hidden, true)
        // Çubuk yukarı kayarken durum çubuğu bölgesinde görünmesin diye aynı anda solar
        findViewById<View>(R.id.toolbar).animate()
            .alpha(if (hidden) 0f else 1f)
            .setDuration(200)
            .start()
        bottomNavigation.animate()
            .translationY(if (hidden) bottomNavigation.height.toFloat() else 0f)
            .setDuration(200)
            .start()
    }

    private fun switchToTab(tab: BrowserTab) {
        lastScrollY = 0
        setBarsHidden(false)
        if (tab.needsReload) {
            tab.needsReload = false
            recreateTabSession(tab, false)
            return
        }
        
        session = tab.session
        attachDelegates(session)
        isPrivateMode = tab.isPrivate
        applyPrivateModeUI()
        
        geckoView.setSession(session)
        tabManager.switchTo(tab.id)
        toolbarSearchBar.setText(tab.url)
        btnTabs.text = tabManager.tabCount.toString()

        if (tab.url.isNotBlank() && tab.url != "about:blank") {
            showBrowserView()
        } else {
            showHomepage()
        }
    }

    private fun setupListeners() {
        browserContainer.setOnRefreshListener {
            reloadCurrentPage()
            browserContainer.isRefreshing = false
        }

        homeSearchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(homeSearchBar.text.toString())
                true
            } else false
        }

        toolbarSearchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(toolbarSearchBar.text.toString())
                true
            } else false
        }

        toolbarSearchBar.setOnFocusChangeListener { _, hasFocus ->
            animateSearchBarFocus(addressBarContainer, hasFocus)
        }

        homeSearchBar.setOnFocusChangeListener { _, hasFocus ->
            animateSearchBarFocus(homeSearchBarContainer, hasFocus)
            animateHomeDim(hasFocus)
        }

        // Boş bir yere dokunulunca arama halinden çık
        val exitSearch = View.OnClickListener {
            if (homeSearchBar.hasFocus() || toolbarSearchBar.hasFocus()) {
                currentFocus?.let {
                    WindowCompat.getInsetsController(window, it).hide(WindowInsetsCompat.Type.ime())
                }
                homeSearchBar.clearFocus()
                toolbarSearchBar.clearFocus()
            }
        }
        homepageContainer.setOnClickListener(exitSearch)
        findViewById<View>(R.id.centralLayout).setOnClickListener(exitSearch)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_back -> if (canGoBack) session.goBack()
                R.id.nav_forward -> if (canGoForward) session.goForward()
                R.id.nav_home -> showHomepage()
                R.id.nav_refresh -> reloadCurrentPage()
                R.id.nav_menu -> showOverflowMenu()
            }
            false
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            if (browserContainer.visibility == View.VISIBLE) {
                showHomepage()
            }
        }

        btnPrivateMode.setOnClickListener {
            togglePrivateMode()
        }

        findViewById<View>(R.id.btnSearchEngineSelector).setOnClickListener {
            showEngineSelectorMenu(it)
        }

        btnTabs.setOnClickListener {
            tabManagerLauncher.launch(Intent(this, TabManagerActivity::class.java))
        }
    }

    /** Hata sayfasındaysak asıl adresi, değilsek mevcut sayfayı yeniden yükler. */
    private fun reloadCurrentPage() {
        val failed = lastFailedUrl
        if (isErrorPageShown && failed != null) {
            session.loadUri(failed)
        } else {
            session.reload()
        }
    }

    /** Seçili temaya göre hata sayfası teması: light, dark veya (gizli modda) private. */
    private fun errorPageTheme(): String {
        val isNight = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        return when {
            isPrivateMode -> "private"
            isNight -> "dark"
            else -> "light"
        }
    }

    private fun escapeHtmlAttr(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun loadOfflineErrorPage(): String {
        val retryUrl = lastFailedUrl
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?.let { escapeHtmlAttr(it) } ?: "#"
        val html = assets.open("error_offline.html").bufferedReader().use { it.readText() }
            .replace("{{RETRY_URL}}", retryUrl)
            .replace("{{LANG}}", resources.configuration.locales[0].language)
            .replace("{{THEME}}", errorPageTheme())
            .replace("{{TITLE}}", escapeHtmlAttr(getString(R.string.error_offline_title)))
            .replace("{{MESSAGE}}", escapeHtmlAttr(getString(R.string.error_offline_message)))
            .replace("{{RETRY_LABEL}}", escapeHtmlAttr(getString(R.string.error_offline_retry)))
        val base64 = android.util.Base64.encodeToString(html.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        return "data:text/html;charset=utf-8;base64,$base64"
    }

    private fun togglePrivateMode() {
        val targetPrivate = !isPrivateMode

        val newSession = createSession(targetPrivate)
        val newTab = tabManager.addTab(newSession, targetPrivate)
        switchToTab(newTab)
        showHomepage()

        if (targetPrivate) {
            Toast.makeText(this, getString(R.string.toast_private_mode_on), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.toast_private_mode_off), Toast.LENGTH_SHORT).show()
        }
    }

    private val bgAnimators = HashMap<View, ValueAnimator>()

    private fun animateBackgroundColor(view: View, toColor: Int) {
        val fromColor = if (view.background is ColorDrawable) {
            (view.background as ColorDrawable).color
        } else {
            ContextCompat.getColor(this, R.color.app_background)
        }
        // Süren animasyon varsa iptal et; renk zaten aynıysa animasyon başlatma (gereksiz çizim)
        bgAnimators.remove(view)?.cancel()
        if (fromColor == toColor) {
            view.setBackgroundColor(toColor)
            return
        }
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnimation.duration = 600
        colorAnimation.interpolator = AccelerateDecelerateInterpolator()
        colorAnimation.addUpdateListener { animator ->
            view.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimation.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (bgAnimators[view] === colorAnimation) bgAnimators.remove(view)
            }
        })
        bgAnimators[view] = colorAnimation
        colorAnimation.start()
    }

    private fun animateSearchBarFocus(container: View, hasFocus: Boolean) {
        val targetScale = if (hasFocus) 1.03f else 1f
        val targetElevation = if (hasFocus) 8f else 0f
        
        container.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(200)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .start()
        
        container.animate()
            .translationZ(targetElevation)
            .setDuration(200)
            .start()
    }

    private var homeDimAnimator: android.animation.ValueAnimator? = null

    /** Arama çubuğu odaktayken ana ekrandaki diğer öğeleri karartır (çubuk hariç). */
    private fun animateHomeDim(dim: Boolean) {
        val central = findViewById<android.view.ViewGroup>(R.id.centralLayout)
        val targets = mutableListOf<View>(findViewById(R.id.glowView), privateSafeText)
        for (i in 0 until central.childCount) {
            val child = central.getChildAt(i)
            if (child !== homeSearchBarContainer) targets.add(child)
        }
        val paint = android.graphics.Paint()
        homeDimAnimator?.cancel()
        val from = if (dim) 1f else 0.2f
        val to = if (dim) 0.2f else 1f
        homeDimAnimator = android.animation.ValueAnimator.ofFloat(from, to).apply {
            duration = 200
            addUpdateListener {
                val v = (it.animatedValue as Float)
                val c = (v * 255).toInt().coerceIn(0, 255)
                paint.colorFilter = android.graphics.LightingColorFilter(
                    android.graphics.Color.rgb(c, c, c), 0
                )
                targets.forEach { t -> t.setLayerType(View.LAYER_TYPE_HARDWARE, paint) }
                // Arka plan da kararır (açık temada yalnızca yazıların kararması kötü görünüyordu)
                findViewById<View>(R.id.homeDimScrim).alpha = 1f - v
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!dim) {
                        targets.forEach { t -> t.setLayerType(View.LAYER_TYPE_NONE, null) }
                        findViewById<View>(R.id.homeDimScrim).alpha = 0f
                    }
                }
            })
            start()
        }
    }

    private fun applyPrivateModeUI() {
        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val accentColor = if (isPrivateMode) {
            androidx.core.content.ContextCompat.getColor(this, R.color.private_mode_accent)
        } else {
            androidx.core.content.ContextCompat.getColor(this, R.color.accent_blue)
        }
        
        val bgColor = if (isPrivateMode) {
            androidx.core.content.ContextCompat.getColor(this, R.color.private_mode_bg)
        } else {
            androidx.core.content.ContextCompat.getColor(this, R.color.app_background)
        }

        val surfaceColor = if (isPrivateMode) {
            androidx.core.content.ContextCompat.getColor(this, R.color.private_mode_surface)
        } else {
            androidx.core.content.ContextCompat.getColor(this, R.color.surface_color)
        }

        val iconGrayColor = androidx.core.content.ContextCompat.getColor(this, R.color.icon_gray)

        // Status Bar ve Nav Bar İkon Rengi Yönetimi
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isPrivateMode) {
            windowInsetsController.isAppearanceLightStatusBars = false
            windowInsetsController.isAppearanceLightNavigationBars = false
        } else {
            windowInsetsController.isAppearanceLightStatusBars = !isNightMode
            windowInsetsController.isAppearanceLightNavigationBars = !isNightMode
        }

        // Renkleri Belirle
        val finalIconColor = if (isPrivateMode) {
            androidx.core.content.ContextCompat.getColor(this, R.color.white)
        } else {
            iconGrayColor
        }
        
        val finalTextColor = if (isPrivateMode) {
            androidx.core.content.ContextCompat.getColor(this, R.color.white)
        } else {
            androidx.core.content.ContextCompat.getColor(this, R.color.text_primary)
        }

        // Arka Planları Uygula
        animateBackgroundColor(appBarLayout, bgColor)
        animateBackgroundColor(homepageContainer, bgColor)
        
        // Arama çubuğu arka planlarını optimize et
        addressBarContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor)
        homeSearchBarContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor)
        
        // Glow efekti
        glowView.animate().alpha(0f).setDuration(200).withEndAction {
            glowView.setBackgroundResource(if (isPrivateMode) R.drawable.bg_glow_purple else R.drawable.bg_glow_yellow)
            glowView.animate().alpha(1f).setDuration(200).start()
        }.start()

        // Bottom Navigation renklerini ayarla
        val navTextColorStateList = android.content.res.ColorStateList.valueOf(accentColor)
        bottomNavigation.itemIconTintList = navTextColorStateList
        bottomNavigation.itemTextColor = navTextColorStateList
        animateBackgroundColor(bottomNavigation, bgColor)

        // İkon ve Metin Renklerini Uygula
        val tabsColor = if (isPrivateMode) accentColor else finalIconColor
        val tabsColorStateList = android.content.res.ColorStateList.valueOf(tabsColor)
        
        btnTabs.backgroundTintList = tabsColorStateList
        btnTabs.setTextColor(tabsColor)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationIconTint(finalIconColor)
        
        toolbarSearchBar.setTextColor(finalTextColor)
        toolbarSearchBar.setHintTextColor(if (isPrivateMode) 0x80FFFFFF.toInt() else androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary))
        homeSearchBar.setTextColor(finalTextColor)
        homeSearchBar.setHintTextColor(if (isPrivateMode) 0x80FFFFFF.toInt() else androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary))
        
        // Private Mode butonu görseli
        btnPrivateMode.imageTintList = android.content.res.ColorStateList.valueOf(
            if (isPrivateMode) accentColor else finalIconColor
        )
        
        // Logo/Marka renkleri
        findViewById<android.widget.TextView>(R.id.brandPart1).setTextColor(accentColor)
        findViewById<android.widget.TextView>(R.id.brandPart2).setTextColor(accentColor)
        findViewById<android.widget.TextView>(R.id.betaBadge).setTextColor(accentColor)
        
        // Private Mode Safe Text
        privateSafeText.setTextColor(accentColor)
        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_info)?.mutate()?.let { icon ->
            val size = (18 * resources.displayMetrics.density).toInt()
            icon.setBounds(0, 0, size, size)
            icon.setTint(accentColor)
            privateSafeText.compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
            privateSafeText.setCompoundDrawablesRelative(null, null, icon, null)
        }
        privateSafeText.setOnClickListener { showPrivateInfoDialog() }

        if (isPrivateMode) {
            privateSafeText.visibility = View.VISIBLE
            privateSafeText.alpha = 0f
            privateSafeText.translationY = 20f
            privateSafeText.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay(150)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            privateSafeText.animate()
                .alpha(0f)
                .translationY(20f)
                .setDuration(250)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    privateSafeText.visibility = View.GONE
                }
                .start()
        }
        
        // Progress bar rengi
        progressBar.progressTintList = android.content.res.ColorStateList.valueOf(accentColor)

        // Speed Dial Adapter'ı güncelle
        if (speedDialRecyclerView.adapter is SpeedDialAdapter) {
            (speedDialRecyclerView.adapter as SpeedDialAdapter).updatePrivateMode(isPrivateMode)
        }
    }

    private fun performSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val engine = prefs.getString("search_engine", "google")
            val searchUrl = when (engine) {
                "duckduckgo" -> "https://duckduckgo.com/?q="
                "brave" -> "https://search.brave.com/search?q="
                "bing" -> "https://www.bing.com/search?q="
                else -> "https://www.google.com/search?q="
            }

            val url = if (trimmedQuery.contains(".") && !trimmedQuery.contains(" ")) {
                if (trimmedQuery.startsWith("http")) trimmedQuery else "https://$trimmedQuery"
            } else {
                "$searchUrl${android.net.Uri.encode(trimmedQuery)}&pws=0&gl=tr&gws_rd=cr"
            }
            
            showBrowserView()
            
            session.loadUri(url)
            currentFocus?.let { 
                WindowCompat.getInsetsController(window, it).hide(WindowInsetsCompat.Type.ime())
            }
            homeSearchBar.clearFocus()
            toolbarSearchBar.clearFocus()
        }
    }

    private fun showBrowserView() {
        homepageContainer.visibility = View.GONE
        browserContainer.visibility = View.VISIBLE
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationIcon(R.drawable.ic_back)
    }

    private fun showHomepage() {
        browserContainer.visibility = View.GONE
        homepageContainer.visibility = View.VISIBLE
        toolbarSearchBar.text.clear()
        homeSearchBar.text.clear()
        lastScrollY = 0
        setBarsHidden(false)

        // Geri ikonunu gizle
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).navigationIcon = null
        
        session.loadUri("about:blank")

        // Klavye kapalıysa kaydırmayı sıfırla (aramadan dönüşte takılı kalan ofseti temizler)
        ViewCompat.getRootWindowInsets(window.decorView)?.let { applyKeyboardOffset(it) }
    }

    private fun showPrivateInfoDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.private_info_title))
            .setMessage(getString(R.string.private_info_saved) + "\n\n" + getString(R.string.private_info_visible))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showOverflowMenu() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheet = layoutInflater.inflate(R.layout.dialog_menu_sheet, null)
        dialog.setContentView(sheet)

        // Panel arka planı (yuvarlak üst köşeler)
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(R.drawable.bg_menu_sheet)
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        // Kısayol kartı ikonları uygulamanın vurgu rengini izler (gizli modda mor)
        val accent = androidx.core.content.ContextCompat.getColor(
            this, if (isPrivateMode) R.color.private_mode_accent else R.color.accent_blue
        )
        listOf(R.id.sheetBookmarksIcon, R.id.sheetHistoryIcon, R.id.sheetDownloadsIcon).forEach { id ->
            sheet.findViewById<android.widget.ImageView>(id)
                .imageTintList = android.content.res.ColorStateList.valueOf(accent)
        }

        fun onClick(id: Int, action: () -> Unit) {
            sheet.findViewById<View>(id).setOnClickListener {
                dialog.dismiss()
                action()
            }
        }

        onClick(R.id.sheetBookmarks) { startActivity(Intent(this, BookmarksActivity::class.java)) }
        onClick(R.id.sheetHistory) { startActivity(Intent(this, HistoryActivity::class.java)) }
        onClick(R.id.sheetDownloads) { startActivity(Intent(this, DownloadsActivity::class.java)) }
        onClick(R.id.sheetTheme) { showThemeSelector() }
        onClick(R.id.sheetSettings) { startActivity(Intent(this, SettingsActivity::class.java)) }
        onClick(R.id.sheetAbout) { startActivity(Intent(this, AboutActivity::class.java)) }
        onClick(R.id.sheetClearExit) {
            dbHelper.clearHistory()
            Toast.makeText(this, getString(R.string.toast_history_cleared_exiting), Toast.LENGTH_SHORT).show()
            finishAffinity()
        }

        dialog.show()
    }

    private fun showThemeSelector() {
        val themes = resources.getStringArray(R.array.theme_entries)
        val themeValues = resources.getStringArray(R.array.theme_values)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.theme_dialog_title))
            .setItems(themes) { _, which ->
                val selectedTheme = themeValues[which]
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString("theme", selectedTheme)
                    .apply()
                applyTheme(selectedTheme)
            }
            .show()
    }

    private fun applyTheme(theme: String?) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun defaultSpeedDialItems() = listOf(
        SpeedDialItem("Google", "https://www.google.com"),
        SpeedDialItem("YouTube", "https://www.youtube.com"),
        SpeedDialItem("GitHub", "https://www.github.com"),
        SpeedDialItem("Wikipedia", "https://www.wikipedia.org"),
        SpeedDialItem("Reddit", "https://www.reddit.com")
    )

    private fun loadSpeedDialItems(): MutableList<SpeedDialItem> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val json = prefs.getString("speed_dial_items", null) ?: return defaultSpeedDialItems().toMutableList()
        return try {
            val array = org.json.JSONArray(json)
            val list = mutableListOf<SpeedDialItem>()
            for (i in 0 until minOf(array.length(), SpeedDialAdapter.MAX_ITEMS)) {
                val obj = array.getJSONObject(i)
                list.add(SpeedDialItem(obj.getString("name"), obj.getString("url")))
            }
            list
        } catch (e: org.json.JSONException) {
            defaultSpeedDialItems().toMutableList()
        }
    }

    private fun saveSpeedDialItems(items: List<SpeedDialItem>) {
        val array = org.json.JSONArray()
        items.forEach { array.put(org.json.JSONObject().put("name", it.name).put("url", it.url)) }
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit().putString("speed_dial_items", array.toString()).apply()
    }

    private fun setupSpeedDial() {
        val items = loadSpeedDialItems()
        lateinit var adapter: SpeedDialAdapter
        adapter = SpeedDialAdapter(items, isPrivateMode, { item ->
            performSearch(item.url)
        }, {
            showAddSpeedDialDialog { newItem ->
                adapter.addItem(newItem)
                saveSpeedDialItems(items)
            }
        }, { item, anchor ->
            val popup = android.widget.PopupMenu(this, anchor)
            popup.menu.add(R.string.speed_dial_delete).setOnMenuItemClickListener {
                adapter.removeItem(item)
                saveSpeedDialItems(items)
                true
            }
            popup.show()
        })

        speedDialRecyclerView.layoutManager = GridLayoutManager(this, 4)
        speedDialRecyclerView.adapter = adapter

        // Satır sayısı 1-2 arasında değişince ana ekran kaymasın: yükseklik hep 2 satırlık olsun
        val sample = layoutInflater.inflate(R.layout.item_speed_dial, speedDialRecyclerView, false)
        sample.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val lp = sample.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val rowHeight = sample.measuredHeight + lp.topMargin + lp.bottomMargin
        speedDialRecyclerView.minimumHeight =
            rowHeight * 2 + speedDialRecyclerView.paddingTop + speedDialRecyclerView.paddingBottom
    }

    private fun showAddSpeedDialDialog(onAdd: (SpeedDialItem) -> Unit) {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val nameInput = EditText(this).apply {
            hint = getString(R.string.speed_dial_name_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine()
        }
        val urlInput = EditText(this).apply {
            hint = getString(R.string.speed_dial_url_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
        }
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(nameInput)
            addView(urlInput)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.speed_dial_add_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                var url = urlInput.text.toString().trim()
                if (url.isEmpty()) return@setPositiveButton
                if (!url.contains("://")) url = "https://$url"
                val host = android.net.Uri.parse(url).host
                if (host.isNullOrEmpty()) return@setPositiveButton
                val name = nameInput.text.toString().trim()
                    .ifEmpty { host.removePrefix("www.") }
                onAdd(SpeedDialItem(name, url))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupKeyboardAnimation() {
        ViewCompat.setWindowInsetsAnimationCallback(
            window.decorView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    applyKeyboardOffset(insets)
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    // Animasyon bittiğinde kesin son değeri uygula (görünmezken takılı kalmasın)
                    ViewCompat.getRootWindowInsets(window.decorView)?.let { applyKeyboardOffset(it) }
                }
            }
        )
    }

    /**
     * Ana ekran içeriğini klavye yüksekliğinin yarısı kadar yukarı kaydırır.
     * Ana ekran gizliyken de uygulanır; böylece klavye aramadan sonra kapandığında
     * kaydırma sıfırlanır ve ana ekrana dönünce içerik yerinde durur.
     */
    private fun applyKeyboardOffset(insets: WindowInsetsCompat) {
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val diff = (imeInsets.bottom - systemBarsInsets.bottom).coerceAtLeast(0)
        // Sadece ortadaki çubuk kayar; üst çubuk odaktayken kaymaz.
        // Çubuk zaten kaymışsa klavye inerken ona eşlik eder.
        val moves = homeSearchBar.hasFocus() || homeSearchBarContainer.translationY != 0f
        homeSearchBarContainer.translationY = if (moves) -diff.toFloat() / 2f else 0f
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (browserContainer.visibility == View.VISIBLE) {
                    if (canGoBack) {
                        session.goBack()
                    } else {
                        showHomepage()
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun engineFaviconUrl(engineKey: String): String {
        if (engineKey == "bing") {
            return "https://www.bing.com/favicon.ico"
        }
        val domain = when (engineKey) {
            "google" -> "google.com"
            "duckduckgo" -> "duckduckgo.com"
            "brave" -> "brave.com"
            else -> "google.com"
        }
        return "https://www.google.com/s2/favicons?domain=$domain&sz=128"
    }

    private fun engineFallbackLetter(engineKey: String): Pair<String, Int> = when (engineKey) {
        "google" -> "G" to android.graphics.Color.parseColor("#4285F4")
        "duckduckgo" -> "D" to android.graphics.Color.parseColor("#DE5833")
        "bing" -> "B" to android.graphics.Color.parseColor("#008373")
        "brave" -> "Br" to android.graphics.Color.parseColor("#FB542B")
        else -> "G" to android.graphics.Color.parseColor("#4285F4")
    }

    private fun refreshEngineBadge() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val engine = prefs.getString("search_engine", "google") ?: "google"
        val badge = findViewById<android.widget.ImageView>(R.id.searchEngineBadge)

        com.bumptech.glide.Glide.with(this)
            .load(engineFaviconUrl(engine))
            .override(64, 64)
            .circleCrop()
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    applyLetterBadge(engine, badge)
                    return true
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean = false
            })
            .into(badge)
    }

    private fun applyLetterBadge(engine: String, badge: android.widget.ImageView) {
        val (letter, color) = engineFallbackLetter(engine)
        val size = 96
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            textSize = size * 0.45f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        val yPos = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(letter, size / 2f, yPos, textPaint)
        badge.setImageDrawable(android.graphics.drawable.BitmapDrawable(resources, bmp))
    }

    private fun showEngineSelectorMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        val entries = resources.getStringArray(R.array.search_engine_entries)
        popup.menu.add(0, 1, 0, entries[0])
        popup.menu.add(0, 2, 1, entries[1])
        popup.menu.add(0, 3, 2, entries[2])
        popup.menu.add(0, 4, 3, entries[3])
        popup.setOnMenuItemClickListener { item ->
            val newEngine = when (item.itemId) {
                1 -> "google"; 2 -> "duckduckgo"; 3 -> "bing"; 4 -> "brave"; else -> "google"
            }
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("search_engine", newEngine).apply()
            refreshEngineBadge()
            true
        }
        popup.show()
    }

    /**
     * Sunucudan gelen dosya adını güvenli hale getirir: dizin ayraçlarını ve "../" gibi
     * yol parçalarını atar, kontrol/yasak karakterleri temizler, baştaki noktaları siler.
     * Geçerli bir ad kalmazsa null döner.
     */
    private fun sanitizeFileName(name: String): String? {
        val cleaned = name
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .filter { it.code >= 32 && it !in "<>:\"|?*" }
            .trim()
            .trimStart('.')
            .take(120)
            .trim()
        return cleaned.ifBlank { null }
    }

    private fun startDownload(response: org.mozilla.geckoview.WebResponse) {
        val url = response.uri
        val contentDisposition = response.headers["Content-Disposition"]
        val mimeType = response.headers["Content-Type"] ?: "application/octet-stream"

        val rawFileName = contentDisposition
            ?.substringAfter("filename=", "")
            ?.substringBefore(';')
            ?.trim('"', ' ')
            ?.ifBlank { null }
            ?: android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
        val fileName = sanitizeFileName(rawFileName)
            ?: android.webkit.URLUtil.guessFileName(url, null, mimeType).let { sanitizeFileName(it) ?: "download" }

        // GeckoView dosyayı zaten açtı: gövdeyi doğrudan kaydet. Aynı adresi DownloadManager ile
        // yeniden indirmek veriyi iki kez harcıyor ve çerezsiz olduğu için girişli sitelerde başarısız oluyordu.
        val body = response.body
        if (body == null) {
            Toast.makeText(this, getString(R.string.toast_download_failed), Toast.LENGTH_SHORT).show()
            return
        }
        val privateDownload = isPrivateMode
        Toast.makeText(this, getString(R.string.toast_download_started, fileName), Toast.LENGTH_SHORT).show()

        Thread({
            // Çakışmada dosya yeniden adlandırılır; kayıt ve açma işlemi gerçek adı kullanmalı
            val savedName = try {
                saveToDownloads(body, fileName, mimeType)
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                if (savedName != null) {
                    if (!privateDownload) {
                        val savedPath = "${android.os.Environment.DIRECTORY_DOWNLOADS}/$savedName"
                        DownloadsDbHelper.getInstance(this).addDownload(savedName, url, savedPath, mimeType)
                    }
                    Toast.makeText(this, getString(R.string.toast_download_completed, savedName), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.toast_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }, "download-io").start()
    }

    /**
     * Akışı herkese açık İndirilenler klasörüne yazar (Android 10+ MediaStore, öncesinde doğrudan dosya).
     * Aynı adlı dosya varsa ezmez, "ad (1).uzantı" gibi yeni ad verir; kaydedilen gerçek adı döndürür.
     */
    private fun saveToDownloads(input: java.io.InputStream, fileName: String, mimeType: String): String {
        return input.use {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType.substringBefore(';').trim())
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw java.io.IOException("MediaStore insert failed")
                try {
                    val out = resolver.openOutputStream(uri) ?: throw java.io.IOException("No output stream")
                    out.use { input.copyTo(it) }
                    values.clear()
                    values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
                resolver.query(uri, arrayOf(android.provider.MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                } ?: fileName
            } else {
                val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val base = fileName.substringBeforeLast('.', fileName)
                val ext = fileName.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
                var target = java.io.File(dir, fileName)
                var n = 1
                while (target.exists()) {
                    target = java.io.File(dir, "$base ($n)$ext")
                    n++
                }
                target.outputStream().use { input.copyTo(it) }
                target.name
            }
        }
    }
}