# stwch 1.0.1 검증

검증일: 2026-10-08. 패키지 `com.local.stwch`, versionCode 2.

- `testDebugUnitTest lintDebug assembleDebug`와 `build.ps1` 성공.
- JUnit 22개 모두 통과: 기존 16개 + 루틴 전용 상태 전이 6개.
- Lint 오류 0개, 도구/라이브러리 버전 안내 경고 2개, KTX 제안 hint 1개.
- APK `releases/stwch-v1.0.1-debug.apk`, 2,496,289 bytes.
- SHA-256 `3dbc949105399836130042dc17821f3b64aa4acf29fc0cd6bfd25be183ab0e1d`.
- apksigner 검증 성공, v2 서명. 기존 debug 서명 그대로, 설치 데이터 보존.
- 일반 Android Emulator `emulator-5554`, Android 15/API 35에 최종 APK `install -r` 성공.

## 루틴 변경 및 실측 결과

| 항목 | 결과 |
|---|---|
| 실행 중 루틴 START 재호출 | runAt 갱신, 누적시간 0, 현재 중간기록 제거. 자동 최근 기록 추가 없음 |
| START 연속 6번 | RUNNING 유지, 최근 기록 수 증가 없음, 마지막 명령에서 새 측정 |
| 일시정지 후 루틴 START | 누적시간과 현재 중간기록 유지하며 재개 |
| 실행 중 루틴 PAUSE | 일시정지하고 완료 구간 1개 기록 |
| 일시정지 후 루틴 PAUSE | IDLE/0ms/현재 중간기록 0개, 과거 완료 기록 유지 |
| APP 출처의 중복 PAUSE | 일시정지 유지, 초기화/중복 기록 없음. 루틴과 분리됨 |
| 중앙 원 터치 | 기존 시작/일시정지/이어 시작 동작 유지 |
| 설정 화면에서 단축키 호출 | 같은 Settings Activity와 task 유지, stwch 메인화면으로 이동하지 않음 |
| 홈 화면에서 호출 | 동일 NexusLauncher Activity/task 유지 |
| stwch 기록 화면에서 호출 | 동일 HistoryActivity/task 유지. 메인화면으로 돌아가지 않음 |
| launcher형 NEW_TASK+CLEAR_TASK flags | 위 설정/홈 화면 유지, 완료 후 단축키 Activity 잔존 없음 |
| 백그라운드/화면 OFF | 서비스 유지, 화면 OFF 3초 이상이 측정시간에 포함됨 |
| 최종 버전/오류 확인 | 설치 versionName 1.0.1, crash buffer에 stwch 오류 없음 |

시간 검사에는 Android 서비스의 비동기 처리를 고려해 실제 저장 상태 변화를 확인했습니다.
처음 설치 직후의 고정 500ms 대기는 쓰기 완료 전 상태를 읽을 수 있어 판단 근거로
사용하지 않았으며, 재검사에서 명령 완료 상태와 기록 수를 확인했습니다.

## 화면 복귀 수정의 범위

기존 단축키 Activity는 standard 실행 모드여서 호출자의 실행 flags/스택 선택에
의존했습니다. 명령 전용 `singleInstance`/task affinity, 투명한 preview 비활성화,
처리 후 자기 task 종료를 적용했습니다. 새 intent는 별도로 받아 명령을 처리하며
메인화면을 여는 코드나 다른 앱의 task를 제거하는 코드는 없습니다.

기존 1.0.0의 화면 복귀 문제를 삼성 루틴+에서 직접 재현하지는 못했습니다.
일반 에뮬레이터의 단축키 호출/launcher flags를 기준으로 수정 후 화면 유지를
검증한 것이며, Samsung 앱이 먼저 일반 `앱 열기`를 실행하는 설정이면 해당 앱 열기
동작은 stwch 단축키에서 취소할 수 없습니다. 루틴의 `앱 바로가기`를 사용하세요.
실제 Galaxy 루틴+, 잠금 중 OEM 정책, Now bar는 실기기 검증 전입니다.

---

# stwch 1.0.0 이전 검증

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
