# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Android developers, QA engineers and security researchers who test location-dependent features on devices they own or are allowed to test. They sit at a desk with a test phone or an emulator, often with a rooted device running LSPosed, and switch between a target app and Latebris many times in a session. (Inferred from the README, docs/VERIFICATION.md and the app's own copy; not confirmed in an interview.)

## Product Purpose

Latebris plays back a saved location environment: a GPS position, the Wi-Fi access points a scan returns, and the LTE cells the phone reports. On a stock phone GPS goes through Android's mock location; with Magisk, Zygisk and LSPosed all three are served only inside the apps in the module's scope. Success: the tester picks a preset, presses Start, sees at a glance which signals are live, and gets real values back the moment they press Stop.

## Positioning

Most mock-location apps move only GPS, system-wide, and leave the mock flag on. Latebris moves GPS, Wi-Fi scan and LTE cells together, and on the hook path only inside chosen apps, while every other app keeps its real Wi-Fi and cell data.

## Operating Context

- Two paths: the stock path (Latebris is the mock location app in Developer options) and the hook path (LSPosed module, target app in scope, reboot after a scope change).
- Presets hold a name, coordinates, altitude, accuracy, Wi-Fi networks (SSID, BSSID, level, frequency, capabilities) and LTE cells (MCC, MNC, TAC, CI, PCI, EARFCN). Capture saves the device's current state as a preset; Auto Fill looks cells up from OpenCellID.
- Move plays a route from the current position to a saved destination at a set speed; a road route needs the user's OpenRouteService key, otherwise it is a straight line.
- Options: master switch and per-signal toggles (GPS, Wi-Fi, cell, movement), applied on the next Start; the two API keys, stored on the device only.
- Status shows permissions (location, notifications), whether the LSPosed module is active, and the active coordinates.

## Capabilities and Constraints

- Jetpack Compose, Material 3, minSdk 31, targetSdk 35. The UI layer (`ui/`) may be rewritten; `LatebrisViewModel`, the hooks, the service and the stored state are verified and must keep working unchanged in behaviour.
- Instrumented tests address UI elements through `LatebrisTestTags`; tags that tests use must survive a redesign.
- Movement modes and presets come from the existing view model; no new capability is claimed by the UI.

## Brand Commitments

- Name: Latebris (Latin, "hiding place"). The in-app Help ends with «Made by Youngin, just for you.»; keep it.
- The owner asked for a full redesign that replaces the old look: «앱 프론트엔드 디자인 다시 해줘. 지금은 예전 ui 랑 디자인 그대로라, 오푸스로 전면 재디자인 해주라.» (#1956)

## Evidence on Hand

- Verified behaviour and numbers: docs/VERIFICATION.md, docs/LIMITATIONS.md.
- Bundled presets: app/src/main/assets/presets/default-presets.json (Urban core, Suburban loop, Rural dropout).
- No user counts, ratings or testimonials exist; none may be shown.

## Product Principles

1. State before decoration: which signals are live, and from which preset, must be readable in one glance.
2. One primary action at a time: Start or Stop is always within reach.
3. Honest labels: the UI says what the verification showed, including what stays real (apps outside the scope) and what needs a key.
4. Reversible by default: Stop always returns real values, and the UI says so.

## Accessibility & Inclusion

Follow Android accessibility defaults: 48 dp touch targets, TalkBack labels on icon-only controls, contrast at WCAG AA in light and dark themes, and font scaling up to 200 % without clipping. (Inferred; no product-specific standard was set.)
