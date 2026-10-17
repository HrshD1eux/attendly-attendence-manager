# Attendly — Native Android Student Attendance Tracker

**Attendly** is a modern, minimal, 100% offline-first attendance tracking application built natively for Android using Kotlin, Jetpack Compose, Material 3, Room, and Jetpack Glance.

---

## 🌟 Key Features

1. **Custom Dashboard & Today's Schedule**
   - Welcomes the student by name (e.g. *Welcome back, Raghav*).
   - Interactive chronological **timeline** connecting today's classes with node indicators.
   - Single-tap **Present**, **Absent**, and **Cancelled** attendance recording directly from the timeline.
   - 2-column quick stat cards showing current **Attendance %** with health status and **Safe Bunks / Recovery Needed**.
   - Undo snackbar for immediate reversal of accidental actions.

2. **Accurate Attendance Engine & Pure Math Calculations**
   - Precise arithmetic using integer inequalities to prevent floating-point boundary errors.
   - **Safe Absences Calculator**: Computes the exact number of classes a student can miss while maintaining $\ge \text{Target}\%$.
   - **Recovery Classes Calculator**: Computes the minimum number of consecutive classes required to restore attendance back to $\text{Target}\%$.
   - Special case handling for impossible scenarios (e.g., target 100% with prior absences).
   - Excludes cancelled classes from the attendance denominator.
   - Starting mid-semester baseline totals (initial attended and missed count support).

3. **Timetable Management & AI Text Import**
   - Day-of-week weekly schedule viewer and editor.
   - Validation ensuring class end times are later than start times, with overlapping session conflict warnings.
   - Configurable laboratory / multi-hour attendance unit policy: *1 unit for entire session* vs *1 unit per hour*.
   - **AI Timetable Prompt Generator**: Includes a ready-to-copy structured prompt for ChatGPT, Gemini, or Claude to extract timetables from images or PDFs into plain text.
   - Built-in plain-text and `.txt` file timetable parser with live review preview (recognized sessions, warnings, unrecognized lines) and Replace vs Merge options.

4. **Monthly Calendar with Historical Corrections**
   - Navigate months with previous/next controls and instant "Today" jump.
   - Color-coded indicator dots (Green for all attended, Red for absences, Grey for cancelled).
   - Daily breakdown of classes on the selected date with the ability to edit status or record ad-hoc sessions.

5. **Android Home-Screen Widgets (Jetpack Glance)**
   - **Attendly Overview Widget**: Glance widget displaying overall attendance percentage, target, and status badge with tap-to-open.
   - **Quick Mark Widget**: Interactive widget with direct **Present** and **Absent** action buttons that record attendance and refresh statistics right from the home screen!

6. **Backup, Restore & Data Privacy**
   - 100% offline — zero tracking, accounts, ads, or external network requests.
   - Export and restore versioned JSON backups using Android's Storage Access Framework (SAF).
   - Export full attendance records to human-readable CSV.
   - Data reset with confirmation safeguards.

7. **Material 3 Design System**
   - Full Light, Dark, and Follow System theme modes persisted via Jetpack DataStore Preferences.
   - Semantic color system (Green, Red, Amber, Neutral) with accessible labels and icons.
   - Edge-to-edge layout support.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture Pattern**: MVVM with Clean Architecture (Domain, Data, UI, Widget layers)
- **Local Database**: Room 2.7.0 with Kotlin Coroutines and Flow
- **Preferences**: Jetpack DataStore Preferences
- **Dependency Injection**: Dagger Hilt
- **Home Screen Widgets**: Jetpack Glance 1.1.1
- **Testing**: JUnit 4, Kotlinx Coroutines Test

---

## 🚀 Building and Running

### Prerequisites
- JDK 17 (Java 17)
- Android SDK Platform 36 (build-tools 35.0.0 / 36.0.0)

### Run Unit Tests
```bash
./gradlew test
```

### Build Release APK
```bash
./gradlew assembleRelease
```
The compiled release APK will be generated at:
```
app/build/outputs/apk/release/
```

---

## 🚀 CI/CD & Automated GitHub Releases

Attendly includes production-grade GitHub Actions workflows for continuous integration and automated releases.

### Workflows
1. **Continuous Integration (`.github/workflows/ci.yml`)**:
   - Triggers on every push and pull request to `main`/`master`.
   - Validates Gradle wrapper, runs all 33 unit tests, compiles the debug APK, and uploads the debug artifact for quick testing.

2. **Release & Publish APK (`.github/workflows/release.yml`)**:
   - Triggers manually via **Actions > Run workflow** or on push of release tags (`v*`).
   - Automatically bumps version in `version.properties` (**patch**, **minor**, or **major**).
   - Generates dynamic, categorized release notes directly from Git commits.
   - Signs the APK with modern Android v1, v2, v3, and v4 signature schemes via `apksigner`.
   - Signs the Android App Bundle (`.aab`) via `jarsigner`.
   - Computes SHA-256 package checksums.
   - Publishes a GitHub Release with:
     - `Attendly-v<version>.apk`
     - `Attendly-v<version>.aab`
     - `checksums.txt`
     - Formatted markdown release notes and commit changelog.

### Setting Up Release Keystore Secrets (Optional / Recommended)
If no secrets are configured, the workflow automatically generates an automated release keystore for the build. To sign with your official production certificate, add the following secrets in **GitHub Repository > Settings > Secrets and variables > Actions**:

| Secret Name | Description | Example / Command |
|---|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.keystore` / `.jks` file | `base64 -w 0 release.keystore` (Linux/macOS) or `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))` (PowerShell) |
| `KEYSTORE_PASSWORD` | Keystore password | `your_keystore_password` |
| `KEY_ALIAS` | Key alias name | `attendly_key` |
| `KEY_PASSWORD` | Key password | `your_key_password` |

To generate a new release keystore locally:
```bash
keytool -genkey -v -keystore release.keystore -alias attendly_key -keyalg RSA -keysize 2048 -validity 10000
```

---

## 🔒 Privacy & Offline Guarantee
Attendly stores all subject data, attendance records, timetables, and user preferences locally in a Room SQLite database and DataStore. No external servers or analytics SDKs are utilized.
