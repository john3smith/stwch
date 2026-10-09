# Confirmed Samsung Now Bar display: 2026-10-09

## Verified result and scope

The existing stwch 1.0.5 test APK displays an actual Now Bar on the connected
Samsung RTL Galaxy S25 FE (SM-S731N), Android 16 / One UI 8.5 (80500), when the
device's Samsung all-app Now Bar test flag is enabled. No APK/source change was
needed. This is an explicitly user-approved temporary device-setting experiment,
not a public Samsung integration API or a production fix built into the APK.

The inspected SystemUI APK matches the device file's SHA-256:
3c766fc15a924ab36a0ee229f74b7457858dcb57d70f233d349cdda5fd784c57.
Its SettingsHelper defines INDEX_DEVELOP_RON_TEST as
enable_notification_nowbar_test, and checks whether its integer value equals 1.
Its NotificationEntry uses this developer option in the additional OEM gate.

## Controlled comparison

| Active settings | Visible lock-screen result |
| --- | --- |
| Original: test flag absent, app key absent, AppOp default | No stwch Now Bar |
| Test flag 1, app key 1, AppOp allow | stwch Now Bar displayed |
| Test flag 1, app key removed, AppOp restored to default | stwch Now Bar still displayed |
| Both keys removed, AppOp default; notification restarted | No stwch Now Bar again |

Displayed content: stopwatch icon, `스톱워치 실행 중`, and `현재 시간` followed
by the elapsed measurement. Two actual captures show elapsed values 53:07 and
53:54; the accumulated measurement was not reset. Each comparison uses an
APP-origin pause/resume and an awake lock-screen capture, not promotion flags
alone. Android promotion and Samsung display are distinct.

## Commands used in the approved experiment

Target was localhost:46217 only; no user's physical phone or emulator was changed.

```text
adb -s localhost:46217 shell settings put secure enable_notification_nowbar_test 1
adb -s localhost:46217 shell settings put system key_now_bar_com_local_stwch 1
adb -s localhost:46217 shell cmd appops set com.local.stwch POST_PROMOTED_NOTIFICATIONS allow
```

The first flag alone was sufficient in the later active configuration. No
independent requirement for the app-specific system key or explicit AppOp allow
was established here. Do not conclude an arbitrary system key is supported just
because `settings put` successfully stores it.

## Restoration and limitations

All three values were restored and verified: secure flag absent (null), system
key absent (null), AppOp default. Only the two newly created settings keys were
removed; no application files, measurement history or user data were deleted.
stwch remains running, but Now Bar is not currently displayed after restoration.

Enabling the Samsung test flag affects eligible apps device-wide, not just stwch.
An ordinary APK cannot enable secure device settings with normal app permissions.
Do not add hidden API calls, WRITE_SECURE_SETTINGS workarounds, package spoofing,
fake media sessions, or support-list modification to the app. Applying the test
setting to another device or retaining it permanently needs explicit user scope.
Firmware-update/reboot persistence and the user's actual phone remain untested.

## Evidence

Ignored local evidence in verification/: nowbar-all-three-enabled-lock.png,
nowbar-test-flag-only-lock.png, nowbar-settings-restored-lock.png, and
nowbar-temporary-settings-20261009.md.

The visible RTL Chrome window was foregrounded and captured for Telegram while
Now Bar was displayed. No new APK was built, uploaded or attached for this test.
