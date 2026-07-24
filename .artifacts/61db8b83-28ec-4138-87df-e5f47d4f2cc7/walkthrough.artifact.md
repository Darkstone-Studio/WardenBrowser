# Walkthrough - Multi-Process Fix & v1.3.12 Update

I have fixed the issue where `GeckoRuntime` was being initialized in every process and updated the application version to v1.3.12.

## Changes Made

### Version Update
#### [build.gradle.kts](file:///C:/Users/Onur/Desktop/BlackstoneBrowser%20v1.3.6/MyBrowser/app/build.gradle.kts)
- Updated `versionCode` to `13`.
- Updated `versionName` to `"1.3.12"`.

### Application Class
#### [WardenApp.kt](file:///C:/Users/Onur/Desktop/BlackstoneBrowser%20v1.3.6/MyBrowser/app/src/main/java/com/wardenbrowser/app/WardenApp.kt)
- **Added Process Guard**: I added a private helper function `isMainProcess()` to determine if the current process is the application's primary process.
    - Uses `Application.getProcessName()` for API 28 and above.
    - Falls back to `ActivityManager.getRunningAppProcesses()` for older Android versions.
- **Conditional Initialization**: The `GeckoRuntime.create()` call is now wrapped in a check that only allows it to run in the main process. This prevents GPU and content processes from attempting to initialize their own runtimes, resolving the stability issues.

## Verification Result

### Build Status
- The project was successfully built using `./gradlew :app:assembleDebug`.

### Stability
- By gating the runtime initialization, the app should no longer crash due to multi-process resource conflicts during startup.
