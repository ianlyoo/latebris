# Latebris

Latebris는 저장해 둔 위치 환경을 안드로이드 기기에서 다시 재생하는 앱입니다.
위치 환경은 GPS 좌표, Wi-Fi 스캔에 잡히는 공유기 목록, 휴대폰이 보고하는 LTE
기지국 목록으로 이루어집니다. 위치에 따라 동작이 달라지는 기능을, 직접 이동하지
않고 본인 기기와 테스트 권한이 있는 앱에서 시험할 때 씁니다.

[English](README.md) · [릴리스](https://github.com/ianlyoo/latebris/releases) · [검증 기록](docs/VERIFICATION.md) · [한계](docs/LIMITATIONS.md) · MIT 라이선스

## 하는 일

프리셋 하나에 좌표, Wi-Fi 공유기 목록, LTE 기지국 목록이 함께 들어 있습니다.
Start를 누르면 Latebris가 그 프리셋을 아래 두 경로 중 하나로 내보냅니다.

| | 일반 경로 | 후킹 경로 |
|---|---|---|
| 기기 | Android 12 이상. 개발자 옵션에서 Latebris를 모의 위치 앱으로 지정 | Magisk, Zygisk, LSPosed로 루팅. Latebris 모듈을 켜고 테스트할 앱을 scope에 추가 |
| GPS | `LocationManager`와 fused location이 프리셋 좌표를 돌려주고, 모의 위치로 표시됨 | scope 안의 앱은 프리셋 좌표를 받고, 모의 위치로 표시되지 않음 |
| Wi-Fi | 실제 값 | scope 안의 앱: `WifiManager.getScanResults()`가 프리셋 공유기 목록을 돌려줌 |
| Cell | 실제 값 | scope 안의 앱: `getAllCellInfo()`와 `requestCellInfoUpdate()`가 프리셋 LTE 기지국을 돌려줌 |
| 검증한 환경 | Android 15 에뮬레이터 (API 35) | Android 14 에뮬레이터 (API 34), Magisk 26.4, LSPosed 1.9.2 |

LSPosed scope 밖의 앱에는 실제 Wi-Fi와 기지국 값이 그대로 보이고, GPS는 일반
경로와 같은 방식으로 보입니다.

후킹 경로에서 "Urban core" 프리셋을 켜 두고, 따로 만든 프로브 앱을 scope 안과 밖에
하나씩 두어 읽은 값입니다. 전체 표와 원본 파일은
[docs/VERIFICATION.md](docs/VERIFICATION.md)에 있습니다.

| 읽은 값 | scope 안 프로브 | scope 밖 프로브 |
|---|---|---|
| GPS (`getCurrentLocation`, fused) | 37.5665, 126.9780 · mock = false | 37.5665, 126.9780 · mock = true |
| Wi-Fi 스캔 | `qa-lab-a`, `qa-lab-b` (프리셋) | `AndroidWifi` (에뮬레이터 자체 공유기) |
| 서빙 기지국 | LTE 450/05, CI 22011, PCI 101, TAC 101 (프리셋) | LTE 310/260, CI 47108 (에뮬레이터 자체 기지국) |

Stop을 누르면 두 프로브 모두 실제 좌표(48.8584, 2.2945)와 에뮬레이터 자체 Wi-Fi,
기지국 값으로 돌아왔습니다.

앱에는 이런 기능도 있습니다.

- **Move**는 현재 위치에서 저장된 목적지까지 정한 속도로 이동합니다.
  OpenRouteService 키가 없으면 직선으로 움직이는데, 이 경우는 API 35 에뮬레이터에서
  확인했습니다. 시속 50 km 설정에서 18초 동안 246 m를 갔습니다(초속 13.7 m, 설정값은
  13.9 m). 키가 있으면 도로 경로를 요청하는데, 이 요청은 단위 테스트로만 확인했습니다.
- **Capture**는 기기의 현재 좌표, Wi-Fi 스캔, 기지국을 프리셋으로 저장합니다.
  에뮬레이터에서는 확인하지 않았습니다.
- **Auto Fill**은 OpenCellID에서 프리셋 좌표 주변의 기지국을 찾아 채웁니다. 단위
  테스트로만 확인했습니다.

## 설치

[릴리스](https://github.com/ianlyoo/latebris/releases)에서 `latebris-0.2.0.apk`를 받습니다.

**0.1.x 버전이 깔려 있다면 먼저 지워 주세요.** 0.2.0에서 패키지 이름이
`com.example.gpstick`에서 `io.github.ianlyoo.latebris`로 바뀌었습니다. 그래서 옛
버전을 지우지 않으면 두 앱이 나란히 설치된 채 같은 신호를 건드립니다.

일반 경로:

1. APK를 설치하고 위치와 알림 권한을 허용합니다.
2. 개발자 옵션 → 모의 위치 앱 선택 → Latebris를 고릅니다.
3. 프리셋을 고르고 Start를 누릅니다. Stop을 누르면 기기가 실제 위치로 돌아갑니다.

후킹 경로는 여기에 더해:

1. LSPosed에서 Latebris 모듈을 켜고, 테스트할 앱을 scope에 넣습니다.
2. 기기를 재부팅합니다. 저희 테스트에서 LSPosed 1.9.2는 scope를 바꾼 뒤 재부팅해야
   적용했습니다.
3. 모듈이 올라오면 Status 화면에 "LSPosed module: Active"가 보입니다.

경로 재생과 Auto Fill에는 본인의 OpenRouteService, OpenCellID 키가 필요합니다.
Options 화면에 입력하면 기기 안에만 저장됩니다.

## 소스에서 빌드

JDK 17과 Android SDK(플랫폼 35)가 필요합니다.

```bash
./gradlew assembleDebug testDebugUnitTest
# 계측 테스트는 숨은 API 객체를 리플렉션으로 만들기 때문에 먼저 아래를 켭니다.
adb shell settings put global hidden_api_policy 1
./gradlew connectedDebugAndroidTest
```

빌드할 때 키를 넣으려면 Gradle 속성이나 환경 변수 `OPEN_ROUTE_SERVICE_API_KEY`,
`OPEN_CELL_ID_API_KEY`를 씁니다. 릴리스 서명은 `ANDROID_SIGNING_STORE_FILE`,
`ANDROID_SIGNING_STORE_PASSWORD`, `ANDROID_SIGNING_KEY_ALIAS`,
`ANDROID_SIGNING_KEY_PASSWORD`를 읽습니다.

`probe` 모듈은 프로브 앱을 `scoped`, `unscoped` 두 가지로 빌드하고,
`tools/verify-emulator.sh`가 에뮬레이터에서 전체 확인을 한 번에 돌립니다. 자세한
방법은 [docs/VERIFICATION.md](docs/VERIFICATION.md)에 있습니다.

## 한계와 사용 범위

[docs/LIMITATIONS.md](docs/LIMITATIONS.md)에 다루지 않는 부분(5G NR 기지국, 연결된
Wi-Fi 정보, GNSS 위성 상태, 실제 휴대폰, 다른 Android와 LSPosed 버전)과 방어하는
쪽이 Latebris를 알아내는 방법을 적어 두었습니다. 줄이면 이렇습니다. 일반 경로는
모든 위치에 모의 위치 표시가 붙고, 후킹 경로는 루팅과 LSPosed가 필요한데 앱이
이 둘을 감지할 수 있습니다.

Latebris는 본인 기기와 테스트 권한이 있는 앱에서만 쓰세요.

## 라이선스

MIT. [LICENSE](LICENSE)를 보세요.
