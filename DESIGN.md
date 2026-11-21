# Design

<!-- impeccable:design-schema 1 -->

The Latebris interface borrows from airport wayfinding signs. A tester glances at
the phone between runs of the app under test and should read in one look where
the device "is" and which signals are live. Signs are built for exactly that
glance, so the app takes their type, colours and one habit: yellow points to what
matters now.

Mode: Operate. Navigation and controls stay Material 3; the sign world shows in
type, colour, density and the location sign.

## Colour

| Role | Light | Dark | Use |
|---|---|---|---|
| Sign yellow | `#FFCC00` on `#16181C` | same | Live signals, the live preset name, the Live plate, the selected tab and travel mode. Nothing decorative. |
| Sign ink | `#16181C` | `#16181C` with a 1 dp `outlineVariant` border | The location sign at the top of Status and Move. |
| Primary | ink `#16181C` | enamel white `#F4F5F7` | Buttons, switches, radios, focus. Never yellow, so yellow keeps one meaning. |
| Ground / surface | `#EEF0F3` / `#FFFFFF` | `#0F1114` / `#181A1F` | Cool greys, never cream. |
| Text / muted | `#16181C` / `#4F5560` | `#F1F2F4` / `#AAB0BA` | All at least 4.5:1 on their ground. |
| Ready | `#14773D` | `#5CC98A` | A requirement that is met. |
| Error | Material error | Material error | A requirement that is missing; a blocked action. |

The theme follows the system light/dark setting: testers work at a desk under
office light and switch between apps all session, so the phone's own setting is
the right signal.

## Type

- Atkinson Hyperlegible (Regular, Bold), bundled under `res/font` with its OFL
  licence in `assets/licenses`. It was drawn for legibility at a glance, which is
  the sign brief.
- Coordinates, accuracy, counts, speeds and cell ids use the system sans with
  tabular figures (`Figures` in `Type.kt`) so live numbers do not jitter.
- Scale: headlineMedium 28 for the preset name on the sign, titleLarge 22 for
  section headings, titleMedium 17 for line labels, body 14–16. Sentence case
  everywhere; no uppercase tracking, no labels above headings.

## Components

- **LocationSign**: ink panel, 12 dp radius. Status: state word, preset name,
  position, then Start/Stop as a full-width yellow button and one line that says
  what the button will do or why it is disabled. Move: From, a yellow down arrow, To.
- **SignalBoard / SignalLine**: one bordered board, one 60 dp line per signal:
  pictogram, label, detail in figures, state word on the right. Live lines are
  yellow; On Start lines are plain; Off lines are muted. Wi-Fi and cells read
  "Hook path" instead of Live when Latebris cannot see the LSPosed module.
- **RequirementLine**: icon (met, missing, optional), label, detail, value.
- **Notice**: a tinted box with a leading icon (info or problem). No side bars.
- **LivePlate**: a small yellow plate with the GPS pictogram and "Live".
- **Pictograms**: GPS crosshair, Wi-Fi fan, cell mast, sign arrow, stop square;
  drawn on a 24-unit grid with a 2.4-unit stroke to sit beside Material's filled
  icons (`Pictograms.kt`).
- Shapes: 4 / 6 / 8 / 12 / 16 dp. Plates, not pills.

## Layout

- Scaffold with a top app bar (name, Live plate when running) and a bottom
  navigation bar: Status, Move, Presets, Options.
- 16 dp side margins; 28 dp above a section heading, 8 dp below it.
- Touch targets at least 48 dp; whole rows toggle switches and select radios.
- The FAB (New preset) appears only on Presets and only while nothing runs.

## Copy

Controls name their action ("Start movement", "Grant permissions"). Hints say
what happens or why something is blocked, in the app's own terms. The interface
claims nothing the verification in `docs/VERIFICATION.md` does not back: GPS
alone on the stock path, all three signals only for apps in the LSPosed scope.
