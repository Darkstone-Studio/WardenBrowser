package com.wardenbrowser.app

import org.mozilla.geckoview.GeckoSession
import java.util.UUID

class TabManager {
    private val tabs = mutableListOf<BrowserTab>()
    private var currentIndex: Int = -1

    val tabCount: Int
        get() = tabs.size

    val currentTab: BrowserTab?
        get() = if (currentIndex in tabs.indices) tabs[currentIndex] else null

    fun addTab(session: GeckoSession, isPrivate: Boolean = false): BrowserTab {
        val tab = BrowserTab(
            id = UUID.randomUUID().toString(),
            session = session,
            isPrivate = isPrivate
        )
        tabs.add(tab)
        currentIndex = tabs.size - 1
        return tab
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index != -1) {
            tabs[index].session.close()
            tabs.removeAt(index)
            
            if (tabs.isEmpty()) {
                currentIndex = -1
            } else {
                if (index == currentIndex) {
                    // Current tab was removed, adjust index to same position or last tab
                    currentIndex = index.coerceAtMost(tabs.size - 1)
                } else if (index < currentIndex) {
                    // A tab before current was removed, shift index down
                    currentIndex--
                }
            }
        }
    }

    fun switchTo(id: String): BrowserTab? {
        val index = tabs.indexOfFirst { it.id == id }
        return if (index != -1) {
            currentIndex = index
            tabs[currentIndex]
        } else {
            null
        }
    }

    fun allTabs(): List<BrowserTab> = tabs.toList()
}
