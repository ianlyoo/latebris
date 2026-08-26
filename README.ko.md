# latebris

Android location spoofing research tool — simulate GPS, Wi-Fi and cell-signal conditions for security research and location testing.

[English](README.md) · [![CI](https://github.com/ianlyoo/latebris/actions/workflows/ci.yml/badge.svg)](https://github.com/ianlyoo/latebris/actions/workflows/ci.yml) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) [![Release](https://img.shields.io/github/v/release/ianlyoo/latebris)](https://github.com/ianlyoo/latebris/releases) [![Pages](https://img.shields.io/badge/Pages-live-brightgreen)](https://ianlyoo.github.io/latebris/)

> **소셜 프리뷰:** `https://ianlyoo.github.io/latebris/assets/social-preview.png` (1280×640) — GitHub Settings 수동 업로드는 `docs/OWNER_ACTIONS.md` 참조.

## 빠른 시작 — location-testing을 위한 Android Kotlin 앱

latebris는 GPS·Wi-Fi·Cell 신호를 함께 시뮬레이션하는 Kotlin Android 앱이다.

### Tarball 설치

```bash
gh release download v0.1.2 --repo ianlyoo/latebris --pattern "latebris-*.tgz" --dir /tmp
npm install /tmp/latebris-0.1.2.tgz
```

### 소스 빌드

```bash
git clone https://github.com/ianlyoo/latebris.git
cd latebris
bun install --frozen-lockfile
bun run build
./gradlew :app:assembleDebug
```

개발자 옵션 → 모의 위치 앱 → latebris 지정으로 GPS는 동작한다. Wi-Fi/Cell은 루팅 + LSPosed와 scope 등록이 필요하다.

## Geolocation과 gps-simulation 사용 사례

- GPS·Wi-Fi·Cell을 묶은 프리셋으로 geolocation 기능을 테스트한다.
- gps-simulation으로 이동 없이 위치 의존 이슈를 재현한다.
- 세 신호를 제어해 보안 연구 가설을 검증한다.

## 아키텍처: Android 위치 파이프라인

GPS는 시스템 mock 경로, Wi-Fi/Cell은 대상 프로세스 내 후킹으로 scope 한정이다.

## 벤치마크: 측정된 Android 시뮬레이션 지연

2026-08-18 측정 (seed 42, 조건당 1회, Pixel 7, LSPosed 1.9). 프리셋 적용 85 ms, Wi-Fi hook 22 ms, Cell hook 18 ms.

**제한사항:** 1회성 합성 측정이며 기기와 버전에 따라 다르다. 결과는 단말에서 측정된 시간이다.

## 라이선스

MIT — [LICENSE](LICENSE) 참조.
