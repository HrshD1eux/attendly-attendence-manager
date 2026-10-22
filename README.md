<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" alt="Attendly Logo" />
</p>

<h1 align="center">Attendly — Attendance Manager</h1>

<p align="center">
  <b>A modern, minimal, 100% offline-first, Free & Open-Source (FOSS) student attendance tracker built natively for Android.</b>
</p>

<p align="center">
  <a href="https://hrshd1eux.github.io/attendly-attendence-manager/"><img src="https://img.shields.io/badge/Website-GitHub_Pages-blue?logo=github" alt="Website" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License: Apache 2.0" /></a>
  <img src="https://img.shields.io/badge/FOSS-100%25_Free_%26_Open_Source-success.svg" alt="100% FOSS" />
  <img src="https://img.shields.io/badge/Google_Play_Services-0%25_Zero_GMS-brightgreen.svg" alt="Zero GMS" />
  <img src="https://img.shields.io/badge/Offline_First-100%25_Local_Storage-blue.svg" alt="100% Offline First" />
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-green.svg" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-purple.svg" alt="Material 3" />
</p>

---

## 🕊️ 100% FOSS & Zero Google Proprietary Libraries

**Attendly is built from the ground up as strictly Free and Open-Source Software (FOSS).**

- **Zero Google Proprietary Dependencies (No GMS)**: Does not link against `play-services-*`, Google Play In-App Review, Google Play Billing, or closed-source Google binaries.
- **No Firebase, Analytics, or Telemetry**: Free from Firebase, Crashlytics, AdMob, or background telemetry.
- **100% Offline Attendance Storage**: All attendance records, timetable sessions, and subject statistics reside strictly on your local device. Network access is used exclusively on-demand when you tap "Check for Updates" to query official GitHub Releases.
- **De-Googled ROM Ready**: Runs smoothly on privacy-focused Android distributions such as **GrapheneOS**, **CalyxOS**, **LineageOS**, **/e/OS**, and standard AOSP without requiring microG or Google Play Services.
- **Permissive Open-Source License**: Released under the [Apache License, Version 2.0](LICENSE).

---

## 🌟 Key Features

1. **Academic Dashboard & Today's Schedule**
   - Personal student greeting and real-time semester attendance summary.
   - Interactive chronological **timeline** connecting today's classes with node indicators.
   - Single-tap **Present**, **Absent**, and **Cancelled** attendance recording directly from the timeline.
   - Quick stat cards showing current **Attendance %** with health status and **Safe Bunks / Recovery Needed**.
   - Quick action buttons for **+ Extra Class** and **Bunk Simulator**.
   - Undo snackbar for immediate reversal of accidental actions.

2. **Smart Class-End Notifications & Background Alarms**
   - Actionable notifications triggered right as classes conclude with one-tap action buttons: `✓ Attended`, `✕ Missed`, and `🚫 Cancelled`.
   - Configurable notification timing offset in Settings (on dismissal, or 5, 10, or 15 minutes before class ends).
   - Android `BootReceiver` integration that automatically re-schedules today's alarms whenever your device reboots or the app updates.

3. **"Catch-Up" Triage Inbox for Unmarked Classes**
   - Never lose track of attendance when you forget to mark classes during busy days.
   - Automatically detects past unmarked sessions over the last 7 days.
   - Dismissible home-screen triage banner with instant **"Mark All Present"** or single-tap sequential review.

4. **Extra & Rescheduled Lecture Logger**
   - Record ad-hoc, substitute, or extra lectures held outside the standard weekly schedule.
   - Choose any subject, date, custom start/end times, and attendance status without polluting or modifying your master timetable.

5. **"What-If" Bunk Simulator & Fest Planner**
   - **Quick Bunk Meter**: Interactive steppers to simulate missing upcoming lectures; visualizes attendance drop and computes exact consecutive classes required to recover back to target.
   - **Fest & Vacation Planner**: Select start and end dates to calculate the cumulative attendance impact across all scheduled subjects before booking trips or attending college fests.

6. **University Leave & Concession Rules (On-Duty & Medical)**
   - Tailored to real-world university attendance bylaws.
   - Dedicated statuses for **On-Duty (OD)** and **Medical Leave**.
   - Configurable OD policy: Counted as *Present* (accredited) or *Excused* (neutral).
   - Configurable Medical policy: *Excused* (excluded from denominator) or *Target Concession* (e.g. relaxed required threshold from 75% down to 65%).

7. **Instant QR Code & Auto-Clipboard Timetable Sync**
   - Share weekly timetables with classmates in seconds via GZIP-compressed QR codes (`ATTENDLY:v1:...`).
   - Built-in QR scanner and image picker with ZXing.
   - Automatic clipboard detection on app launch with one-tap import banner.

8. **Accurate Attendance Engine & Pure Math Calculations**
   - Precise arithmetic using integer inequalities to prevent floating-point boundary errors.
   - **Safe Absences Calculator**: Computes the exact number of classes a student can miss while maintaining $\ge \text{Target}\%$.
   - **Recovery Classes Calculator**: Computes the minimum number of consecutive classes required to restore attendance back to $\text{Target}\%$.
   - Special-case handling for boundary edge cases (e.g., target 100% with prior absences).
   - Excludes cancelled classes from the attendance denominator.
   - Mid-semester baseline totals (support for initial attended and missed count offsets).

9. **Timetable Management & AI Text Import**
   - Weekly day-by-day timetable planner and visual schedule viewer.
   - Conflict detection with warning prompts for overlapping class timings.
   - Configurable laboratory attendance unit policy: *1 unit per session* vs *1 unit per hour*.
   - **AI Timetable Prompt Generator**: Copy-paste prompts designed for ChatGPT, Gemini, or Claude to turn timetable PDFs or image tables into structured plain text.
   - Built-in plain-text and `.txt` file timetable parser with live review preview (recognized sessions, warnings, unrecognized lines) and Replace vs Merge options.

10. **Monthly Calendar with Holiday Markers & Historical Corrections**
    - Navigate months with previous/next controls and instant "Today" jump.
    - Color-coded indicator dots (Green for attended, Red for absences, Amber for OD/Medical, Grey for cancelled).
    - Holiday integration with national holidays preloaded, custom holiday creation, and calendar red circle badges.
    - Daily breakdown of classes on the selected date with the ability to edit status or record ad-hoc sessions.

11. **Android Home-Screen Widgets (Jetpack Glance)**
    - **Attendly Overview Widget**: Glance widget displaying overall attendance percentage, target, and status badge with tap-to-open.
    - **Quick Mark Widget**: Interactive widget with direct **Present**, **Absent**, and **Cancelled** action buttons that record attendance and refresh statistics directly from your home screen with zero delay.

12. **Offline Backup, Semester Archive & Data Privacy**
    - 100% offline — zero tracking, accounts, ads, or external network requests.
    - Automated local backups with seamless restore prompts.
    - Clean **New Semester** reset with secure historical data archiving.
    - Export and restore versioned JSON backups using Android's Storage Access Framework (SAF).
    - Export full attendance records to human-readable CSV.

13. **Material 3 Design System**
    - Full Light, Dark, and Follow System theme modes persisted via Jetpack DataStore Preferences.
    - Semantic color system (Green, Red, Amber, Neutral) with accessible contrast, labels, and icons.
    - Full edge-to-edge layout support.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture Pattern**: MVVM with Clean Architecture (Domain, Data, UI, Widget layers)
- **Local Database**: Room 2.7.0 (AOSP / SQLite) with Kotlin Coroutines and Flow
- **Preferences**: Jetpack DataStore Preferences
- **Dependency Injection**: Dagger Hilt
- **Home Screen Widgets**: Jetpack Glance 1.1.1
- **QR Code Engine**: ZXing Core 3.5.3 (pure Java offline barcode engine)
- **Testing**: JUnit 4, Kotlinx Coroutines Test

---

## 🚀 Building and Running

### Prerequisites
- JDK 17 (Java 17)
- Android SDK Platform 36 (build-tools 35.0.0 / 36.0.0)

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
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
   - Validates Gradle wrapper, runs all unit tests, compiles the debug APK, and uploads the debug artifact for quick testing.

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

### Setting Up Release Keystore Secrets
In your GitHub repository, navigate to **Settings > Secrets and variables > Actions** and add:

| Secret Name | Description | Example / Notes |
|---|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.keystore` / `.jks` file | The Base64 string from `attendly-keystore-base64.txt` |
| `KEYSTORE_PASSWORD` | Keystore password | Configured release password |
| `KEY_ALIAS` | Key alias name | `attendly` |
| `KEY_PASSWORD` | Key password | Configured release password |

*(Note: If secrets are omitted, the workflow automatically generates an automated fallback keystore so release builds always succeed without failing.)*

---

## 📄 License
```
Copyright 2026 HrshD1eux and Attendly Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
