# Latebris

안드로이드 위치 환경 시뮬레이션을 위한 내부 QA 도구입니다.

- GPS, Wi-Fi, Cell Tower 데이터를 프리셋 기반으로 시뮬레이션합니다.
- GPS는 mock location 기반으로 동작합니다.
- Wi-Fi / Cell 치환은 Xposed/LSPosed 후킹 경로를 사용합니다.

## Internal QA Only

- 이 도구는 사내 테스트 기기와 QA 환경을 전제로 하는 내부 테스트 전용 유틸리티입니다.
- 일반 사용자 배포 목적의 앱이 아닙니다.

## Full Test Requirements

Wi-Fi / Cell까지 포함한 전체 기능 테스트에는 아래 환경이 필요합니다.

- rooted Android device or rooted emulator
- Magisk
- LSPosed

## Important: Add The Target App To LSPosed Scope

외부 테스트 대상 앱에서 Wi-Fi / Cell 값이 바뀌게 하려면, LSPosed에서 해당 앱을 이 모듈의 scope에 추가해야 합니다.

- 모듈 활성화만으로는 충분하지 않습니다.
- 실제로 검증할 대상 앱을 scope에 추가해야 합니다.
- scope에 추가되지 않은 앱에서는 원래 Wi-Fi / Cell API 결과가 유지됩니다.

더 자세한 사용 순서는 `How_to_use.md`를 참고하면 됩니다.

처음 기기 세팅부터 보고 싶다면 `How_to_setup_stock_android.md`를 먼저 읽는 것을 권장합니다.
