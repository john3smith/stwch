# stwch 1.0.9: paused Now Bar shows elapsed time

The paused notification no longer uses `일시정지` as its short critical text.
Title, normal text, BigTextStyle expanded text, and short critical text all use
the same frozen `NotificationTime.text(elapsed)` string. Examples: 01:23, 1:01:05.
This accommodates Samsung surfaces choosing different standard notification fields.

Ongoing/promoted request and foreground service remain active while paused.
Chronometer remains off; no timer callbacks are scheduled while paused.
Resume continues the measurement, reset ends the notification. No Samsung private
API or device-setting change was added. Includes 1.0.8's removal of the diagnostics
and integration buttons while retaining the separate ADB help.

## Verification

- 37 JVM tests passed, lintDebug and assembleDebug succeeded.
- Installed version 1.0.9 / versionCode 10 with install -r on emulator-5554,
  standard YTDown_API_35. No data wipe.
- Actual posted paused notification ID 41: title=text=bigText=shortCriticalText=00:01.
- showChronometer=false, requestPromotedOngoing=true, ONGOING_EVENT and foreground
  service flags remained set. Value stayed identical after a later 3-second check.
- Initial diagnostic ran before the asynchronous pause finished and saw missing
  paused fields; reinspection after the state transition verified all four fields.
- UI hierarchy still excluded the two removed buttons and retained ADB help.
- API 35 cannot confirm Android 16 promotion or actual Samsung Now Bar rendering.
  A real One UI 8.5 phone must verify the final appearance. No physical phone was
  connected or configured during this task.

Existing routine TASK_ON_HOME caller behavior and firmware-dependent Samsung
Now Bar eligibility remain known limitations; this release is a debug test build.
