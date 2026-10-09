# Paused stopwatch retains its Live Update request

Version 1.0.7 changes pause from "end the foreground notification" to "freeze an
ongoing user-initiated measurement session". Only resetting ends the session.

The former code set ongoing and requestPromotedOngoing to false on pause, detached
the notification, and stopped the service. These actions intentionally removed
the public Android qualification conditions needed to remain a Live Update.

The updated notification keeps ID 41, ongoing=true, promotion requested=true,
title `스톱워치 일시정지`, body `일시정지 · MM:SS`, short critical text `일시정지`,
and usesChronometer=false while paused. Its static BigTextStyle is a supported
Live Update style; it does not show a fake advancing clock or progress percentage.
The foreground service remains with no recurring timer callback and no wake lock.
Reopening the app can restore both running and paused foreground sessions.
Diagnostics also read an active paused notification rather than reporting it absent.

Pure NotificationSession tests cover running/paused/idle, frozen time, repeated
APP pause, resume, reset, and the existing ROUTINE second-pause reset semantics.
This change does not override Samsung's display policy, alter the device-wide
ADB setting, or change the unrelated TASK_ON_HOME routine limitation.

Official qualification requirements:
https://developer.android.com/develop/ui/views/notifications/live-update

Samsung Now Bar on an Android 16/One UI phone still requires actual screen
verification; Android promotion flags alone are not evidence of its display.

## Verification results

- testDebugUnitTest: 36 tests passed (29 existing + 7 session-policy tests), no failures/errors.
- lintDebug and assembleDebug succeeded.
- Standard emulator YTDown_API_35 / emulator-5554: cold restart recovered its
  previously missing Android services without wiping data. Installed 1.0.7 with
  adb install -r and confirmed versionCode 8 / versionName 1.0.7.
- Running notification used ID 41 and retained its ongoing foreground flags.
- Pausing retained the same notification, title `스톱워치 일시정지`, static text
  `일시정지 · 00:02`, requestPromotedOngoing=true, showChronometer=false,
  and shortCriticalText `일시정지`. Service was still foreground with ID 41.
- A later observation retained the identical 00:02 time and promotion request.
- APP-origin resume showed running text `현재 시간 00:04`, chronometer=true,
  promotion request=true. Reset removed the active notification and stopped the service.
- No Samsung device was connected in this task. API 35 cannot verify Android 16
  hasPromotableCharacteristics/actual promotion, or Samsung lock-screen Now Bar.
- No physical-phone setting or Samsung ADB test flag was changed.
