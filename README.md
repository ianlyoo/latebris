# Latebris

Latebris is an Android app that plays back a saved location environment: a GPS
position, the Wi-Fi networks a scan returns, and the LTE cells the phone reports.
Use it to test location-dependent features on devices and apps you own or are
allowed to test.

[한국어](README.ko.md) · [Releases](https://github.com/ianlyoo/latebris/releases) · [Verification](docs/VERIFICATION.md) · [Limitations](docs/LIMITATIONS.md) · MIT licence

## What it does

A preset bundles a position, a list of Wi-Fi access points and a list of LTE
cells. Press Start and Latebris serves that preset in one of two ways.

| | Stock path | Hook path |
|---|---|---|
| Device | Any Android 12+ device; Latebris is the mock location app in Developer options | Rooted with Magisk, Zygisk and LSPosed; the Latebris module is enabled and the target app is in its scope |
| GPS | `LocationManager` and fused location return the preset position, marked as mock | Apps in scope get the preset position, not marked as mock |
| Wi-Fi | Real | Apps in scope: `WifiManager.getScanResults()` returns the preset networks |
| Cell | Real | Apps in scope: `getAllCellInfo()` and `requestCellInfoUpdate()` return the preset LTE cells |
| Verified on | Android 15 emulator (API 35) | Android 14 emulator (API 34), Magisk 26.4, LSPosed 1.9.2 |

Apps outside the LSPosed scope keep seeing the real Wi-Fi and cell data, and
see GPS the stock way.

Measured on the hook path with the separate probe app, once inside the scope and
once outside it, while the "Urban core" preset ran (full table and files in
[docs/VERIFICATION.md](docs/VERIFICATION.md)):

| Reading | Probe in scope | Probe outside scope |
|---|---|---|
| GPS (`getCurrentLocation`, fused) | 37.5665, 126.9780 · mock = false | 37.5665, 126.9780 · mock = true |
| Wi-Fi scan | `qa-lab-a`, `qa-lab-b` (preset) | `AndroidWifi` (the emulator's own access point) |
| Serving cell | LTE 450/05, CI 22011, PCI 101, TAC 101 (preset) | LTE 310/260, CI 47108 (the emulator's own cell) |

After Stop, both probes read the real position again (48.8584, 2.2945) and the
emulator's own Wi-Fi and cell.

![Latebris running on the rooted API 34 emulator, next to the probe inside the LSPosed scope (mock: false) and the probe outside it (mock: true)](docs/assets/hook-path-emulator.png)

Also in the app:

- **Move** plays a route from the current position to a saved destination at a
  set speed. Without an OpenRouteService key it moves in a straight line, which
  was checked on the API 35 emulator: 246 m in 18 s at a 50 km/h setting
  (13.7 m/s against 13.9). With a key it asks for a road route; that request is
  covered by unit tests only.
- **Capture** saves the device's current position, Wi-Fi scan and cells as a
  preset. Not checked on an emulator.
- **Auto Fill** looks up nearby cells for a preset from OpenCellID. Unit tests
  only.

## Install

Download `latebris-0.2.0.apk` from [Releases](https://github.com/ianlyoo/latebris/releases).

**Uninstall any 0.1.x build first.** 0.2.0 changes the package name from
`com.example.gpstick` to `io.github.ianlyoo.latebris`, so an old build would stay
installed next to it and act on the same signals.

Stock path:

1. Install the APK and allow location and notifications.
2. Developer options → Select mock location app → Latebris.
3. Pick a preset and press Start. Stop returns the device to its real position.

Hook path, in addition:

1. Enable the Latebris module in LSPosed and add the app you are testing to its scope.
2. Reboot. LSPosed 1.9.2 applied a scope change only after a reboot in our tests.
3. The Status screen shows "LSPosed module: Active" once the module is loaded.

Route playback and Auto Fill need your own OpenRouteService and OpenCellID keys.
Enter them under Options; they stay on the device.

## Build from source

Needs JDK 17 and the Android SDK (platform 35).

```bash
./gradlew assembleDebug testDebugUnitTest
# Instrumented tests build hidden-API objects through reflection:
adb shell settings put global hidden_api_policy 1
./gradlew connectedDebugAndroidTest
```

Optional build-time keys: the Gradle properties or environment variables
`OPEN_ROUTE_SERVICE_API_KEY` and `OPEN_CELL_ID_API_KEY`. Release signing reads
`ANDROID_SIGNING_STORE_FILE`, `ANDROID_SIGNING_STORE_PASSWORD`,
`ANDROID_SIGNING_KEY_ALIAS` and `ANDROID_SIGNING_KEY_PASSWORD`.

The `probe` module builds the probe app in two flavours, `scoped` and `unscoped`,
and `tools/verify-emulator.sh` runs the whole check on an emulator. See
[docs/VERIFICATION.md](docs/VERIFICATION.md).

## Limitations and responsible use

[docs/LIMITATIONS.md](docs/LIMITATIONS.md) lists what is not covered (5G NR cells,
the connected Wi-Fi network, GNSS satellite status, real phones, other Android
and LSPosed versions) and how a defender can detect Latebris. In short: the stock
path marks every location as mock, and the hook path needs root and LSPosed,
which apps can detect.

Use Latebris only on devices you own and apps you are allowed to test.

## Licence

MIT. See [LICENSE](LICENSE).
