# Fix MainActivity Startup Crash

The application is crashing on startup with `java.lang.RuntimeException: Unable to start activity ComponentInfo{com.wardenbrowser.app/com.wardenbrowser.app.MainActivity}`.

## Analysis

The primary cause of the crash appears to be an `UninitializedPropertyAccessException` in `MainActivity`.

1.  In `MainActivity.onCreate`, `setupGeckoView()` is called.
2.  `setupGeckoView()` calls `createSession(isPrivateMode)`.
3.  `createSession()` calls `applySettings()` BEFORE the newly created session is assigned to the `lateinit var session` property of `MainActivity`.
4.  `applySettings()` attempts to access `session.settings.allowJavascript`, which throws an `UninitializedPropertyAccessException` because `session` hasn't been initialized yet.

Additionally, I noticed a potential race condition or crash in `WardenApp` if `geckoRuntime` is accessed before it's initialized, or if the process name detection is unreliable on some devices.

## Proposed Changes

### [MainActivity](file:///C:/Users/Onur/Desktop/BlackstoneBrowser v1.3.6/MyBrowser/app/src/main/java/com/wardenbrowser/app/MainActivity.kt)

- [MODIFY] Update `applySettings()` to accept an optional `GeckoSession` parameter. If provided, it uses that session; otherwise, it falls back to the `lateinit var session` property (with a safety check).
- [MODIFY] Update `createSession()` to pass the newly created `GeckoSession` to `applySettings(newSession)`.
- [MODIFY] Update `onResume()` to call `applySettings()` without arguments (it will use the initialized `session`).

### [WardenApp](file:///C:/Users/Onur/Desktop/BlackstoneBrowser v1.3.6/MyBrowser/app/src/main/java/com/wardenbrowser/app/WardenApp.kt)

- [MODIFY] Improve `isMainProcess()` to be more robust.
- [MODIFY] Ensure `geckoRuntime` is initialized or provide a safe way to access it. (Actually, just ensuring it's initialized in the main process is usually enough, but we can add a check).

## Verification Plan

### Manual Verification
- Deploy the app to a device/emulator.
- Verify that the app starts without crashing.
- Verify that private mode toggling still works.
- Verify that settings (like desktop mode) are applied correctly.

### Automated Tests
- N/A (UI-based crash fix)
