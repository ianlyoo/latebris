# Changelog

## [0.2.0]

The LSPosed hooks now work in other apps. Verified on an Android 14 (API 34)
emulator with Magisk and LSPosed 1.9.2, and the stock mock-location path on an
Android 15 (API 35) emulator, against a separate probe app; see
[docs/VERIFICATION.md](https://github.com/ianlyoo/latebris/blob/main/docs/VERIFICATION.md).

**Uninstall Latebris 0.1.x before installing 0.2.0.** The application id
changed from `com.example.gpstick` to `io.github.ianlyoo.latebris`, so Android
treats it as a different app.

### Fixed
- Opening Latebris after it was force-stopped or crashed mid-simulation removes
  the mock location providers the dead process left behind. Before, the device
  kept reporting the last fake position while the app showed Stopped.
- Hooks read the simulation state through LSPosed shared preferences. Before,
  they asked an exported content provider, which Android 11+ package
  visibility hides from apps that do not declare it, so in other apps every
  hook silently returned the real value.
- Cell info: LTE cells are built with the exact API 30+ constructor. Before,
  cell id, PCI, TAC and EARFCN landed in the wrong fields and MCC/MNC were
  empty. The first preset tower is reported as the registered serving cell.
- Wi-Fi scan results carry `capabilities` and, on Android 13+, `getWifiSsid()`.
- A hooked `Location` getter no longer makes a cross-process call each time.
- Movement positions are stored at full precision.
- `LatebrisViewModelTest` was stored in EUC-KR and failed to compile its inputs
  correctly; it is UTF-8 now.

### Added
- `requestCellInfoUpdate()` is hooked as well as `getAllCellInfo()`.
- Options: enter your own OpenRouteService and OpenCellID keys. Without a
  route key, movement follows a straight line, and the Move panel says so.
- Status shows whether the LSPosed module is active.
- A separate probe app (`probe/`, scoped and unscoped flavors) and
  `tools/verify-emulator.sh`, which runs it and saves the readings.
- CI: unit tests, debug builds and lint on every push.

### Changed
- No API key is built in. The OpenRouteService key that 0.1.x embedded has
  been removed from the build.
- The exported state provider, probe activity and probe receiver are gone,
  and app backup is off, so presets with real coordinates and BSSIDs stay on
  the device.
- The release workflow creates a draft and never moves a tag.
- README (English and Korean) and the project page are rewritten from the
  verification results. Removed: the npm/bun install steps (there is no npm
  package), the benchmark (no data behind it), the CI badge and leftover
  scaffold files. Added docs/VERIFICATION.md and docs/LIMITATIONS.md.

## [0.1.2] - 2026-07-03
## [0.1.1] - 2026-06-30
## [0.1.0] - 2026-06-30

Initial releases.
