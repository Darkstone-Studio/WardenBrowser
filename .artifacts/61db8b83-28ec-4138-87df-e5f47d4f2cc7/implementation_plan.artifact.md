# Implementation Plan - Refactor Session Management

This plan outlines the refactoring of session creation and delegate management in `MainActivity.kt`. This will make the session logic reusable for future multi-tab features while maintaining current app behavior.

## Proposed Changes

### [MainActivity Refactor]

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Onur/Desktop/BlackstoneBrowser%20v1.3.6/MyBrowser/app/src/main/java/com/wardenbrowser/app/MainActivity.kt)

- **New Helper: `createSession(isPrivate: Boolean)`**:
    - Extracted logic to build a `GeckoSessionSettings`, create a `GeckoSession`, open it with the runtime, and attach delegates.
- **New Helper: `attachDelegates(target: GeckoSession)`**:
    - Consolidated all delegate assignments (`navigationDelegate`, `contentDelegate`, `scrollDelegate`, `progressDelegate`) into a single reusable function.
- **New Function: `switchToTab(tab: BrowserTab)`**:
    - Logic to switch the active `session` variable, update `geckoView`, update the `tabManager` state, and refresh UI elements like the address bar and tab count.
- **Refactored: `setupGeckoView()`**:
    - Updated to use `createSession()` for building the session, while keeping the existing `tabManager` integration (closing old tab if needed and adding the new one).

## Verification Plan

### Automated Tests
- Build the project using `./gradlew :app:assembleDebug` to ensure all references are correct after refactoring.

### Manual Verification
- [ ] **Initial Launch**: Verify the browser still loads the homepage/start page correctly.
- [ ] **Navigation**: Verify address bar searches and back/forward navigation work as before.
- [ ] **Privacy Mode**: Toggle Private Mode and verify the session correctly resets with private settings.
- [ ] **Theme Change**: Change the theme and verify the session recovers without issues.
