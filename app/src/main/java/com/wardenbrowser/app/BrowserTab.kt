package com.wardenbrowser.app

import org.mozilla.geckoview.GeckoSession

data class BrowserTab(
    val id: String,
    val session: GeckoSession,
    var title: String = "New Tab",
    var url: String = "",
    var isPrivate: Boolean = false,
    var needsReload: Boolean = false
)
