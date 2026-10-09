# stwch 1.0.10 루틴 메인 화면 연결

2026-10-09. 일반 API 35 에뮬레이터에서 루틴 플래그 재현, Activity/Task 재사용,
최근 앱 유지, 명령·측정시간 보호를 확인했습니다. JUnit 43개/린트/빌드 성공.
실제 삼성 모드 및 루틴 검증 결과가 아닙니다.
[상세 원인·수정·검증·제한](docs/routine-foreground-2026-10-09.md)을 참고하세요.
아래 항목은 해당 구버전의 당시 검증 기록이며 최신 동작 설명이 아닙니다.

---

# stwch 1.0.3 진행 중 검증 / 배포 보류

검증일: 2026-10-08. versionCode 4. 일반 Android Emulator API 35만 사용.

- Gradle `testDebugUnitTest lintDebug assembleDebug` 성공, JUnit 24개 통과.
- Lint 오류 0개/기존 안내 경고 2개/hint 1개. Crash buffer에 stwch 오류 없음.
- 실제 알림: 제목 `스톱워치 일시정지`, 본문/BigText `일시정지 · 00:02`.
  3초 후에도 00:02 유지, showChronometer=false, 승격 요청 false 확인.
- API 36 이상 실행 중에만 ProgressStyle(indeterminate)을 적용. 임의 완료 퍼센트 없음.
  API 36 런타임/실제 Samsung 나우바 표시는 검증하지 못함.
- 마지막 유효 단축키 action/수신 시각/flags 한 건의 로컬 진단 저장 추가.
- `NEW_TASK | CLEAR_TASK | TASK_ON_HOME`(0x1000c000)으로 Settings에서 호출하면
  현재 1.0.2와 수정 중 코드가 모두 NexusLauncher/Home으로 이동하는 것을 재현.
  이전 검증은 TASK_ON_HOME 옵션이 없어서 이 경우를 놓쳤음.
- NoDisplay + onCreate 즉시 처리/finish 실험: FGS 시작은 허용됐지만 Home 이동 유지.
- singleTask에서 HOME 옵션을 제거한 자기 intent로 재호출하는 실험도 Home 이동 유지.
  실패한 두 실험은 최종 코드에서 모두 되돌렸으며 홈 이동을 해결했다고 보고하지 않음.
- HOME 옵션 없는 호출은 기존 Settings 화면 유지.
- 현재 Galaxy의 실제 호출 flags는 확인하지 못했으므로 Samsung 원인 확정이 아님.
  모델/Android/One UI 버전과 루틴 설정 확인이 필요함.
- 로컬 APK `releases/stwch-v1.0.3_미완성-debug.apk`, 2,433,493 bytes,
  SHA-256 `14bd6aa9e2e723e9667eca2121acbb7c1cd3ef4e6937fcee3410adaca1843d0b`.
  apksigner v2 서명 검증 통과, 데이터 보존 설치 성공.
- 홈 이동 미해결과 Samsung 검증 부재로 완료 APK Release/Telegram 배포 보류.

---

# stwch 1.0.2 이전 검증

검증일: 2026-10-08. 패키지 `com.local.stwch`, versionCode 3.

- `testDebugUnitTest lintDebug assembleDebug` 성공. JUnit 24개, 실패/오류 0개.
- Lint 오류 0개, 기존 버전 안내 경고 2개와 KTX hint 1개.
- Windows 실행 정책으로 `build.ps1` 직접 실행은 제한됨. 정책을 영구 변경하지 않고
  같은 Gradle 검증 후 새 이름의 APK를 복사해 패키징함.
- `releases/stwch-v1.0.2-debug.apk`, 2,430,061 bytes.
- SHA-256 `f81b6f5fddf1dda41ca9d352e1c13d9632e58648d39c2ca87f28ec7667eb97b9`.
- apksigner v2 서명 검증 통과. aapt min 26/target 36/version 1.0.2 확인.
- 일반 Android Emulator `emulator-5554`, Android 15/API 35, `install -r` 성공.

## 변경 경로 수동 검증

- Settings Activity/task 211에서 START/PAUSE/중복 PAUSE/CLEAR_TASK형 START를
  실행해도 동일 Settings Activity 유지. 프로세스 강제 종료 후 cold shortcut START도
  같은 화면 유지 및 `isForeground=true` 확인.
- Home에서 명령 호출 후 동일 NexusLauncher/task 192 유지.
- HistoryActivity/task 243에서 launcher형 PAUSE 실행 후 동일 기록 화면 유지.
- START 연속 6회: RUNNING, 누적시간 0, history 20개 유지, 명령 Activity 잔존 없음.
- 루틴 중복 PAUSE: IDLE/0ms/중간기록 0, 기존 완료 기록 보존.
- 실제 알림창의 `이어서 시작` 버튼 터치: PAUSED → RUNNING, 1,149ms 누적시간
  유지, drawer를 닫으면 이전 Settings Activity 유지. 버튼 PendingIntent는
  Activity가 아닌 Foreground Service로 전달됨.
- 실행 중 알림 본문 `현재 시간 00:01 → 현재 시간 00:03` 갱신 실측.
  제목/본문에 중간기록 문구 없음. 시스템 Chronometer와 승격 요청 extra true 확인.
- 화면 OFF 3초 후 복귀, 서비스/측정 유지. 절전 지연 시 콜백 tick을 세지 않음.
- 나우바 진단 화면 API 35 미지원 안내, 실제 모델/버전/알림·채널 상태 표시 확인.
- crash buffer에 stwch crash 없음.

## 이번 수정의 한계

1.0.1의 `singleInstance` + `finishAndRemoveTask`는 OEM이 명령 task 제거 후 Home을
선택할 가능성이 있는 경로였음. 이번에는 affinity 없는 standard/noHistory 명령
Activity가 자기 Activity만 finish하도록 수정함. 에뮬레이터의 호출 화면 유지 검증은
통과했지만, 사용자가 보고한 Galaxy Routine+ 환경을 직접 재현하지 못했으므로
삼성 실기기의 원인 확정/화면 유지 해결을 보장하지 않음. 단축키 ID와 클래스는 유지함.

기존 코드도 이미 표준 Live Updates 요청 extra를 설정하고 있었음. 따라서 나우바
문제가 이번 버전만으로 해결됐다는 증거는 없음. API 36 진단 분기는 SDK 36 빌드로
검증했으나 API 35 에뮬레이터에서 실행할 수 없으며, Samsung Now bar 자체도 없음.
실제 Galaxy 모델/Android/One UI 버전, 표시 허용 설정과 새 진단 결과가 필요함.
비공개 삼성 API, 가짜 미디어 세션이나 임의의 시스템/개발자 설정 변경은 사용하지 않음.

---

# stwch 1.0.1 이전 검증

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
