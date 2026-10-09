# stwch 1.0.10 routine foreground handoff

## Scope and root cause

The requested behavior is now to show stwch's main screen when a routine runs,
not to keep a command-only shortcut invisible. Existing shortcut IDs and the
`com.local.stwch.ShortcutActivity` component remain compatible with saved mappings.

Manifest audit (unchanged):

| Component | Launch mode / task | History / Recents | Role |
| --- | --- | --- | --- |
| MainActivity | singleTop, normal application affinity | default history and Recents inclusion | launcher and visible stopwatch |
| ShortcutActivity | standard, empty affinity | noHistory=true, excludeFromRecents=true | transient OEM mapping entry |
| ShortcutPickerActivity | default standard/app affinity | defaults | CREATE_SHORTCUT compatibility picker |
| HistoryActivity | default standard/app affinity, not exported | defaults | saved interval list |
| StopwatchService | not an Activity; not exported | stopWithTask=false, specialUse FGS | measurement and notification |

In 1.0.9 ShortcutActivity dispatched a service command in onResume and immediately
called finish(), without opening MainActivity. Therefore a cold shortcut-only
launch never created a normal, persistent UI task. Its hidden, no-history command
entry disappears while the independent foreground service keeps working. This
explains why background measurement can work with no stwch recent UI task.

Fresh API 35 reproduction also demonstrated a caller-flags-dependent home return:
`am start -W -n com.local.stwch/.ShortcutActivity -a com.local.stwch.START -f 0x1000c000`.
The flags are NEW_TASK | CLEAR_TASK | TASK_ON_HOME. From Settings, or with stwch
MainActivity visible, the resumed Activity became NexusLauncher/Home. With an
already existing MainActivity, its task remained stopped in Recents: disappearance
of that existing task was NOT reproduced. No Samsung routine Intent was captured
in this run, so these flags are a verified Android reproduction, not a claim about
what the user's Galaxy actually sends.

MainActivity had no onNewIntent command handling. Its onCreate/onResume did not
finish or manipulate tasks; onPause persisted the shared measurement. No
finishAffinity, finishAndRemoveTask, moveTaskToBack, HOME Intent or CLEAR_TASK call
exists in the app's current source. ShortcutActivity's finish and the picker's
finish are ordinary completion of transient activities. The FGS is independent
of Activity lifetime and remains the correct owner of stopwatch state.

## Minimal fix

1. A resumed ShortcutActivity creates a new explicit MainActivity Intent. It does
   not copy arbitrary caller extras, categories, data or launch flags.
2. NEW_TASK selects the normal application task instead of the command-only task;
   CLEAR_TOP returns from HistoryActivity to MainActivity; SINGLE_TOP reuses the
   existing MainActivity through onNewIntent rather than replacing its root.
   Caller CLEAR_TASK, TASK_ON_HOME and EXCLUDE_FROM_RECENTS are not forwarded.
3. The shortcut finishes only itself AFTER handing off to the visible main UI.
   It no longer dispatches the service command as well, preventing double delivery.
4. MainActivity queues valid commands in onCreate/onNewIntent and dispatches them
   once in onResume. Saved state contains only still-pending commands, so ordinary
   resume or rotation does not replay a completed PAUSE as a double-pause RESET.
5. The existing service, stopwatch engine, atomic persistence, notification actions
   and routine-specific START/PAUSE semantics remain unchanged. Notification
   buttons still operate through getForegroundService, without opening the UI.
6. Local `stwch.Activity` logcat entries include lifecycle event, instance identity,
   task ID, root/finishing state, flags and parsed command only. No arbitrary
   caller content, account information or screen data is logged.

No singleTask/singleInstance manifest change, accessibility workaround, service
background Activity launch, device-policy change or new library was added.

## Verification performed

Target: regular YTDown_API_35 emulator, emulator-5554, Android 15/API 35.
Physical Samsung hardware and ARTEMIS were not used. Install used `adb install -r`
without clearing app data. Test intervals exist only in the emulator.

- testDebugUnitTest: **43 tests**, zero failures/errors; includes six new pure
  inbox tests for order, single delivery, pending restoration, invalid state,
  intentional double pause, and no replay after completed pause.
- lintDebug and assembleDebug: successful. Existing deprecated legacy shortcut
  constants and Gradle upgrade warnings remain; this is not a warning-free claim.
- Normal ACTION_MAIN/CATEGORY_LAUNCHER launch: visible MainActivity, cold launch.
- Routine START with 0x1000c000 while MainActivity visible: same ActivityRecord,
  task 285, instance 26752964; onNewIntent then onResume; no Home return.
- Cold routine START after force-stop: visible MainActivity, task 289, RUNNING.
- Routine PAUSE: PAUSED, frozen accumulated=2542 ms. Re-opening through launcher
  and requesting rotation kept the same frozen value and did not reset it.
  Rotation requests alone are not proof that an OEM recreated an Activity; the
  pure inbox recreation test verifies the no-replay policy separately.
- Routine START from Settings while paused: MainActivity foreground, accumulated
  remained 2542 ms (resume rather than reset).
- Routine START from Home while already running: MainActivity foreground,
  accumulated reset to 0 as requested for a real second START command.
- MainActivity task 289 remained Recent #0 with base/topActivity=MainActivity;
  command trampolines did not remain on its stack.
- Instance 245275892/task 289 was reused on the background handoffs. Trace shows
  new-intent/resume, not a root destroy/recreate for each command.

During later follow-up verification, the emulator's system_server stopped
responding, emitted system_server_pre_watchdog and then temporarily lost the
Activity service. That attempt was not accepted as a passing test. A data-
preserving emulator reboot was initiated; follow-up results are recorded below
only after fresh checks. No stwch Java crash was observed in the available crash
buffer. The watchdog's full underlying cause was not established, so this is
not presented as proof that memory pressure or the app caused it.

## Limits and real-Galaxy diagnostics

This is a verified fix for the app's command-only/finish path and the emulator
flag reproduction, NOT confirmation that Samsung Modes and Routines works on the
user's phone. A routine that issues a later separate Home action, destroys a task
itself, or is rejected by lock-screen/background launch policies cannot be fully
controlled by the app. No background-launch security policy is bypassed.

If the Galaxy still goes Home, compare a launcher launch and the actual routine:

```text
adb logcat -v time -s stwch.Activity:D stwch.FGS:D *:S
adb shell dumpsys activity activities
adb shell dumpsys activity recents
adb shell dumpsys activity services com.local.stwch
```

Inspect stwch entries locally; full activity/recents dumps may include other apps
and must not be shared unredacted. `shortcut-handoff` reveals the original flags;
`new-intent` / matching instance+task IDs proves reuse; `destroy finishing=true`
indicates Activity completion. Separate this from a service crash or a later
launcher/SystemUI foreground transition. Galaxy firmware version and the exact
routine's actions/ordering are needed if the symptom remains.

References (official):
- https://developer.android.com/guide/components/activities/tasks-and-back-stack
- https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_TASK_ON_HOME
- https://developer.android.com/guide/components/activities/background-starts

## Artifact

`releases/stwch-v1.0.10-debug.apk` — versionCode 11, 2439441 bytes.
SHA-256: `74d8faa0b2fbfeaf89eb8d869e2b037ebbf826fd123b035400b659bca4337981`.
Debug-signed test build, not a physically verified Galaxy production release.

## Fresh follow-up after emulator recovery

- The Windows emulator crash-report dialog identified a native
  EXCEPTION_ACCESS_VIOLATION_WRITE in qemu-system-x86_64.exe (address 0x0).
  The report was NOT sent externally. Declining it allowed the restarted AVD
  to boot; no data wipe, SDK reinstall or phone test was performed.
- Fresh boot_completed=1 and installed versionName=1.0.10/versionCode=11 verified.
- Repeated routine PAUSE: first command froze 1066 ms; second changed state to
  IDLE/0 ms. Service list then showed no remaining stopwatch service; MainActivity
  stayed resumed in task 291. No accidental duplicate command dispatch occurred.
- Actual History button was located in a fresh UI hierarchy and tapped. It opened
  HistoryActivity above MainActivity in task 291. Routine START with 0x1000c000
  returned to the same MainActivity and removed only the HistoryActivity above it.
- A new PAUSE froze 1001 ms. Explicit landscape rotation caused MainActivity
  destroy/create/resume with finishing=false, changing instance 31686550 to
  256974002 while retaining task 291. State stayed PAUSED/1001 ms, NOT IDLE.
  Restoring original rotation settings recreated it again (instance 82056950)
  without replaying the command. Rotation settings were restored in finally.
- The fresh UI hierarchy and visible emulator screen retained the stopwatch,
  history button and ADB help, with the two previously removed diagnostic/settings
  buttons absent. This was emulator UI verification, not Galaxy Now Bar evidence.
