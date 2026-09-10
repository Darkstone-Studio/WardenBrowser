# Fix Fatal Exception in MainActivity

The application crashes on startup with a `RuntimeException: Unable to start activity`. Based on code analysis, the most likely cause is an `UninitializedPropertyAccessException` for the `lateinit var session` in `MainActivity`.

## Problem Identification

In `MainActivity.onCreate`, `setupGeckoView()` is called. This function calls `createSession(isPrivateMode)` and assigns the result to the `session` property.

```kotlin
    private fun setupGeckoView() {
        session = createSession(isPrivateMode) // 1. Calls createSession
        // ...
    }
```

Inside `createSession`, `applySettings()` is called **before** the session is returned and assigned to the `session` property.

```kotlin
    private fun createSession(isPrivate: Boolean): GeckoSession {
        // ...
        val newSession = GeckoSession(settings)
        // ...
        applySettings() // 2. Calls applySettings
        // ...
        return newSession
    }
```

`applySettings()` then tries to access `session.settings`, which triggers the exception because `session` is not yet initialized.

```kotlin
    private fun applySettings() {
        // ...
        session.settings.allowJavascript = ... // 3. Accesses 'session' which is uninitialized
        // ...
    }
```

## Proposed Changes

### [MainActivity.kt](file:///C:/Users/Onur/Desktop/BlackstoneBrowser%20v1.3.6/MyBrowser/app/src/main/java/com/wardenbrowser/app/MainActivity.kt)

1.  Modify `applySettings()` to accept an optional `GeckoSession` parameter. If provided, it will apply settings to that session; otherwise, it will use the member `session` (for calls from `onResume`).
2.  Update `createSession()` to pass the `newSession` it just created to `applySettings()`.

## Verification Plan

### Automated Tests
- I will perform a build to ensure no syntax errors are introduced.
- Since I cannot run the app in an emulator currently (as per previous tool response), I will rely on code analysis and build verification.

### Manual Verification
- The user should run the application and verify that it no longer crashes on startup.
