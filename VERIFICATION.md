# stwch 1.0.0 검증

검증일: 2026-10-08. 패키지: `com.local.stwch`, versionCode 1.

## 빌드와 APK

- `./gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain`: 성공.
- `./build.ps1`: 같은 검증을 실행하고 버전 APK 복사까지 성공.
- JUnit: 16 tests, failures 0, errors 0, skipped 0.
- Lint: errors 0, warnings 2, hint 1. 경고는 Gradle/Core의 새 버전 안내이며,
  hint는 사용하지 않는 KTX 확장 제안입니다. 검증된 현재 버전을 유지했습니다.
- 호환 단축키 선택기를 위한 구형 CREATE_SHORTCUT 결과 상수에는 의도된 deprecated
  컴파일 경고가 있습니다. 현대 앱 단축키는 별도 정적 shortcuts.xml로 제공합니다.
- APK: `releases/stwch-v1.0.0-debug.apk`, 2,493,457 bytes.
- SHA-256: `6af6b87f287654081e2f646cd1de7b4bb4af4912d21a459da451912ce5c0a4af`.
- aapt: minSdk 26 / targetSdk 36 / versionName 1.0.0 / versionCode 1 확인.
- apksigner verify: 성공, 서명자 1명, APK Signature Scheme v2.
- 운영 배포 키를 생성하거나 저장소에 포함하지 않았습니다. 개인 테스트용 debug 서명입니다.

## JVM 테스트 범위

시작·정지, 실행 중 중복 START 초기화/이전 구간 기록, PAUSED START 이어 시작,
중앙 TOGGLE의 실행 중 정지 동작, 중복 PAUSE 무시, 실행 중 RESET 저장,
일시정지 RESET 중복 기록 방지, 중간기록 10개 제한/구간 계산,
정지 중 중간기록 방지, 최근 기록 20개/최신순, wall clock 변경 독립성,
같은 부팅 프로세스 복원, 재부팅 및 역행 monotonic 복원, 시간 포맷을 검증했습니다.

## 에뮬레이터 수동 검증

대상: 일반 Android Emulator `YTDown_API_35`, `emulator-5554`, Android 15/API 35.
ARTEMIS/실제 휴대폰은 사용하지 않았습니다. APK는 `adb install -r`로 설치했으며
앱/에뮬레이터 데이터 전체 초기화는 하지 않았습니다. 실제 화면의 UI 트리에서
resource-id/text와 bounds를 확인한 후 터치했습니다.

| 항목 | 결과 |
|---|---|
| 설치·시작·알림 권한 | 최종 APK 설치 성공, version 1.0.0, 권한 허용 확인 |
| 중앙 버튼 | 시작 → 일시정지 → 이어 시작 확인 |
| START 단축키를 실행 중 다시 호출 | runAt 갱신, 누적 0부터 새 측정, 이전 기록 reason=restart |
| 일시정지 후 START | accumulated 유지하며 RUNNING으로 복귀 |
| PAUSE 연속 호출 | 같은 기록 중복 추가 없음 |
| 중간기록 제한 | 화면에서 11회 터치 후 정확히 10개(1~10), 기존 기록 유지 |
| RESET | IDLE/0ms/현재 중간기록 0개, 과거 기록 유지 |
| 백그라운드·화면 OFF | Home 후 FGS 유지, 화면 Asleep 5초 동안 유지, 복귀 후 경과시간에 포함 |
| 알림 | ongoing/stopwatch category, showChronometer=true, requestPromotedOngoing=true, 제어 2개 확인 |
| 알림에서 이어서 시작 | 실제 알림 버튼 터치로 RUNNING, 누적시간/10개 중간기록 유지 |
| 알림에서 초기화 | 실제 알림 버튼 터치로 IDLE/0ms/중간기록 제거/최근 기록 유지 |
| 프로세스 종료·재실행 | 이 앱만 force-stop 후 다시 열어 runAt 유지, 종료 기간 포함, 중간기록 유지 |
| 세 단축키 등록 | dumpsys shortcut에서 stopwatch_start/pause/reset 모두 확인 |
| 호환 바로가기 선택창 | CREATE_SHORTCUT 실행 시 시작/일시정지/초기화 3개 표시 |
| 기록 화면 | 시작/중지 시각, 구간·누적시간, 상태와 최신순 카드 확인 |
| 화면 회전 | 가로 전환 시 중앙 버튼 유지, PAUSED 상태 유지. 에뮬레이터 원래 회전 설정 복원 |
| 최종 APK 재설치 | 저장 상태 유지, 최종 UI 시작/중간기록 3개/일시정지 재확인 |
| 오류 확인 | Android crash buffer에 com.local.stwch 관련 항목 없음 |

스크린샷은 앱이 열린 에뮬레이터 창을 앞으로 가져온 후 ADB로 새로 캡처하고
직접 확인했습니다. 검증 중 생성한 기록은 테스트용 에뮬레이터 내부에만 남으며
APK·소스에는 포함되지 않습니다.

## 확인하지 못한 부분 / 한계

- 에뮬레이터에 Samsung One UI/Now bar가 없습니다. 측면 버튼의 기능별 선택 목록과
  실제 Galaxy Now bar 표시는 실기기 검증 전이며 보장하지 않습니다. 정적 바로가기와
  Android 16 Live Updates 요청을 구현했습니다. 지원 여부는 OEM/기종/설정에 따릅니다.
- API 26/36 실제 실행은 미검증입니다. API 35에서 실행했고 lint로 최소 API 호출을 검사했습니다.
- 실제 기기 재부팅/제조사 절전은 실행하지 않았습니다. 재부팅 복원은 JVM 모델로 검증했습니다.
- 저장공간 부족 IO 오류는 안내하고 메모리 측정을 계속하도록 처리했으나,
  디스크를 실제로 가득 채우는 장애 주입은 하지 않았습니다.
- 강제 종료는 시스템 알림을 제거합니다. 다시 앱을 열어 같은 부팅의 측정을 복원합니다.
- 기기 재부팅 시 마지막 저장 checkpoint로 일시정지하며 미저장 구간은 추정하지 않습니다.
- 첫 GitHub 저장소 생성·푸시·Release 배포는 정확한 저장소 승인 후 진행합니다.
