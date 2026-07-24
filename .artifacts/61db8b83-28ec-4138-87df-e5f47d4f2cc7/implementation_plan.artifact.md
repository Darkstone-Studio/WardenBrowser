# Fix Multi-Process GeckoRuntime Initialization

This plan addresses a critical issue where `GeckoRuntime.create()` is invoked in every process (main, GPU, tab), leading to application crashes shortly after launch. We will implement a process guard to ensure initialization only occurs in the main process.

## Proposed Changes

### Application Class Logic

#### [MODIFY] [WardenApp.kt](file:///C:/Users/Onur/Desktop/BlackstoneBrowser%20v1.3.6/MyBrowser/app/src/main/java/com/wardenbrowser/app/WardenApp.kt)
- **Process Guard**: Implement an `isMainProcess()` helper function.
    - On API 28+, use `getProcessName()`.
    - On older APIs, iterate through `ActivityManager.getRunningAppProcesses()` to match `Process.myPid()`.
- **Conditional Initialization**: Wrap the `GeckoRuntime` initialization logic inside a check for `isMainProcess()`.
- **Imports**: Add necessary imports: `android.app.ActivityManager`, `android.os.Build`, and `android.os.Process`.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew :app:assembleDebug` to ensure no syntax or compilation errors.

### Manual Verification
- [ ] Launch the app and monitor stability for at least 10 seconds.
- [ ] Check logs (if available) to confirm `GeckoRuntime.create()` is only called once.
- [ ] Verify that GeckoView content still loads correctly in the main process.
