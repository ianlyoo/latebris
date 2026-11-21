# Limitations

What Latebris 0.2.0 does not cover, what was not tested, and how a defender can
tell it is running. The verified behaviour is in [VERIFICATION.md](VERIFICATION.md).

## Tested only here

- Emulators only, no real phone. Hook path: Android 14 (API 34) google_apis arm64
  emulator rooted with rootAVD, Magisk 26.4 with Zygisk, LSPosed 1.9.2. Stock
  path: Android 15 (API 35) google_apis arm64 emulator.
- Not tested: the hook path on Android 15 or later, Vector or other LSPosed forks,
  KernelSU, vendor builds (Samsung, Xiaomi and so on), real radios.
- LSPosed 1.9.2 applied a module scope change only after a reboot.
- Not run on an emulator: route playback with an OpenRouteService key, OpenCellID
  Auto Fill, and Capture. The two network clients have unit tests against a mock
  server.

## Signals not covered

- **Cells:** only LTE. The hook replaces the whole cell list with the preset's
  LTE cells, so an app looking for 5G NR, WCDMA or GSM cells finds none. Signal
  values are drawn from fixed ranges (RSRP −95 to −85 dBm, RSRQ −12 to −8 dB).
- **MNC with a leading zero:** presets store the MNC as a number, so a
  three-digit MNC below 100 (for example `050`) comes out as two digits (`50`).
- **Connected Wi-Fi:** only the scan list is replaced. `WifiManager.getConnectionInfo()`
  and the network's `WifiInfo` keep the real connected network. No
  `SCAN_RESULTS_AVAILABLE_ACTION` broadcast is sent for the preset list, and every
  preset access point carries the current time as its scan timestamp.
- **GNSS:** satellites (`GnssStatus`), NMEA sentences and raw GNSS measurements are
  not simulated.
- **Location fields:** the hook changes latitude, longitude, altitude, accuracy
  and the mock flag of `Location` objects from the gps, network, passive and fused
  providers. Speed, bearing, time and extras are left as they are.
- **Hidden APIs:** LTE cells are built through hidden constructors and setters
  (`CellIdentityLte`, `CellSignalStrengthLte`, `CellInfoLte`), and on Android 13+
  the scan result's SSID is set through the hidden `ScanResult.setWifiSsid`. A
  later Android release can remove them; the hook then logs the failure and
  leaves the real values in place.

## Lifecycle

- **Stop** restores the real values on both paths.
- **If Latebris is killed during a simulation** (crash or force-stop), hooked
  Wi-Fi and cell values return to the real ones within about 10 seconds, because
  the hook only trusts state with a fresh heartbeat. GPS does not: Android keeps
  the mock location providers of a dead app registered and keeps serving the last
  fake position. Opening Latebris again removes them.

## How a defender can detect Latebris

On the stock path:

- Every location carries the mock flag (`Location.isMock()`, Android 12+).
- The device has a mock location app set (`AppOpsManager.OPSTR_MOCK_LOCATION`),
  and the gps and network providers are test providers while it runs.

On the hook path:

- Root, Magisk, Zygisk and LSPosed are all detectable: the `su` binary, the
  Magisk and LSPosed manager apps, files under `/data/adb`, and Xposed bridge
  frames in a stack trace taken inside a hooked method.
- The Latebris package itself is visible to apps that may query installed
  packages, and the module's state file sits in LSPosed's shared-preferences
  directory, readable by every app in its scope.
- Hooked getters are slow. In our probe, 1000 calls to `Location.getLatitude()`
  took 4 to 37 ms inside the scope and under 1 ms outside it.
- Apps outside the scope still see the stock path's mock location, so the same
  device reports a mock-flagged fix to one app and a clean one to another. A
  server that compares reports from several apps on one device can see that.
- The signals do not agree with each other: a GPS fix with no matching satellites
  in `GnssStatus`, a connected Wi-Fi network that is missing from the scan list,
  a cell list with LTE only, and an IP address geolocated elsewhere.
- Device attestation (Play Integrity) on a rooted device with an unlocked boot
  loader normally fails. We did not test this.
