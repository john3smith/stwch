# Samsung RTL Now bar verification — 2026-10-08

## Target and authorization

User-connected Samsung Remote Test Lab device, SM-S731N, Android 16/API 36,
One UI 8.5 (`ro.build.version.oneui=80500`). Installed debug APKs preserving
application data. The ordinary API 35 emulator remained connected separately.
No personal phone, ARTEMIS, new reservation, router/firewall change, root,
private Samsung notification extras, or fake media session was used.

## Confirmed observations

- stwch 1.0.3 launches and runs its stopwatch on the Samsung device.
- Notification permission and its channel are enabled.
- App notification settings allow lock-screen notifications and content display.
- While running, the app diagnostic reports all three Android 16 conditions true:
  `canPostPromotedNotifications()`, `hasPromotableCharacteristics()`,
  and the posted notification's `FLAG_PROMOTED_ONGOING`.
- Notification shade displays an expanded stopwatch card with elapsed time,
  progress indicator, pause/reset actions, and chronometer.
- Settings → Now bar → View more / Live information lists built-in supported
  entries but does not list stwch. Scrolling to the end was verified.
- On a freshly awakened lock screen, Now brief appears at the bottom;
  stwch does not. A fresh awake swipe at the bar did not expose stwch either.

## Experiments that did not fix the issue

Compared original indeterminate ProgressStyle with a local 1.0.4 experiment
using a 60-second current-minute progress segment, then a tracker icon. Both
experimental APK variants were installed and started on the Samsung device.
Neither exposed stwch in the Now bar. The current-minute progress/version/string
changes were reverted; the source retains 1.0.3 rather than shipping an
unverified improvement. Experimental artifacts are local-only, not releases.

The tracker build passed Gradle compilation, 24 JVM tests (0 failures/errors),
and lint. The build wrapper correctly refused replacing an existing versioned
1.0.4 APK with different bytes; the tracker experiment was installed from
`app/build/outputs/apk/debug/app-debug.apk`, not that stale versioned artifact.

## Evidence and interpretation

Local, ignored verification captures include `rtl-shade-before.png`,
`rtl-lock-fresh.png`, `rtl-lock-swiped-awake.png`, and
`rtl-lock-tracker-built.png`. No screenshots containing private credentials
were captured or published. UiAutomator cannot reliably reach idle while the
notification updates every second; failed dumps were not treated as fresh UI.

Android notification promotion works; Samsung's final Now bar rendering does
not on this tested configuration. This narrows the issue to OEM display
eligibility/compatibility or configuration, but does **not** establish a specific
undocumented allowlist as the cause. No claim of a working Now bar fix is made.
The developer-settings intent returned to Home, so no developer override was
enabled and no managed-device policy was bypassed.

## Next bounded verification

Compare the same APK on a Samsung build whose live-information settings list
third-party apps, or obtain Samsung's documented admission criteria for this
firmware. A developer-only all-app override, if offered and approved, may help
distinguish OEM admission from app formatting but is not a production fix.
The separately reported routine `TASK_ON_HOME` behavior remains unresolved;
this session does not claim that it was fixed.

References:

- https://developer.android.com/develop/ui/views/notifications/live-update
- https://www.samsung.com/us/support/answer/ANS10002524/
