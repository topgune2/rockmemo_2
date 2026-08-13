param(
    [string]$CommitMessage = "Release build: biometric lock, note management, version display, APK export"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$env:Path = "C:\Program Files\Git\cmd;$env:Path"

$versionMain = "1"
$versionMinor = "0"
$artifactName = "RockMemo-v${versionMain}.${versionMinor}.apk"

$gradlew = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat not found in $repoRoot. Run the Gradle wrapper once before building."
}

& $gradlew assembleRelease
if ($LASTEXITCODE -ne 0) {
    throw "Android release build failed."
}

$releaseApk = Join-Path $repoRoot "app\build\outputs\apk\release\app-release-unsigned.apk"
if (-not (Test-Path $releaseApk)) {
    throw "Release APK not generated at $releaseApk"
}

$artifactDir = Join-Path $repoRoot "artifacts"
New-Item -ItemType Directory -Force -Path $artifactDir | Out-Null
Copy-Item -Path $releaseApk -Destination (Join-Path $artifactDir $artifactName) -Force

Write-Host "APK copied to $artifactDir\$artifactName"

# Commit source changes and the exported APK artifact.
git add .
git commit -m $CommitMessage
if ($LASTEXITCODE -ne 0) {
    Write-Host "No changes to commit. Continuing without a new commit."
}

git push origin HEAD
if ($LASTEXITCODE -ne 0) {
    throw "Push to origin failed. Confirm GitHub remote and authentication."
}

Write-Host "Build, export, commit, and push finished successfully."
