param(
    [string]$CommitMessage = "Version 1.1: fix biometric lock and signed APK export"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$env:Path = "C:\Program Files\Git\cmd;$env:Path"

$versionMain = "1"
$versionMinor = "1"
$artifactName = "RockMemo-v${versionMain}.${versionMinor}.apk"

$gradlew = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat not found in $repoRoot. Run the Gradle wrapper once before building."
}

& $gradlew assembleRelease
if ($LASTEXITCODE -ne 0) {
    throw "Android release build failed."
}

$androidSdk = $env:ANDROID_HOME
if (-not $androidSdk) {
    $androidSdk = $env:ANDROID_SDK_ROOT
}
if (-not $androidSdk) {
    $androidSdk = 'C:\Users\topgunE2\AppData\Local\Android\Sdk'
}

if (-not (Test-Path $androidSdk)) {
    throw "Android SDK not found at $androidSdk"
}

$buildToolsRoot = Join-Path $androidSdk 'build-tools'
$apksigner = Get-ChildItem $buildToolsRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1 | ForEach-Object { Join-Path $_.FullName 'apksigner.bat' }
if (-not $apksigner) {
    throw "apksigner.bat not found in $buildToolsRoot"
}

$keystore = Join-Path $HOME '.android\rockmemo-release.jks'
$keyAlias = 'rockmemo-release'
$storePassword = 'rockmemo123'
$keyPassword = 'rockmemo123'

if (-not (Test-Path $keystore)) {
    $jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Users\topgunE2\tools\jdk-17\jdk-17.0.20+8' }
    & (Join-Path $jdk 'bin\keytool.exe') -genkeypair -v `
        -keystore $keystore `
        -alias $keyAlias `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -storepass $storePassword `
        -keypass $keyPassword `
        -dname "CN=RockMemo, OU=Development, O=Local, L=Unknown, S=Unknown, C=US"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create local keystore for signing."
    }
}

$unsignedApk = Join-Path $repoRoot "app\build\outputs\apk\release\app-release-unsigned.apk"
$releaseApk = Join-Path $repoRoot "app\build\outputs\apk\release\app-release-signed.apk"
if (-not (Test-Path $unsignedApk)) {
    throw "Release APK not generated at $unsignedApk"
}

& $apksigner sign --ks $keystore --ks-pass pass:$storePassword --key-pass pass:$keyPassword --ks-key-alias $keyAlias --out $releaseApk $unsignedApk
if ($LASTEXITCODE -ne 0) {
    throw "Signing the APK failed."
}

$artifactDir = Join-Path $repoRoot "artifacts"
New-Item -ItemType Directory -Force -Path $artifactDir | Out-Null
Copy-Item -Path $releaseApk -Destination (Join-Path $artifactDir $artifactName) -Force

Write-Host "Signed APK exported to $artifactDir\$artifactName"

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

Write-Host "Build, sign, export, commit, and push finished successfully."
