# Implementation Plan - One-Time Welcome Screen

Add a one-time welcome/onboarding screen shown only on the very first launch to introduce the user to the app's features and set up the `has_seen_welcome` preference.

## Proposed Changes

### [Component] Resources

#### [NEW] [activity_welcome.xml](file:///C:/Users/Onur/Desktop/WardenBrowser%20v1.4.14/MyBrowser/app/src/main/res/layout/activity_welcome.xml)
- Create a centered layout with the app logo, title, a brief subtitle, and a "Get Started" button.

#### [MODIFY] [strings.xml (Default/Turkish)](file:///C:/Users/Onur/Desktop/WardenBrowser%20v1.4.14/MyBrowser/app/src/main/res/values/strings.xml)
- Add `welcome_subtitle` and `welcome_get_started`.

#### [MODIFY] [strings.xml (English)](file:///C:/Users/Onur/Desktop/WardenBrowser%20v1.4.14/MyBrowser/app/src/main/res/values-en/strings.xml)
- Add English translations for the new strings.

### [Component] Activity Logic

#### [NEW] [WelcomeActivity.kt](file:///C:/Users/Onur/Desktop/WardenBrowser%20v1.4.14/MyBrowser/app/src/main/java/com/wardenbrowser/app/WelcomeActivity.kt)
- Create `WelcomeActivity`.
- Implement a check at the start of `onCreate`: if `has_seen_welcome` is true, immediately start `MainActivity` and finish.
- Set up the "Get Started" button to save the preference and navigate to `MainActivity`.

### [Component] Manifest

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Onur/Desktop/WardenBrowser%20v1.4.14/MyBrowser/app/src/main/AndroidManifest.xml)
- Register `WelcomeActivity` and make it the `LAUNCHER`.
- Remove the launcher intent filter from `MainActivity` and set `android:exported="false"`.

## Verification Plan

### Manual Verification
1.  **First Launch:**
    - Clear app data or uninstall/reinstall.
    - Launch the app.
    - Verify that the Welcome screen appears with the correct logo and text.
    - Click "Get Started".
    - Verify it navigates to the Home screen.
2.  **Subsequent Launches:**
    - Close the app (swipe away from recents).
    - Launch the app again.
    - Verify that it goes straight to the Home screen, skipping the Welcome screen.
3.  **Localization:**
    - Switch system language to English.
    - Repeat the first launch verification and ensure text is in English.
