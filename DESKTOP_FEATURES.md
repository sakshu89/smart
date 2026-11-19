# Desktop environment feature integration

This source tree now contains an initial integrated desktop feature layer built on the existing Kvaesitso desktop mode.

## Included in this build

1. **Desktop environment** — keeps the existing desktop home, taskbar, app menu, keyboard shortcuts, mouse secondary-click menus, and Android window-launch integration. `Ctrl+Alt+B` opens the desktop browser; `Ctrl+Alt+L` attempts to open the Linux runtime. Recent-app refresh is now asynchronous on `Dispatchers.IO`, uses a package lookup map, bounds the locally launched history, and polls every 30 seconds instead of every 4 seconds.
2. **Desktop browser** — a resizable in-app WebView activity with desktop-oriented viewport/user-agent settings, URL/search input, back navigation, and `Ctrl+L`, `Ctrl+R`, `Ctrl+W` shortcuts. YouTube opens as a website, not as the native YouTube application.
3. **Android app compatibility** — preserves native Android app launching, split-screen/freeform launch requests, and fullscreen fallback. The Android OS and OEM decide whether an app may actually resize or enter freeform mode.
4. **Gaming companion foundation** — stores per-package key/action bindings and exposes profile read/write/delete APIs. The current integration is profile storage and a launcher entry point, not a system-wide touch injector. A production mapper needs a separately designed, user-consented input/accessibility service and device/game compatibility testing; games that reject simulated input must remain unsupported.
5. **Linux runtime handoff** — detects and opens an already-installed Termux or Termux:X11 app. This launcher does not bundle a Linux distribution, root the device, or claim to run Linux GUI apps itself. A distro and compatible graphical session must be installed/configured by the user.

## Known platform limits

- `ActivityOptions.setLaunchBounds()` is a request. Many Android builds ignore it unless desktop/freeform windowing is enabled by the OS/OEM.
- A WebView desktop user-agent does not guarantee identical desktop website behavior. Some sites require browser-specific features or sign-in flows unavailable in WebView.
- The gaming module currently persists profiles only. It does not inject touch events, bypass anti-cheat, or modify games.
- Linux GUI support depends on an external runtime such as Termux plus a compatible X11/VNC display setup; it is not a native Android window manager feature.

## Suggested next verification

Build `defaultDebug` and `fdroidDebug` in GitHub Actions, then test on a phone with a physical keyboard/mouse and on a device with desktop/freeform windowing enabled. Validate browser playback, app fallback behavior, profile persistence, and Termux handoff separately.


## High-performance input and display update (experimental v2)

- `GameKeyMapperAccessibilityService` is an opt-in AccessibilityService that can translate configured physical key-down events into normalized screen taps or swipes. Enable it manually in Android Accessibility settings. Profile keys use `KeyEvent.keyCodeToString`, e.g. `KEYCODE_BUTTON_A` or `KEYCODE_W`; actions use `tap:x:y` or `swipe:x1:y1:x2:y2:durationMs`, with normalized coordinates from 0 to 1. Profiles are package-specific.
- This is a gesture bridge, not a full commercial-grade mapper UI: no analog joystick, mouse-look, multi-touch chord engine, macro recorder, or game compatibility guarantee is included yet. Some games reject accessibility gestures. Do not use it to bypass anti-cheat or game restrictions.
- `DesktopRefreshRateController` queries display modes and requests the highest reported mode for this app's own window. It does not set a global refresh rate or control another app's rendering. A panel that reports 60 Hz cannot be made 185 Hz by software; 1 Hz may be an internal low-power mode not exposed to third-party apps. Actual cadence is chosen by Android/OEM and may be reduced for battery/thermal policy.
- This code is source-integrated but has not been Gradle-compiled or device-tested in this environment.
