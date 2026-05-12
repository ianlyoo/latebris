# Latebris

안드로이드 위치 기반 보안 검증을 위해 만든 모의해킹/보안 연구용 도구입니다.

GPS 좌표뿐 아니라 Wi-Fi 및 Cell Tower 기반 위치 신호까지 앱 프로세스 관점에서 위조하거나 시뮬레이션해, 위치 신뢰 모델이 약한 Android 앱이 어떤 방식으로 우회될 수 있는지 실험하는 프로젝트입니다.

## What It Does

- GPS 위치는 Android mock location 경로를 사용해 프리셋 기반으로 시뮬레이션합니다.
- Wi-Fi / Cell Tower 값은 Xposed/LSPosed 후킹 경로를 통해 대상 앱 프로세스 안에서 치환합니다.
- 프리셋 기반으로 GPS, Wi-Fi, Cell 데이터를 함께 관리합니다.
- 대상 앱이 GPS만 보는지, 네트워크 위치 신호까지 검증하는지 확인하는 테스트 흐름을 제공합니다.

## Security Research Scope

이 프로젝트는 승인된 테스트 기기와 본인이 검증 권한을 가진 앱을 대상으로 한 보안 연구 및 모의해킹 실험을 전제로 합니다.

- 일반 사용자 배포 목적의 앱이 아닙니다.
- 타인의 서비스, 계정, 위치 기반 정책, 출석/인증/보상 시스템을 우회하기 위한 사용을 의도하지 않습니다.
- Wi-Fi / Cell 치환은 시스템 전체 값을 바꾸는 방식이 아니라, LSPosed에서 scope에 추가한 대상 앱 프로세스 안에서 API 결과를 바꾸는 방식입니다.

## Full Test Requirements

GPS만 테스트할 때와 Wi-Fi / Cell까지 포함한 전체 기능을 테스트할 때의 요구 조건이 다릅니다.

GPS mock location 테스트:

- Android device or emulator
- Developer options
- Mock location app 설정

Wi-Fi / Cell spoofing 테스트:

- rooted Android device or rooted emulator
- Magisk
- LSPosed

## Important: Add The Target App To LSPosed Scope

대상 앱에서 Wi-Fi / Cell 값이 바뀌게 하려면, LSPosed에서 해당 앱을 이 모듈의 scope에 추가해야 합니다.

- 모듈 활성화만으로는 충분하지 않습니다.
- 실제로 보안 검증할 대상 앱을 scope에 추가해야 합니다.
- scope에 추가되지 않은 앱에서는 원래 Wi-Fi / Cell API 결과가 유지됩니다.

더 자세한 사용 순서는 `How_to_use.md`를 참고하면 됩니다.

처음 기기 세팅부터 보고 싶다면 `How_to_setup_stock_android.md`를 먼저 읽는 것을 권장합니다.
