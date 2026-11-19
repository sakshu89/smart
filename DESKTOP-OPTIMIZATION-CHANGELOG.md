# Desktop optimization revision

This revision is a conservative stability/performance pass over the uploaded SmartDock/Kvaesitso source.

## Changes

- First-run desktop mode is now opt-in; a previously saved user choice is preserved.
- UsageStats collection and app-list processing run on `Dispatchers.IO`.
- Desktop recent-app refresh interval changed from 4 seconds to 30 seconds, with an immediate first refresh.
- Recently launched package history is bounded to 24 entries.
- Installed-app lookup is indexed once per refresh instead of repeatedly scanning the installed-app list.
- Taskbar copy now describes the UsageStats-backed list as recent apps, not guaranteed live processes.
- App-details launch failures are logged and reported instead of silently ignored.
- Added an unsigned GitHub Actions CI workflow for unit tests, lint and debug APK assembly.
- Added device compatibility/performance verification notes.

## Not claimed as complete

This is not a proof of universal smoothness, minimum RAM use, zero crashes, or universal floating-window support. Those require successful Gradle CI and physical-device profiling across the intended Android/OEM matrix. Android owns multi-window policy and may ignore requested launch bounds.
