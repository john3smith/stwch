# Now bar startup follow-up — 2026-10-09

## Confirmed defect and narrow fix

The old onStartCommand built and posted the foreground notification before
applying the incoming stopwatch command. Starting from IDLE therefore initially
posted a paused/non-ongoing/non-promoted notification, then replaced it with the
running notification. This ordering is directly visible in source. It is NOT
proof that Samsung caches initial eligibility or that this is the cause of the
previous Now bar failure.

ForegroundCommandSequence now applies the pure in-memory state transition first,
publishes the matching initial FGS notification, then persists through the existing
AtomicFile store. Disk I/O/fsync is not moved ahead of startForeground. Subsequent
timer refresh, notification channel, styles, command origins and routine semantics
remain unchanged. A small diagnostic log contains only state and notification
flags, not elapsed time, histories, URLs or personal data.

Local version is 1.0.5 / versionCode 6. The earlier local-only 1.0.4 formatting
experiments and versioned APK are preserved and not reused or overwritten.

## Verification

- testDebugUnitTest, lintDebug, assembleDebug: passed after fixing an interpolation
  syntax error introduced during this edit.
- 29 JVM tests, zero failures/errors (22 engine, 2 notification-time, 5 command
  sequencing). New cases cover initial start, accumulated-time resume, pause,
  null-command service recovery, and the routine's second-pause reset.
- API 35 regular emulator: data-preserving APK installation passed.
- Cold first START log: RUNNING, ongoing=true, promoteRequested=true.
- PAUSE: PAUSED, ongoing=false, promoteRequested=false.
- Resume: RUNNING; second routine PAUSE: IDLE. No stwch crash found in crash buffer.
- APK SHA-256: fa093155ee4991dd8de1417468dfdf6a8cd0160fb45f62afccece819e03c6017.

## Outstanding validation — not a completed Now bar fix

Only emulator-5554 was connected in this session; no Samsung RTL/physical device
was present. The emulator has no Samsung Now bar. The new initial-notification
sequence must be compared on a Samsung device, including a cold start, pause and
resume, lock-screen content settings, live-notification allowance, and actual
visible Now bar appearance. Android promotion flags alone do not establish that.

No fake media session, reflection into OEM classes, private Samsung extras,
developer allow-all override, service/channel reset, or device-wide setting change
was used. No final Release or new Telegram APK attachment was sent. The routine
TASK_ON_HOME problem was outside this narrow fix and remains unresolved.

References checked:

- https://developer.android.com/develop/ui/views/notifications/live-update
- https://developer.android.com/reference/android/provider/Settings#ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS
- https://www.samsung.com/us/support/answer/ANS10002524/

The current Android guide calls the management intent by a different name than
the SDK 36 reference; the compiled SDK 36 and reference both confirm the existing
APP_NOTIFICATION_PROMOTION_SETTINGS action. It was not replaced with a guessed
constant. Channel IMPORTANCE_LOW is above the documented IMPORTANCE_MIN blocker.
The Android 17 MetricStyle stopwatch API is not an Android 16 compatibility fix.
