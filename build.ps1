# build.ps1
# Wrapper script for building the ChorebooHabitTrackerFriend Android project.
#
# Usage (from WSL or any shell that can invoke powershell.exe):
#   powershell.exe -File build.ps1 assembleDebug
#   powershell.exe -File build.ps1 testDebugUnitTest
#   powershell.exe -File build.ps1 assembleRelease
#
# This script exists because the project must be built using the Windows-native
# Android SDK and JDK. Running ./gradlew from WSL fails because the SDK build-tools
# contain Windows .exe binaries (aapt2.exe, etc.) that Linux cannot execute.
# All Gradle tasks must go through gradlew.bat on the Windows side.

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

# Fall back to Android Studio bundled JBR if JAVA_HOME is not already set.
$ANDROID_STUDIO_JBR = 'C:\Program Files\Android\Android Studio\jbr'
if (-not $env:JAVA_HOME) {
    if (Test-Path $ANDROID_STUDIO_JBR) {
        $env:JAVA_HOME = $ANDROID_STUDIO_JBR
        Write-Host "JAVA_HOME not set -- using Android Studio JBR: $env:JAVA_HOME"
    } else {
        Write-Error "JAVA_HOME is not set and Android Studio JBR was not found at $ANDROID_STUDIO_JBR"
        exit 1
    }
}

$ProjectDir = 'C:\Users\elihebdon\AndroidStudioProjects\ChorebooHabitTrackerFriend'
Set-Location $ProjectDir

if ($GradleArgs.Count -eq 0) {
    Write-Host 'No Gradle task specified. Available common tasks:'
    Write-Host '  assembleDebug'
    Write-Host '  assembleRelease'
    Write-Host '  testDebugUnitTest'
    Write-Host '  connectedDebugAndroidTest  (requires device/emulator)'
    Write-Host '  lint'
    Write-Host ''
    Write-Host 'Usage: powershell.exe -File build.ps1 TASK [options]'
    exit 0
}

Write-Host "Running: gradlew.bat $GradleArgs"
Write-Host ''

cmd /c gradlew.bat @GradleArgs
exit $LASTEXITCODE
