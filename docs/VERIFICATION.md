# Verification

How Latebris 0.2.0 was checked, and what each check showed. Everything here ran
on emulators on one Mac (Apple silicon) in November 2026. What was not checked is
in [LIMITATIONS.md](LIMITATIONS.md).

0.3.0 changed only the interface. The whole run below was repeated with the 0.3.0
build on both emulators and gave the same readings at every step; the getter
cost in scope was 4 to 22 ms and under 1 ms outside it.

## Set-up

| | Hook path | Stock path |
|---|---|---|
| Emulator image | Android 14, API 34, google_apis arm64 | Android 15, API 35, google_apis arm64 |
| Build fingerprint | `google/sdk_gphone64_arm64/emu64a:14/UE1A.230829.050/12077443:userdebug/dev-keys` | `google/sdk_gphone64_arm64/emu64a:15/AE3A.240806.043/12960925:userdebug/dev-keys` |
| Root | rootAVD (commit 613caa4) with Magisk 26.4, Zygisk on | none |
| Hook framework | LSPosed 1.9.2 (7024), Zygisk build | none |
| Latebris | module enabled; scope: the `scoped` probe only | mock location app (`appops android:mock_location allow`) |

The probe is a separate app in this repository (`probe/`). It reads location,
Wi-Fi and cell data through the public APIs an ordinary app uses and logs each
reading as one JSON line (logcat tag `LatebrisProbe`):

- `lm.gps.last`, `lm.gps.current`: `LocationManager.getLastKnownLocation` and
  `getCurrentLocation` on the gps provider;
- `fused.current`: Google Play services fused location;
- `getter.cost`: wall time of 1000 `Location.getLatitude()` calls;
- `wm.scanResults`: `WifiManager.getScanResults()`;
- `tm.allCellInfo`, `tm.requestCellInfoUpdate`: `TelephonyManager`.

It is built twice, as `io.github.ianlyoo.latebris.probe.scoped` and
`...probe.unscoped`, so that one copy sits inside the LSPosed scope and the other
is the control outside it. Latebris never hooks anything but the apps in its scope.

## The run

`tools/verify-emulator.sh <out-dir>` runs both probes at six steps and saves
their JSON lines, a screenshot of each, the hook log lines, the device details
(fingerprint, APK hashes, Magisk version, the LSPosed module and scope tables), a
screen recording of one probe run, and a summary table. The emulator's real
position is set to 48.8584, 2.2945 with `adb emu geo fix` first; the preset is
"Urban core" (37.5665, 126.9780; two access points `qa-lab-a` and `qa-lab-b`;
one LTE cell 450/05).

### Hook path (API 34, Magisk, LSPosed)

| Step | Probe | GPS | Wi-Fi scan | Serving cell |
|---|---|---|---|---|
| 1. Simulation off | scoped | 48.8584, 2.2945 · mock false | AndroidWifi | LTE 310/260 CI 47108 |
| | unscoped | 48.8584, 2.2945 · mock false | AndroidWifi | LTE 310/260 CI 47108 |
| 2. Start | scoped | **37.5665, 126.9780 · mock false** | **qa-lab-a, qa-lab-b** | **LTE 450/05 CI 22011 PCI 101 TAC 101** |
| | unscoped | 37.5665, 126.9780 · mock true | AndroidWifi | LTE 310/260 CI 47108 |
| 3. Stop | scoped | 48.8584, 2.2945 · mock false | AndroidWifi | LTE 310/260 CI 47108 |
| | unscoped | 48.8584, 2.2945 · mock false | AndroidWifi | LTE 310/260 CI 47108 |
| 4. Start again | scoped | **37.5665, 126.9780 · mock false** | **qa-lab-a, qa-lab-b** | **LTE 450/05 CI 22011** |
| | unscoped | 37.5665, 126.9780 · mock true | AndroidWifi | LTE 310/260 CI 47108 |
| 5. Latebris force-stopped, 12 s later | scoped | 37.5665, 126.9780 · mock true | AndroidWifi | LTE 310/260 CI 47108 |
| | unscoped | 37.5665, 126.9780 · mock true | AndroidWifi | LTE 310/260 CI 47108 |
| 6. Latebris reopened | scoped | 48.8584, 2.2945 · mock false | AndroidWifi | LTE 310/260 CI 47108 |
| | unscoped | 48.8584, 2.2945 · mock false | AndroidWifi | LTE 310/260 CI 47108 |

The scoped probe read the same preset values from `getLastKnownLocation`,
`getCurrentLocation` and fused location, and the same cell from
`getAllCellInfo` and `requestCellInfoUpdate`. The Wi-Fi capabilities of the
preset access points read `[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]`.

Step 5 shows the lifecycle limit described in LIMITATIONS.md: the hooked Wi-Fi
and cell values went back to the real ones once the heartbeat expired, while GPS
stayed on the last fake position because Android kept the dead app's mock
providers. Reopening Latebris removed them (step 6; `dumpsys location` then lists
the gps and network providers without `[mock]`).

1000 `getLatitude()` calls took 5 to 22 ms in the scoped probe and under 1 ms in
the unscoped one.

The Status screen of Latebris reads "LSPosed module: Active" on this emulator and
"Not active" on the stock one.

### Stock path (API 35)

| Step | GPS (both probes) | Wi-Fi, cell |
|---|---|---|
| 1. Simulation off | 48.8584, 2.2945 · mock false | real |
| 2. Start | 37.5665, 126.9780 · mock true | real |
| 3. Stop | 48.8584, 2.2945 · mock false | real |
| 4. Start again | 37.5665, 126.9780 · mock true | real |
| 5. Latebris force-stopped, 12 s later | 37.5665, 126.9780 · mock true | real |
| 6. Latebris reopened | 48.8584, 2.2945 · mock false | real |

Movement without a routing key, on the same emulator: Move from the "Suburban
loop" preset towards "Urban core" at a 50 km/h setting. Ten position samples in
18 seconds moved steadily towards the destination, 246.3 m in total (13.68 m/s
against the set 13.89 m/s), never more than 0.04 m off the straight line between
the two points.

## Builds and tests

```bash
./gradlew assembleDebug assembleRelease testDebugUnitTest
adb shell settings put global hidden_api_policy 1   # on the test emulator
./gradlew connectedDebugAndroidTest
```

The instrumented tests build real `ScanResult` and `CellInfoLte` objects through
the hidden APIs the hooks use, check the hook state Latebris publishes, and
check that movement coordinates keep full double precision. Without
`hidden_api_policy 1` the hidden-API tests fail on a stock emulator, because
test APKs are not exempt from the hidden-API checks.

## Reproduce

1. Build: `./gradlew :app:assembleDebug :probe:assembleDebug`.
2. Install Latebris and both probes with `adb install -r -g`, then
   `adb shell appops set io.github.ianlyoo.latebris android:mock_location allow`.
3. Hook path only: root the emulator (rootAVD with Magisk, Zygisk on), install
   LSPosed, enable the Latebris module with only the scoped probe in its scope,
   reboot, then `adb root`.
4. Open Latebris, pick a preset, and run
   `ANDROID_SERIAL=<emulator> tools/verify-emulator.sh <out-dir>`.
