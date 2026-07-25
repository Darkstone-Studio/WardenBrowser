# Lazy Reload for Background Tab Crashes

Optimize crash recovery by only recreating the currently visible tab immediately. Background tabs that crash or are killed will be marked for a lazy reload, which occurs only when the user switches to them.

## Proposed Changes

### [app](file:///C:/Users/Onur/Desktop/BlackstoneBrowser%20v1.3.6/MyBrowser/app)

#### [MODIFY] [BrowserTab.kt](file:///C:/Users/Onur/Desktop/BlackstoneBrowser%20v1.3.6/MyBrowser/app/src/main/java/com/wardenbrowser/app/BrowserTab.kt)
- Add `var needsReload: Boolean = false` to track tabs that require a lazy session reconstruction.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Onur/Desktop/BlackstoneBrowser%20v1.3.6/MyBrowser/app/src/main/java/com/wardenbrowser/app/MainActivity.kt)
- **Delegate Update**: In `onCrash` and `onKill`, check if the crashed tab is the `isVisible` (current) tab.
    - If visible: Recreate immediately with `recreateTabSession()`.
    - If in background: Set `needsReload = true`.
- **Switch Logic Update**: In `switchToTab()`, check if the tab `needsReload`.
    - If `true`, reset the flag and call `recreateTabSession(tab, false)` to lazily rebuild it.

## Verification Plan

### Automated Tests
- Run `analyze_file` on `BrowserTab.kt` and `MainActivity.kt`.
- Verify no compilation errors are introduced.

### Manual Verification
1.  Open multiple tabs and load websites in each.
2.  Simulate a process kill/crash while the app is in the background (which typically affects all background sessions).
3.  Re-open the app. The current tab should recover immediately.
4.  Open the Tab Manager and switch to a background tab.
5.  Verify the background tab reloads its URL lazily upon selection.
6.  Verify that the Tab Manager still shows the correct titles/URLs even for crashed background tabs (thanks to the previous persistence fix).
