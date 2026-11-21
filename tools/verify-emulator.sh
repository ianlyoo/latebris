#!/usr/bin/env bash
# Runs the Latebris verification matrix on one emulator and saves everything it saw.
#
#   tools/verify-emulator.sh <out-dir>
#
# Before running (see docs/VERIFICATION.md):
#   - ANDROID_SERIAL names the emulator; adb is on PATH.
#   - Latebris and both probe flavors are installed:
#       ./gradlew :app:assembleDebug :probe:assembleDebug
#       adb install -r -g app/build/outputs/apk/debug/app-debug.apk
#       adb install -r -g probe/build/outputs/apk/scoped/debug/probe-scoped-debug.apk
#       adb install -r -g probe/build/outputs/apk/unscoped/debug/probe-unscoped-debug.apk
#       adb shell appops set io.github.ianlyoo.latebris android:mock_location allow
#   - Hook path only: the Latebris module is enabled in LSPosed with only the
#     scoped probe in its scope, and the emulator was rebooted afterwards.
#     Run `adb root` first so the script can read the Magisk and LSPosed records.
#   - The emulator shows the Latebris home screen with a preset selected.
#
# Steps: simulation off -> on -> after Stop -> on again -> after force-stop of
# Latebris -> after reopening Latebris. At each step both probes run once; their JSON lines, a screenshot and
# a summary table go to <out-dir>. One screen recording covers a full probe run.
set -euo pipefail

OUT="${1:?usage: tools/verify-emulator.sh <out-dir>}"
mkdir -p "$OUT"
APP=io.github.ianlyoo.latebris
PROBE_ACTIVITY=io.github.ianlyoo.latebris.probe.ProbeActivity
REAL_LON=2.2945
REAL_LAT=48.8584

log() { printf '%s %s\n' "$(date +%H:%M:%S)" "$*" | tee -a "$OUT/run.log"; }

# Center "x y" of the first on-screen element whose text or content description is
# exactly $1, or nothing.
center_of() {
  adb shell uiautomator dump /sdcard/latebris-ui.xml >/dev/null
  adb exec-out cat /sdcard/latebris-ui.xml | tr '>' '\n' | grep -F -e "text=\"$1\"" -e "content-desc=\"$1\"" | head -1 |
    sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' |
    awk '{ if (NF == 4) print int(($1 + $3) / 2), int(($2 + $4) / 2) }'
}

# Tap the element whose text is exactly $1, scrolling down until it shows.
tap_text() {
  local xy
  for _ in 1 2 3 4 5 6; do
    xy=$(center_of "$1") || true
    if [ -n "$xy" ]; then
      adb shell input tap $xy
      return 0
    fi
    adb shell input swipe 540 1900 540 900 300
    sleep 1
  done
  log "not on screen: $1"
  return 1
}

# Bring Latebris to the front on its Status view, where Start and Stop live.
open_latebris_status() {
  adb shell am start -W -n "$APP/.ui.MainActivity" >/dev/null
  sleep 2
  tap_text Menu
  sleep 1
  tap_text Status
  sleep 1
}

# Run one probe flavor in a fresh process and keep its JSON lines and screen.
probe() {
  local step="$1" flavor="$2" pkg="io.github.ianlyoo.latebris.probe.$2"
  adb shell pm path "$pkg" >/dev/null 2>&1 || return 0
  adb shell am force-stop "$pkg"
  adb logcat -c
  adb shell am start -W -n "$pkg/$PROBE_ACTIVITY" >/dev/null
  for _ in $(seq 1 40); do
    adb logcat -d -s LatebrisProbe:I | grep -q '"tm.requestCellInfoUpdate"' && break
    sleep 1
  done
  adb logcat -d -s LatebrisProbe:I | grep -o '{.*}' >"$OUT/$step-$flavor.jsonl"
  {
    echo "== $step $flavor"
    adb logcat -d | grep -a -E 'Latebris(Module|[A-Za-z]*Hook)|LSPosed' || true
  } >>"$OUT/hook-logcat.txt"
  sleep 1
  adb exec-out screencap -p >"$OUT/$step-$flavor.png"
  log "probe $step $flavor: $(wc -l <"$OUT/$step-$flavor.jsonl" | tr -d ' ') lines"
}

probes() {
  probe "$1" scoped
  probe "$1" unscoped
}

log "device $(adb shell getprop ro.build.fingerprint | tr -d '\r')"
{
  echo "fingerprint: $(adb shell getprop ro.build.fingerprint | tr -d '\r')"
  echo "android: $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
  for pkg in "$APP" "$APP.probe.scoped" "$APP.probe.unscoped"; do
    path=$(adb shell pm path "$pkg" 2>/dev/null | head -1 | sed 's/^package://' | tr -d '\r') || true
    [ -n "$path" ] || continue
    echo "$pkg $(adb shell dumpsys package "$pkg" | grep -m1 versionName | tr -d ' \r') sha256=$(adb shell sha256sum "$path" | cut -d' ' -f1)"
  done
  if adb shell 'command -v magisk' >/dev/null 2>&1; then
    echo "magisk: $(adb shell 'magisk -v' 2>/dev/null | tr -d '\r')"
    echo "zygisk module: $(adb shell 'cat /data/adb/modules/*/module.prop' 2>/dev/null | grep -E '^(id|version)=' | tr '\n' ' ')"
    echo "lsposed modules (mid|package|enabled):"
    adb shell "sqlite3 /data/adb/lspd/config/modules_config.db 'select mid,module_pkg_name,enabled from modules'" 2>/dev/null | tr -d '\r'
    echo "lsposed scope (mid|package|user):"
    adb shell "sqlite3 /data/adb/lspd/config/modules_config.db 'select * from scope'" 2>/dev/null | tr -d '\r'
  fi
} >"$OUT/device.txt"

adb emu geo fix "$REAL_LON" "$REAL_LAT" 35 >/dev/null

log "step 1: simulation off"
probes 01-off

log "step 2: start the simulation"
open_latebris_status
tap_text Start
sleep 4
adb exec-out screencap -p >"$OUT/02-latebris-running.png"
adb shell screenrecord --time-limit 40 /sdcard/latebris-probe.mp4 &
RECORDER=$!
sleep 1
probes 02-on
wait "$RECORDER" || true
adb pull /sdcard/latebris-probe.mp4 "$OUT/02-on-probe-run.mp4" >/dev/null

log "step 3: stop the simulation"
open_latebris_status
tap_text Stop
sleep 3
probes 03-after-stop

log "step 4: start again, then force-stop Latebris"
open_latebris_status
tap_text Start
sleep 4
probes 04-on-again
adb shell am force-stop "$APP"
log "force-stopped $APP; waiting 12 s for the heartbeat to expire"
sleep 12
probes 05-after-force-stop

log "step 6: reopen Latebris (it clears providers a dead process left behind)"
adb shell am start -W -n "$APP/.ui.MainActivity" >/dev/null
sleep 4
adb shell dumpsys location | grep -E '^    (gps|network) provider' >"$OUT/06-location-providers.txt" || true
probes 06-after-reopen

python3 -I - "$OUT" <<'PY' | tee "$OUT/summary.md"
import json, pathlib, sys
out = pathlib.Path(sys.argv[1])
print("| step | probe | GPS (lm.gps.current) | Wi-Fi scan | cell (getAllCellInfo, first) | 1000 getLatitude() |")
print("|---|---|---|---|---|---|")
for f in sorted(out.glob("*.jsonl")):
    step, flavor = f.stem.rsplit("-", 1)
    rows = {}
    for line in f.read_text().splitlines():
        try:
            d = json.loads(line)
        except ValueError:
            continue
        rows[d.get("source")] = d
    g = rows.get("lm.gps.current", {})
    gps = f"{g['lat']:.4f}, {g['lon']:.4f} acc {g['acc']:.2f} mock={str(g['mock']).lower()}" if "lat" in g else "none"
    w = rows.get("wm.scanResults", {})
    wifi = ", ".join(f"{n['ssid']} {n['bssid']}" for n in w.get("networks", [])) or "none"
    c = (rows.get("tm.allCellInfo", {}).get("cells") or [{}])[0]
    cell = f"{c.get('type')} {c.get('mcc')}/{c.get('mnc')} ci {c.get('ci')} pci {c.get('pci')} tac {c.get('tac')} earfcn {c.get('earfcn')}" if c else "none"
    cost = rows.get("getter.cost", {}).get("ms")
    print(f"| {step} | {flavor} | {gps} | {wifi} | {cell} | {cost:.1f} ms |" if cost is not None else f"| {step} | {flavor} | {gps} | {wifi} | {cell} | - |")
PY
log "done: $OUT"
