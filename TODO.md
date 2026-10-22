# 📌 Attendly — Developer To-Do & Release Notes

## 🚨 Before Google Play Store / F-Droid Release:
Refer to [PLAYSTORE_CHECKLIST.md](PLAYSTORE_CHECKLIST.md) for full instructions:
- [ ] **Remove `REQUEST_INSTALL_PACKAGES`** from `app/src/main/AndroidManifest.xml`.
- [ ] **Disable in-app `.apk` downloader** in `SettingsViewModel.kt` (redirect to browser / store URL via `Intent.ACTION_VIEW` instead of local file installation).
- [ ] **Store Title**: Publish as **"Attendly: Attendance Manager"** to prevent trademark/impersonation collision with existing Play Store apps named "Attendly".
- [ ] **Privacy Policy URL**: Link the GitHub Pages privacy section in Google Play Console.
- [ ] **Test Target SDK**: Verify build with targetSdk 35 on Google Play Console.
