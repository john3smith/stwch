@echo off
setlocal
set "ADB=C:\ytdownforand\.tools\android-sdk\platform-tools\adb.exe"
if not exist "%ADB%" (
    echo ERROR: ADB executable was not found.
    pause
    exit /b 1
)
echo Samsung Now Bar unofficial device-wide test setting.
echo Connect only one physical phone. Emulators are excluded.
echo Original value - note this before restoring the setting:
"%ADB%" -d shell settings get secure enable_notification_nowbar_test
if errorlevel 1 goto :failed
"%ADB%" -d shell settings put secure enable_notification_nowbar_test 1
if errorlevel 1 goto :failed
echo Applied value - should be 1:
"%ADB%" -d shell settings get secure enable_notification_nowbar_test
if errorlevel 1 goto :failed
echo Pause and resume stwch, then check the lock screen.
echo This setting does not guarantee Now Bar support on every firmware.
pause
exit /b 0

:failed
echo ERROR: ADB command failed. Check connection and USB debugging authorization.
echo Multiple physical devices must not be connected at the same time.
pause
exit /b 1
