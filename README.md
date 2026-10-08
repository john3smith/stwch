# stwch

동글동글한 피치·민트 디자인의 Android 스톱워치. Kotlin 네이티브 UI, 로컬 저장,
백그라운드 알림, 세 가지 앱 단축키를 제공합니다. 서버·계정·인터넷 권한이 없습니다.

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
  일시정지 상태에서는 일반 알림으로 남고 초기화하면 알림이 사라집니다.

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

- [삼성 측면 버튼 안내](https://www.samsung.com/sg/support/mobile-devices/how-to-customise-the-side-button-with-new-features-on-your-galaxy-phone-and-tablet/)
- [Android 앱 단축키](https://developer.android.com/develop/ui/compose/system/shortcuts/creating-shortcuts)

## 나우바 / Live Updates

AndroidX Core 1.17.0의 `setRequestPromotedOngoing(true)`와 시스템 Chronometer를
사용합니다. Manifest에 `POST_PROMOTED_NOTIFICATIONS`를 선언하고, 실행 중에는
표준 ongoing 알림을 사용합니다. 커스텀 RemoteViews, 가짜 미디어 세션,
비공개 삼성 API는 사용하지 않습니다. Android 16(API 36) 이상 지원 기기는
앱의 `버튼·나우바 설정 → 실시간 알림 설정`에서 표시 허용을 확인할 수 있습니다.

1.0.2의 **나우바 진단** 버튼은 기기 모델/Android 버전, 알림 허용, 채널 상태,
진행 상태를 표시합니다. API 36 이상에서는 실제 게시된 알림을 읽어
`canPostPromotedNotifications()`, `hasPromotableCharacteristics()`,
`FLAG_PROMOTED_ONGOING`을 구분해서 표시합니다. 시스템 승격 여부가 나우바
화면 표시를 증명하는 것은 아닙니다. API 35 이하에서는 표준 API 미지원 이유를 표시합니다.
기존 버전도 표준 승격 요청을 하고 있었으므로, 이번 변경만으로 나우바 미표시가
해결됐다고 단정하지 않습니다. Galaxy 모델/One UI 버전과 실제 진단 결과가 필요합니다.

갤럭시의 `잠금화면 및 AOD → Now bar` 지원 앱 목록에 stwch가 나타나면 활성화하세요.
실제 나우바 채택 여부는 OEM의 추가 조건, 소프트웨어/기종과 사용자 설정에
의존하며 앱만으로 강제할 수 없습니다. 일시정지는 진행 중 Live Update가 아니라
일반 상태 알림으로 전환됩니다. Android 8~15에서는 일반 상태 알림을 제공합니다.
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
시작 명령에서만 실행하고, 일시정지/초기화 시 종료합니다.

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

배포 APK: `releases/stwch-v1.0.2-debug.apk`. 개인 테스트용 debug 서명이며
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
