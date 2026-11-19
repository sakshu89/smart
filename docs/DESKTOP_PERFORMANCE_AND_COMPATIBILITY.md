# Desktop mode: performance, compatibility and verification

## Compatibility target

The project currently declares `minSdk = 26` (Android 8.0). This includes Android 9 and newer without excluding Android 8 devices. Keep this minimum unless a concrete dependency or API requirement forces a change.

## Performance changes in this revision

- Usage-stat queries run on `Dispatchers.IO`, not the Compose/UI thread.
- The desktop recent-app strip refreshes immediately and then every 30 seconds while the desktop screen is composed, instead of every 4 seconds.
- Recently launched package history is bounded to 24 entries.
- Installed-app lookup is indexed once per refresh rather than repeatedly scanning the full app list for every package.
- Desktop mode defaults to disabled for new installs; existing saved preferences remain respected.
- App-details launch failures are logged and surfaced to the user instead of being silently swallowed.

## Android windowing constraints

A third-party launcher cannot force arbitrary apps into freeform windows or override an app's manifest/system multi-window restrictions. `ActivityOptions.setLaunchBounds()` is a request; Android and the device manufacturer decide whether to honor it. Split-screen availability and accessibility-based system actions vary by Android version and OEM.

The desktop's recent-app list is based on UsageStats and launcher launch history. It is not a guaranteed list of currently alive processes. Android intentionally restricts process visibility and background execution.

## Device verification matrix

Before calling a build release-ready, test at least:

- Android 9, Android 10/11, Android 12/13, Android 14/15/16 where available.
- Low-memory device (2–3 GB RAM), mid-range device, and high-refresh/high-end device.
- Portrait/landscape rotation, launcher process recreation, app install/uninstall, work-profile apps, permission denied/granted, and accessibility service disabled/enabled.
- App launch latency, scroll smoothness, memory after 15 minutes, background battery drain, and repeated split-screen requests.

## Release gates

A successful Gradle build is not proof of smoothness or low memory use. Require CI build/lint/unit-test success plus physical-device smoke tests and Android Studio/Perfetto profiling. Do not claim universal 60/90/120 FPS, a fixed RAM footprint, or universal floating-window support without measurements on the target devices.
