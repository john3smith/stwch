# Samsung RTL follow-up — 2026-10-09

## Scope and actual target

The user connected the existing RTL session. No new reservation was created.
ADB target `localhost:46217` reported SM-S731N, Android 16/API 36,
One UI 8.5 (`ro.build.version.oneui=80500`). The emulator stayed separate.
Installed the existing 1.0.5 test APK preserving data and enabled its notification
runtime permission in this test environment. No account or user content was read.

## Fresh verification

- First START from IDLE: foreground notification is ongoing and requests promotion.
- Posted notification has `FLAG_PROMOTED_ONGOING`, ProgressStyle, category
  stopwatch, PUBLIC visibility, and an enabled IMPORTANCE_LOW channel.
- App notification settings allow notifications, lock-screen display and contents.
- Actual awake lock screen shows Now brief, not stwch. Black captures taken too
  early after wake were discarded as evidence; a fresh awake capture was inspected.
- SystemUI reports no primary/secondary ongoing activity for stwch. Its updates
  say `onEntryUpdated and OA data is empty`; it remains in the TimeOrder section.
- A bounded BigTextStyle/chronometer comparison also did not display the Now bar.
  This experiment was reverted. The original 1.0.5 APK was restored on the device.
- `testDebugUnitTest lintDebug assembleDebug` succeeds; existing 29 JVM tests
  report zero failures/errors. No unverified formatting change was retained.

## Verified OEM gate, not merely an assumption

Read-only inspection of this device's own SystemUI and framework shows that
`NotificationEntry.setSbn` checks Samsung's allowed ongoing-app list before setting
the RON eligibility used by `isOngoingActivity`. `AllowedOngoingActivityListManager`
loads that list and its enabled state from the notification service. Android's
`FLAG_PROMOTED_ONGOING` alone does not bypass this separate Samsung gate.

Read-only Binder getter transaction numbers were obtained from this exact
device's `INotificationManager.Stub` before querying; they must NOT be assumed
portable or used in application code:

- `getOngoingActivityAllowListUsingState` (204): true.
- `getAllowedOngoingActivityAppList` (203): four packages — Google Maps, Samsung
  Interpreter, Samsung Safety Assurance, and Android Bluetooth. No stwch.
- `getBlockedRONAppList` (205): empty.

The combination of the enabled gate, missing package and observed empty OA data
explains this build's exclusion. It does not prove the behavior of all Galaxy
phones or firmware releases. The OEM also has a developer all-app testing option;
its existence in implementation is not proof that managed RTL settings permit it.

## Safety and remaining work

No support-list changes, device-policy changes, developer override, package
impersonation, retained private notification extras, hidden API calls in the app, root,
fake media session, or system modification was made. Downloaded inspection tools
were from jadx's official release, with SHA-256 checked against GitHub's digest;
device binaries/decompiled output stay in ignored local verification files.

The application-side startup fix is built and tested, but actual Now bar display
is still blocked on this firmware. Do not label 1.0.5 a completed Now bar fix or
publish a final binary on that basis. A user-approved, normally accessible
developer test option can establish rendering compatibility, but is not a
production fix and must not be forced past RTL management restrictions. Samsung
admission or firmware without this gate is needed for default production display.

References:

- https://developer.android.com/develop/ui/views/notifications/live-update
- https://www.samsung.com/us/support/answer/ANS10002524/

The Android guide explicitly permits additional OEM eligibility criteria.

## Additional isolated compatibility experiment

After the initial report, tested Samsung's ongoing-activity manifest metadata
and five Bundle hints verified in this device's framework: style=1, primary and
secondary text, and Now bar primary and secondary text. No permissions, support
lists or system settings were changed. This created stwch OA data, but SystemUI
placed it in **Hidden list / promoted:false**; actual lock screen still did not
show stwch. App notification settings still did not offer its Samsung live
notification switch. These unsupported hints and metadata were reverted, and the
unchanged original 1.0.5 APK restored. This strengthens the distinction between
creating OEM presentation data and being admitted for actual display.

## Approved developer-option follow-up

The user approved enabling the all-app live-notification option only on the RTL
test device, if normally accessible, without bypassing device-management policy.
The same ADB device remained connected. Opened About phone / Software information
through normal Settings UI and tried the observed Build number row. Repeated taps
did not enable developer settings (`development_settings_enabled` stayed null).
Opening `android.settings.APPLICATION_DEVELOPMENT_SETTINGS` returned to the
Samsung launcher instead of a developer settings screen. No PIN/password prompt
was encountered or automated. These observations establish that the attempted
normal Settings path is unavailable, not its exact management-policy cause.

No secure/global settings write, Binder setter, support-list change, or developer
override was applied. The all-app option is **not enabled** and rendering under
that option remains untested. The signed-in RTL Web Client has its own Developer
Options entry, but neither current Playwright nor Chrome DevTools browser bindings
exposed that existing session (only separate about:blank pages). A user opening
that official client panel can reveal whether RTL offers a supported enable path;
do not infer that it does or force the device's disabled Settings path.
