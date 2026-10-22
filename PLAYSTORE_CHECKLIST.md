# ⚠️ Google Play Store & F-Droid Release Checklist

> **CRITICAL REMINDER**: Before publishing Attendly to the **Google Play Store**, **F-Droid**, or any official app marketplace, complete this checklist to prevent **instant policy rejection, app suspension, or developer account termination**.

---

## 🚨 1. Remove In-App APK Download & Package Installer (MANDATORY)

Google Play strictly prohibits apps from downloading executable package files (`.apk`) or requesting the `REQUEST_INSTALL_PACKAGES` permission unless the app's core purpose is a package installer or app store (Play Console Policy: *Malware / Device and Network Abuse / Request Install Packages Policy*).

### Action A: Remove Permission from `AndroidManifest.xml`
In [`app/src/main/AndroidManifest.xml`](file:///h:/Programming/Main%20Projects/Android/attendence/app/src/main/AndroidManifest.xml), delete:
```xml
<!-- REMOVE THIS LINE BEFORE PLAY STORE RELEASE -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

### Action B: Disable In-App Downloader in `SettingsViewModel.kt`
In [`app/src/main/java/com/hrshd1eux/attendly/ui/settings/SettingsViewModel.kt`](file:///h:/Programming/Main%20Projects/Android/attendence/app/src/main/java/com/hrshd1eux/attendly/ui/settings/SettingsViewModel.kt):
- Change `downloadAndInstallUpdate` so it does NOT download the `.apk` file into internal storage and does NOT call `ApkInstaller.installApk(...)`.
- Instead, simply open the browser or Play Store listing with an `Intent.ACTION_VIEW`:
```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releasePageUrl))
context.startActivity(intent)
```

### Action C: Remove or Exclude `ApkInstaller.kt`
- [`app/src/main/java/com/hrshd1eux/attendly/core/util/ApkInstaller.kt`](file:///h:/Programming/Main%20Projects/Android/attendence/app/src/main/java/com/hrshd1eux/attendly/core/util/ApkInstaller.kt) can be excluded or kept dormant without calling `canRequestPackageInstalls()`.

---

## 🏷️ 2. Distinctive Title & Trademark Protection

Multiple apps named "Attendly" already exist on Google Play (e.g., `app.vercel.attendly.android`). To avoid immediate automated rejection under Google Play's **Impersonation Policy**:
- Set the Store Listing Title to a distinctive name:
  - **`Attendly: Attendance Manager`** or **`Attendly — Student Attendance`**
- Do NOT use single word "Attendly" as the sole listing title.
- Keep the in-app Trademark Notice in the About dialog:
  > *"Attendly: Attendance Manager is an independent open-source project and is not affiliated with or endorsed by other Attendly products or entities."*

---

## 🛡️ 3. Privacy Policy URL

Both Google Play and F-Droid require a live, publicly accessible Privacy Policy link even if no personal data is collected:
- Ensure the hosted URL (e.g., on GitHub Pages: `https://hrshd1eux.github.io/attendly-attendence-manager/`) includes the explicit privacy statement:
  > *"Attendly: Attendance Manager stores 100% of attendance records, schedules, and user data locally on your device using Room SQLite. No account creation, analytics, telemetry, or cloud tracking exists. Network access is used strictly on-demand if the user manually checks for software updates via GitHub."*

---

## ⚖️ 4. Academic Debarment & Calculation Disclaimer

Always preserve the legal disclaimer in the app's About and Explanation dialogs:
> *"Calculation Disclaimer: Attendly is an informal self-tracking estimator. Official university records always prevail. The authors accept no liability for academic debarment, attendance shortages, or lost examination eligibility."*

---

## 📦 5. Google Play App Bundle (.aab) & Target SDK

- Build the release bundle using:
  ```bash
  ./gradlew bundleRelease
  ```
- Google Play requires target SDK 35+ (already configured in `build.gradle.kts`).
- Ensure keystore release credentials are set up in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, etc.).
