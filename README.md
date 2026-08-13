# RockMemo

RockMemo is a small Android memo application designed for a device such as the OPPO Reno13A running Android 16. It uses biometric authentication before the notes can be accessed and stores notes in encrypted preferences.

## Features
- Biometric unlock (fingerprint or face recognition) on startup
- Multiple memo creation and deletion
- Version display with main and minor version components
- Release APK export script with GitHub commit + push workflow

## Build and export
1. Open a terminal in the project root.
2. Run `gradlew.bat assembleRelease` once the wrapper is generated.
3. Execute `powershell -ExecutionPolicy Bypass -File .\scripts\build_and_commit.ps1` to produce the APK, commit it to GitHub, and push the result.

## Notes
- The application targets Android 14 API 34 for compatibility with the local SDK environment.
- Each release exports the APK to `artifacts/RockMemo-v1.0.apk` before the repository commit is created.
