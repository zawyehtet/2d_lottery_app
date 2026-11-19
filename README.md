# Lucky2D (Pure Native Android — Kotlin + Jetpack Compose)

Lucky2D is a 100% offline, pure native Android Kotlin + Jetpack Compose 2D ledger and agent management application.

## Architecture & Tech Stack
- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose + Material 3 + Navigation Compose
- **State**: Lifecycle ViewModel (`LotteryViewModel`)
- **Offline Storage**: Local JSON files + SharedPreferences via Gson (`LuckyRepository`)
- **Audio & Haptics**: Native `SoundPool` (`CleanAudio2D`) with raw sound resources
- **Security**: Offline Device ID (SHA-256 of `ANDROID_ID`) & HMAC-SHA256 activation gate (`LicenseManager`)

## Build & Release
- **Local / CI Build**: `./gradlew assembleDebug` (JDK 17, `compileSdk = 34`, `minSdk = 24`, `targetSdk = 34`)
- **CI/CD**: `.github/workflows/build-apk.yml` builds `app/build/outputs/apk/debug/app-debug.apk` on push to `main` and publishes it to GitHub Releases (`latest`).
