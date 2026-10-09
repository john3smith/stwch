# stwch

[처음 사용하는 분을 위한 설명서](USER_GUIDE.md) · [APK 다운로드·미완성 버전 확인](https://github.com/john3smith/stwch/releases)

> **1.0.9 멈춘 시간 표시 (2026-10-09):** 일시정지 나우바의 짧은 표시값, 제목,
> 본문을 `01:23`처럼 멈춘 측정시간으로 통일했습니다. `일시정지` 문구 대신 시간이
> 보이며 크로노미터는 계속 꺼져 있습니다. 1.0.8의 버튼 삭제 변경도 포함합니다.
> **1.0.8 화면 정리:** `나우바 진단`, `버튼·나우바 설정` 버튼을
> 제거했습니다. 스톱워치·일시정지 나우바 유지 기능과 별도의 ADB 활성화 안내는 유지합니다.
> **1.0.7 일시정지 나우바 유지:** 일시정지 때 ongoing·승격 요청과
> 포그라운드 알림을 유지합니다. 시간 갱신은 멈추고 `스톱워치 일시정지`와 멈춘 시간을
> 표시하며 초기화할 때 알림을 제거합니다. 실제 Samsung 표시 여부는 별도 검증이 필요합니다.
> **1.0.6 ADB 사용 안내:** 앱 메인화면에 나우바 ADB 활성화 안내를
> 추가했습니다. 기존 1.0.5 APK는 삼성 RTL Galaxy S25 FE / Android 16 / One UI 8.5에서
> 기기 전체 테스트 플래그를 활성화했을 때 실제 나우바가 표시됐습니다.
> 앱 자체가 해당 설정을 변경하지 않으며 모든 기기의 표시를 보장하지 않습니다.
> [실제 표시 검증 기록](docs/nowbar-confirmed-display-2026-10-09.md).

동글동글한 피치·민트 디자인의 Android 스톱워치. Kotlin 네이티브 UI, 로컬 저장,
백그라운드 알림, 세 가지 앱 단축키를 제공합니다. 서버·계정·인터넷 권한이 없습니다.

> **1.0.3 미완성 사전 릴리즈:** 사용자의 게시 요청에 따라 현재 테스트 APK를
> GitHub Pre-release로 제공합니다. 일시정지 알림 표시와 Android 16
> ProgressStyle은 구현했지만 `TASK_ON_HOME` 호출 시 홈 복귀 및 삼성 RTL
> 기기의 실제 Now bar 표시는 미해결입니다. 정식 안정판이나 해결 완료 버전이
> 아닙니다. APK 파일명의 `_미완성` 표시와 릴리즈의 알려진 제한을 확인하세요.

## 사용하기

- 중앙 원 터치: 시작 → 일시정지 → 이어서 시작.
- **루틴의 스톱워치 시작**: 준비 상태면 시작, 실행 중이면 현재 시간·중간기록을
  초기화하고 0부터 새로 측정합니다. 버리는 실행 구간을 중간기록이나 최근 기록에
  자동 추가하지 않습니다. 일시정지 상태면 누적시간과 중간기록을 유지하며 이어서 시작합니다.
- **루틴의 스톱워치 일시정지**: 실행 중이면 일시정지하고 완료한 구간을 기록합니다.
  일시정지 상태에서 다시 누르면 시간·중간기록을 초기화합니다. 준비 상태에서는 아무 변화가 없습니다.
- **스톱워치 초기화 단축키**: 시간과 현재 중간기록 초기화. 기존 최근 기록은 유지합니다.
- 중간기록: 실행 중 최대 10개. 총시간과 직전 중간기록 이후 구간시간 표시.
  10개 이후에는 기존 기록을 덮어쓰지 않습니다.
- 기록: 중지한 구간의 시작·중지 시각, 구간 측정시간과 누적시간을 최신순으로 20개.
  일시정지 또는 실행 중 초기화 시 이전 구간을 저장합니다. 루틴의 중복 시작으로
  버린 구간은 저장하지 않습니다. 이미 일시정지한 상태에서 초기화해도 동일 기록을 다시 저장하지 않습니다.
- 알림 권한을 허용하면 실행 중 시스템 Chronometer, `현재 시간 MM:SS`
  (1시간 이상은 `H:MM:SS`)과 일시정지·초기화 버튼이 표시됩니다. 알림에는
  중간기록 개수를 표시하지 않습니다. 알림 본문은 측정시간 기준 초 경계마다 갱신합니다.
  1.0.9부터 일시정지하면 제목·본문·나우바 짧은 표시값은 멈춘 측정시간으로 바뀌며
  Chronometer와 본문 갱신이 멈춥니다. 재개 버튼은 `이어서 시작`입니다.
  1.0.7부터 일시정지 상태에서도 ongoing 알림과 Live Update 승격 요청을 유지하고,
  초기화하면 알림이 사라집니다. 멈춘 시간을 표시하며 타이머 갱신은 하지 않습니다.

루틴 옵션과 앱/알림 버튼의 명령 경로를 분리했습니다. 중앙 원은 항상 시작/일시정지/
이어 시작이며, 알림의 일시정지를 중복 호출했다고 초기화하지는 않습니다.
기존에 완료해 저장된 최근 기록은 루틴 초기화/새 시작으로 삭제하지 않습니다.

| 루틴 호출 전 상태 | 시작 버튼 | 일시정지 버튼 |
|---|---|---|
| 준비 | 0부터 시작 | 변화 없음 |
| 실행 중 | 현재 시간·중간기록 초기화 후 새 시작 | 일시정지·구간 저장 |
| 일시정지 | 기존 시간·중간기록을 유지하고 이어 시작 | 시간·중간기록 초기화 |

## 갤럭시 버튼 매핑

`설정 → 유용한 기능 → 측면 버튼 → 두 번 누르기 → 앱 → stwch`에서 선택합니다.
앱은 일반 LAUNCHER 항목으로 등록돼 있고 `android.app.shortcuts`에 다음 3개
정적 단축키가 설치 시 등록됩니다.

1. 스톱워치 시작 (`stopwatch_start`)
2. 스톱워치 일시정지 (`stopwatch_pause`)
3. 스톱워치 초기화 (`stopwatch_reset`)

기능 단위 매핑은 삼성 설정/Good Lock이 해당 앱 단축키를 노출하는 경우 가능합니다.
일반 Android 앱이 삼성 설정 메뉴를 강제로 수정할 수는 없습니다. 기종·One UI에
따라 세부선택지를 제공하지 않으면 앱 아이콘 길게 누르기 또는 앱 바로가기 지원
버튼 도구를 사용하세요. `CREATE_SHORTCUT` 선택창도 제공합니다. 잠금 상태에서는
기기 보안 정책에 따라 잠금 해제가 필요할 수 있습니다. 접근성 권한이나 하드웨어
키 가로채기는 사용하지 않습니다. 갤럭시 실기기 메뉴는 별도 확인이 필요합니다.

루틴+ 등에서는 **앱 바로가기 → stwch → 시작/일시정지/초기화**를 선택해야 합니다.
일반 `앱 열기 → stwch`는 화면을 여는 동작으로, 위 세 명령과 다릅니다.
1.0.1의 `singleInstance`/전용 task 제거 방식을 폐기했습니다. 1.0.2에서는
affinity가 없는 `standard` 투명 Activity에서 명령을 처리한 후 **Activity만 `finish()`** 합니다.
`finishAndRemoveTask`, `finishAffinity`, `moveTaskToBack`이나 HOME intent를 사용하지
않으며 메인화면을 열지 않습니다. 서비스 시작은 Activity가 resume된 뒤 수행해
최신 Android의 사용자 시작 Foreground Service 정책을 지킵니다.
알림 명령 버튼은 `PendingIntent.getForegroundService`를 사용해 Activity를 전혀 열지 않습니다.
같은 단축키 ID와 클래스명을 유지하므로 저장된 매핑과 호환됩니다.
다만 삼성 루틴 UI가 내부적으로 수행하는 화면 전환까지 앱에서 제어할 수는 없습니다.

`NEW_TASK | CLEAR_TASK | TASK_ON_HOME`(`0x1000c000`) 호출은 1.0.2/현재 코드 모두
이전 Settings 화면에서 Home으로 이동했습니다. `TASK_ON_HOME`은 호출자가 Android에
홈 복귀를 요청하는 옵션입니다. NoDisplay/즉시 종료 또는 자신의 task를 재호출해서
옵션을 제거하는 실험도 이 이동을 해결하지 못해 최종 코드에 포함하지 않았습니다.
일반 `NEW_TASK | CLEAR_TASK`만 사용하는 경우 기존 화면은 유지됩니다. 이 재현이
실제 Galaxy의 호출 옵션을 확인한 것은 아닙니다. 마지막 유효 루틴 명령/시각/flags
한 건만 앱 내부에 기록합니다. 1.0.8부터 나우바 진단 화면은 제거되어 앱 UI에서
표시하지 않습니다. 호출자 앱 이름,
전체 URL, 화면 내용과 앱 사용 이력은 수집하지 않습니다.

- [Android TASK_ON_HOME 정의](https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_TASK_ON_HOME)

- [삼성 측면 버튼 안내](https://www.samsung.com/sg/support/mobile-devices/how-to-customise-the-side-button-with-new-features-on-your-galaxy-phone-and-tablet/)
- [Android 앱 단축키](https://developer.android.com/develop/ui/compose/system/shortcuts/creating-shortcuts)

## 나우바 / Live Updates

### 나우바 ADB 활성화 (Windows)

**APK 설치만으로 나우바가 나타나지 않는 Galaxy 개발자용 기기는 PC의 ADB 설정이
필요할 수 있습니다.** 검증한 방법은 삼성의 비공식 기기 전체 테스트 플래그이며,
stwch 패키지만 허용하는 설정이 아닙니다. 모든 One UI에서 동작을 보장하지 않으며
펌웨어 업데이트 후 유지 여부도 검증하지 않았습니다. 스톱워치 자체는 이 설정 없이도 사용 가능합니다.

1. Windows 10/11 PC에 휴대폰을 연결하고 개발자 옵션 → USB 디버깅을 켠 뒤
   휴대폰에서 연결을 허용합니다. USB 드라이버가 없으면 제조사 공식 드라이버를 설치해야 합니다.
2. [stwch-nowbar-adb-enable.bat](tools/stwch-nowbar-adb-enable.bat)를 내려받아 더블클릭합니다.
   EXE, Android Studio, 관리자 권한은 필요하지 않습니다. BAT 하나에 필요한 PowerShell 코드가 들어 있습니다.
3. BAT는 옆의 `platform-tools/adb.exe` 또는 `adb.exe`, `ANDROID_SDK_ROOT` /
   `ANDROID_HOME`, Android Studio 기본 SDK 경로, 이전에 다운로드한 캐시, PATH 순으로 ADB를 찾습니다.
   고정된 개발 PC 경로를 사용하지 않습니다. ADB가 없으면 공식 SDK 라이선스·다운로드 안내를
   표시하며 `y`로 동의한 경우에만 Google 공식 Platform-Tools를 다운로드합니다.
   저장 위치는 `%LOCALAPPDATA%/stwch/adb-cache/<고유폴더>/platform-tools`이고 기존 SDK를 덮어쓰지 않습니다.
   이미 ADB가 있으면 다운로드 없이 오프라인으로 사용할 수 있습니다.
4. **실제 휴대폰은 한 대만 연결**해야 합니다. 목록의 `emulator-*`, `localhost:*`,
   `127.0.0.1:*` 기기는 제외하고, 나머지 준비된 기기가 정확히 한 대일 때 해당 시리얼을
   명시하여 실행합니다. USB뿐 아니라 이미 연결된 무선 ADB도 지원합니다. 다른 실제 기기가
   여러 대이거나 승인 대기/연결 오류이면 설정을 변경하지 않고 중단합니다.
5. 표시된 **선택 기기와 원래 값**을 확인하고 원래 값을 기록해 둡니다. 기기 전체 비공식
   설정 변경에 `y`로 동의하면 활성화합니다. 이어서 표시되는 적용 값이 `1`이면
   설정이 저장된 것입니다. 이것만으로 실제 나우바 표시 성공을 의미하지는 않습니다.
6. stwch의 알림 권한을 허용하고 스톱워치를 시작합니다. 이미 실행 중이라면 앱에서
   **일시정지 → 다시 시작**합니다. 화면을 껐다 켜서 잠금화면의 나우바를 확인합니다.

ADB 경로만 안전하게 확인하려면 `stwch-nowbar-adb-enable.bat --check`를 실행합니다.
이 모드는 다운로드, 기기 목록 조회, 휴대폰 설정 변경을 하지 않습니다.
특정 ADB 실행 파일을 지정하려면 실행 전 `STWCH_ADB` 환경변수에 절대 경로를 넣습니다.

도구 로직 검증: `powershell -NoProfile -ExecutionPolicy Bypass -File tools/test-portable-nowbar-bat.ps1`.
PowerShell 구문, USB/무선 기기 선택, 에뮬레이터 제외, 복수·미승인 기기 거부, 취소,
읽기/쓰기 오류와 값 검증을 20개 검사로 확인합니다. 휴대폰 응답은 모의 처리하며 실제
설정 변경·다운로드는 하지 않습니다. 자동 다운로드/추출 경로는 다른 PC에서 실동작 검증하지 않았습니다.

BAT는 선택한 시리얼을 `-s`로 지정해 다음 설정만 활성화합니다. 루팅, 앱 데이터 삭제,
앱별 권한 강제 허용은 하지 않습니다. 아래 수동 예시는 실제 기기가 한 대일 때의 `-d` 방식입니다.

```bat
adb -d shell settings put secure enable_notification_nowbar_test 1
adb -d shell settings get secure enable_notification_nowbar_test
```

되돌릴 때는 **실행 전 값**을 기준으로 복원합니다. 원래 `null`이었다면 새로 만든 설정만 삭제합니다.

```bat
adb -d shell settings delete secure enable_notification_nowbar_test
```

원래 `0`이었다면 `adb -d shell settings put secure enable_notification_nowbar_test 0`을 사용합니다.
원래 다른 값이 있었다면 그 값으로 복원합니다. 기존 알림은 일시정지·재개하여 다시 게시합니다.
이 설정을 앱에서 자동으로 변경하지 않습니다. USB 디버깅은 작업 후 필요에 따라 꺼 주세요.

### 기존 검사와 공식 API

아래 내용은 ADB 테스트 플래그 적용 전인 2026-10-08의 검사입니다. 이후 실제 표시 결과는
[2026-10-09 검증 기록](docs/nowbar-confirmed-display-2026-10-09.md)을 참고하세요.

2026-10-08 삼성 Remote Test Lab의 SM-S731N(Android 16 / One UI 8.5)에서
실제 설치·실행하여 확인했습니다. 알림 권한, 잠금화면 표시, 승격 가능 조건 및
실제 `FLAG_PROMOTED_ONGOING`은 모두 허용/참이었지만 잠금화면 Now bar에
stwch는 나타나지 않았습니다. **시스템 승격 성공을 Now bar 표시 성공으로
간주하지 않습니다.** 자세한 실기기 관찰과 실패한 실험은
[Samsung RTL 검증 기록](docs/samsung-rtl-2026-10-08.md)에 정리했습니다.

AndroidX Core 1.17.0의 `setRequestPromotedOngoing(true)`와 시스템 Chronometer를
사용합니다. Manifest에 `POST_PROMOTED_NOTIFICATIONS`를 선언하고, 실행 중에는
표준 ongoing 알림을 사용합니다. 커스텀 RemoteViews, 가짜 미디어 세션,
비공개 삼성 API는 사용하지 않습니다. Android 16(API 36) 이상 지원 기기는
기기의 stwch 알림 설정에서 실시간 알림 표시 허용을 확인할 수 있습니다.

진행 중 1.0.3에서는 API 36 이상 실행 중 알림에 공개 `ProgressStyle`의
indeterminate 표현을 사용합니다. 종료 시점이 정해지지 않은 스톱워치에 임의의
완료 퍼센트를 만들어 넣지 않습니다. API 35 이하와 일시정지는 `BigTextStyle`을
사용합니다. 공개 API 컴파일과 API 35 실행만 검증했으며, 실제 One UI 호환 문제를
해결했다는 증거는 아닙니다. 기기 버전 및 표시 허용 상태 확인이 필요합니다.

다음 내용은 제거 이전 버전의 진단 기능 설명입니다. **1.0.8부터 진단 버튼과
버튼·나우바 설정 버튼은 없습니다.**

1.0.2의 **나우바 진단** 버튼은 기기 모델/Android 버전, 알림 허용, 채널 상태,
진행 상태를 표시합니다. API 36 이상에서는 실제 게시된 알림을 읽어
`canPostPromotedNotifications()`, `hasPromotableCharacteristics()`,
`FLAG_PROMOTED_ONGOING`을 구분해서 표시합니다. 시스템 승격 여부가 나우바
화면 표시를 증명하는 것은 아닙니다. API 35 이하에서는 표준 API 미지원 이유를 표시합니다.
기존 버전도 표준 승격 요청을 하고 있었으므로, 이번 변경만으로 나우바 미표시가
해결됐다고 단정하지 않습니다. Galaxy 모델/One UI 버전과 실제 진단 결과가 필요합니다.

갤럭시의 `잠금화면 및 AOD → Now bar` 지원 앱 목록에 stwch가 나타나면 활성화하세요.
실제 나우바 채택 여부는 OEM의 추가 조건, 소프트웨어/기종과 사용자 설정에
의존하며 앱만으로 강제할 수 없습니다. 1.0.7부터 일시정지도 이어서 실행할 수 있는
ongoing 세션으로 유지하고 Live Update 승격을 요청합니다. 초기화하면 세션을 종료합니다.
Android 8~15에서는 일반 상태 알림을 제공합니다.
일반 Android 에뮬레이터는 삼성 Now bar가 없으므로 표시 검증을 대신할 수 없습니다.

[Android Live Updates 조건](https://developer.android.com/develop/ui/views/notifications/live-update)

## 시간·저장·배터리

`SystemClock.elapsedRealtime()`로 시간량을 계산해 화면 OFF/절전 시간을 포함하고
휴대폰 시각 변경에 영향을 받지 않습니다. UI는 보이는 동안만 50ms 간격으로
렌더링합니다. 서비스는 실행 중에만 초 경계마다 알림 본문을 갱신합니다. 네트워크
폴링이나 WakeLock은 없으며 시스템 Chronometer도 알림 시간을 표시합니다.
절전 중 Handler가 지연돼도 tick을 세지 않고 단조 시계에서 계산하므로 측정은
틀어지지 않습니다. 화면을 켜면 즉시 알림 본문을 갱신합니다. 일시정지/초기화/서비스
종료 시 콜백을 제거합니다. `specialUse` Foreground Service는 사용자
명령으로 실행하며, 일시정지 중에는 정적인 알림과 함께 유지하고 초기화 후 종료합니다.
일시정지 중에는 초 단위 콜백을 예약하지 않습니다.

상태·중간기록·기록은 앱 내부 AtomicFile로 원자 저장합니다. 같은 부팅에서 앱
프로세스가 다시 생성돼도 경과시간을 복원합니다. 기기 재부팅 시 monotonic 기준이
바뀌므로 마지막 저장 시점의 누적시간으로 일시정지 복원하며, 재부팅 전 미저장
구간은 추정하지 않습니다. 자동 부팅 실행은 하지 않습니다. 강제 종료/제조사
절전 정책은 상태 알림을 없앨 수 있으며, 다시 앱을 열면 저장 상태를 복원합니다.
앱 제거/데이터 삭제는 기록을 삭제합니다. 서버 전송이나 자동 백업은 없습니다.
저장공간 부족 등 파일 저장 오류는 안내하며 현재 측정은 메모리에서 계속됩니다.
저장 실패가 해결되기 전에 프로세스가 종료되면 미저장 기록은 복원되지 않습니다.

## 빌드

- 최소 Android 8/API 26, target/compile API 36.
- JDK 17, Android SDK 36, Gradle 8.13, AGP 8.13.2, Kotlin 2.2.10.
- Android Studio에서 이 디렉터리를 프로젝트로 여세요. 다른 PC에서는
  `local.properties`를 본인의 SDK 경로로 생성하세요(이 파일은 Git 제외).
- 런타임 외부 의존성은 AndroidX Core 1.17.0, 테스트는 JUnit 4.13.2입니다.

```powershell
./build.ps1
# 직접 실행:
./gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

기존 배포 APK: `stwch-v1.0.2-debug.apk`.
1.0.3 사전 릴리즈 APK: `stwch-v1.0.3-unfinished-debug.apk`.
배포 파일은 [GitHub Releases](https://github.com/john3smith/stwch/releases)에 있으며,
1.0.3은 알려진 미해결 문제가 있는 테스트용입니다. 로컬 빌드의 `_미완성` 파일명과
GitHub 자산의 `unfinished` 파일명은 같은 미완성 상태를 나타냅니다.
개인 테스트용 debug 서명이며
공개 스토어 배포 전에는 별도 보호된 운영키와 foreground-service 정책 검토가 필요합니다.
API 36 빌드는 나우바 지원 조건을 구현하지만 실제 갤럭시 표시를 보장하지 않습니다.

## 주요 코드와 검증

- `StopwatchEngine.kt`: 단조 시계 상태 머신, 10개 중간기록, 20개 구간 기록.
- `WatchStore.kt`: 앱 내부 원자 저장과 같은 부팅/재부팅 복원.
- `StopwatchService.kt`: Foreground Service와 상태 알림, Live Update 요청.
- `NotificationTime.kt` / `NotificationTimeTest.kt`: 알림의 초 단위 시간 표시와 갱신 경계.
- `ShortcutActivity.kt` / `res/xml/shortcuts.xml`: 버튼용 3개 단축키와 호환 선택창.
- `MainActivity.kt` / `Ui.kt`: 터치 원형 화면, 중간기록, 설정 안내.
- `HistoryActivity.kt`: 최신 20개 기록 조회.
- `StopwatchEngineTest.kt`: 시작/중복 시작/이어 시작, 일시정지·초기화,
  10/20 제한, 시간 변경, 프로세스/기기 재부팅 복원, 시간 포맷 단위 테스트.

실제 에뮬레이터 검증 결과와 제한사항은 `VERIFICATION.md`에 기록했습니다.
