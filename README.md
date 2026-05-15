# Namma Railu Buddy

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue.svg)](app/build.gradle.kts)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20RTDB%20%7C%20FCM-orange.svg)](https://firebase.google.com)

**Namma Railu Buddy** is a Kotlin Android railway assistance app focused on **Karnataka / South Western Railway** commuters. It combines Firebase-backed live station data, crowd-sourced platform verification, destination proximity alarms, train listings, delay alerts, and map-based navigation in a single mobile experience.

> *Namma* (ನಮ್ಮ) means “our” in Kannada — a commuter-first companion for daily rail travel.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Screenshots](#screenshots)
- [Installation](#installation)
- [Build & APK](#build--apk)
- [Firebase Setup](#firebase-setup)
- [Project Structure](#project-structure)
- [Documentation](#documentation)
- [Testing](#testing)
- [Future Enhancements](#future-enhancements)
- [Contributing](#contributing)
- [License](#license)

---

## Features

| Module | Description |
|--------|-------------|
| **Authentication** | Email/password sign-up and login via Firebase Auth |
| **Home dashboard** | Station & train selection, live status, coach sequence visualization |
| **Platform pings** | Crowd-sourced platform updates with vote counts and cooldown |
| **Destination alarm** | GPS-based proximity alert when nearing destination station |
| **Trains** | Searchable list of trains (Firebase + local fallback data) |
| **Alerts** | Delay and platform-change notifications from live data |
| **Map** | Google Maps view with station marker |
| **Profile** | User email, logout, app info |

**Regional focus:** Stations include SBC, YPR, MYS, DVG, UBL, MAQ, DWR and Karnataka-centric train names (Siddhaganga, Brindavan, Chamundi, etc.).

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | XML layouts, View Binding, Material Design |
| Architecture | Activities + Fragments, bottom navigation |
| Backend | Firebase Authentication, Realtime Database (asia-southeast1) |
| Maps & location | Google Play Services Maps & Fused Location |
| Build | Gradle (Kotlin DSL), AGP 9.x |
| Testing | JUnit, Espresso (instrumented) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     LoginActivity / RegisterActivity         │
│                     (Firebase Auth)                          │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  MainActivity + BottomNavigationView                         │
│  ┌──────────┬──────────┬──────────┬──────────┐            │
│  │  Home    │  Trains  │  Alerts  │ Profile  │            │
│  │ Fragment │ Fragment │ Fragment │ Fragment │            │
│  └────┬─────┴────┬─────┴────┬─────┴──────────┘            │
└───────┼──────────┼──────────┼───────────────────────────────┘
        │          │          │
        ▼          ▼          ▼
   Firebase RTDB (stations, platformPings, alarmSessions)
        │
        ▼
   MapActivity (Google Maps)
```

Detailed flows: [docs/APP_FLOW.md](docs/APP_FLOW.md) · Architecture notes: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## Screenshots

> Add images under `docs/screenshots/` (see [docs/screenshots/README.md](docs/screenshots/README.md)).

| Login | Home | Trains | Alerts |
|-------|------|--------|--------|
| *Add `01-login.png`* | https://github.com/Sushameendra07/namma-railu-buddy/blob/03a182fe676c4353f4e7b0aec7c4bd5cb09b527d/WhatsApp%20Image%202026-05-15%20at%209.09.57%20PM.jpeg| https://github.com/Sushameendra07/namma-railu-buddy/blob/8399cfe22ed571cf40df2be2e4620d3fe068a7ac/WhatsApp%20Image%202026-05-15%20at%209.09.58%20PM.jpeg| *Add `06-alerts.png`* |

**Demo:** See [DAY5_DEMO_SCRIPT.md](DAY5_DEMO_SCRIPT.md) for a 3–5 minute presentation script.

---

## Installation

### Prerequisites

- Android Studio Ladybug (2024.2+) or newer
- JDK 11+
- Android SDK with API 36 (compile) / min API 24
- A Firebase project with Auth + Realtime Database enabled
- Google Maps API key (Maps SDK for Android)

### Clone & open

```bash
git clone https://github.com/YOUR_USERNAME/namma-railu-buddy.git
cd namma-railu-buddy
```

1. Open the project folder in **Android Studio**.
2. Copy `app/google-services.json.example` → `app/google-services.json` and fill in values from the [Firebase Console](https://console.firebase.google.com/).
3. Ensure `local.properties` contains your SDK path (Android Studio creates this automatically; it is gitignored).
4. Configure Maps API key (see [Firebase Setup](#firebase-setup)).
5. Sync Gradle and run on emulator or device.

---

## Build & APK

### Debug (development)

```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat installDebug

# macOS / Linux
./gradlew assembleDebug
./gradlew installDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Release

```bash
gradlew.bat assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`  
For Play Store distribution, configure signing in `app/build.gradle.kts` and use `bundleRelease` for AAB.

### Verify build

```bash
gradlew.bat assembleDebug assembleRelease
gradlew.bat connectedDebugAndroidTest
```

---

## Firebase Setup

### 1. Create project

1. [Firebase Console](https://console.firebase.google.com/) → Add project → **Namma Railu Buddy**.
2. Add Android app with package: `com.greatingcard.nammarailubuddy`.
3. Download `google-services.json` → place in `app/` (do **not** commit; see `.gitignore`).

### 2. Enable services

| Service | Use in app |
|---------|------------|
| **Authentication** | Email/Password |
| **Realtime Database** | Stations, trains, platform pings, alarms |
| **Cloud Messaging** | Dependency included for future push alerts |

Database URL (regional): `https://nammarailubuddy-default-rtdb.asia-southeast1.firebasedatabase.app`

### 3. Seed data

Import [day1_firebase_seed.json](day1_firebase_seed.json) into Realtime Database (JSON import) for demo stations and trains.

### 4. Security rules (important)

Use authenticated reads/writes in production. Example starter rules:

```json
{
  "rules": {
    "stations": { ".read": "auth != null", ".write": false },
    "platformPings": {
      ".read": "auth != null",
      "$pingId": { ".write": "auth != null" }
    },
    "alarmSessions": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid"
      }
    }
  }
}
```

### 5. Google Maps API key

**Do not hardcode keys in `AndroidManifest.xml` for public repos.**

Recommended: store in `local.properties`:

```properties
MAPS_API_KEY=your_maps_key_here
```

And reference via `manifestPlaceholders` in `app/build.gradle.kts` (see [docs/CODE_QUALITY.md](docs/CODE_QUALITY.md)).

---

## Project Structure

```text
nkn/
├── app/
│   ├── src/main/java/com/greatingcard/nammarailubuddy/
│   │   ├── adapters/          # RecyclerView adapters
│   │   ├── models/            # Train data class
│   │   ├── *Activity.kt       # Login, Register, Main, Map
│   │   └── *Fragment.kt       # Home, Trains, Alerts, Profile
│   ├── src/main/res/          # layouts, drawables, menu, values
│   ├── google-services.json   # local only (gitignored)
│   └── build.gradle.kts
├── docs/                      # Architecture, testing, evaluation
├── gradle/
├── day1_firebase_seed.json    # Firebase demo seed
├── DAY5_DEMO_SCRIPT.md
├── DAY5_TEST_MATRIX.md
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

**Do not upload:** `.idea/`, `.gradle/`, `build/`, `local.properties`, `app/google-services.json`, `captures/`.

---

## Documentation

| Document | Purpose |
|----------|---------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Components, Firebase schema, dependencies |
| [docs/APP_FLOW.md](docs/APP_FLOW.md) | Auth, navigation, alarms, platform pings |
| [docs/TESTING.md](docs/TESTING.md) | Test matrix & manual QA checklist |
| [docs/CODE_QUALITY.md](docs/CODE_QUALITY.md) | Refactoring & security recommendations |
| [docs/EVALUATION_REVIEW.md](docs/EVALUATION_REVIEW.md) | Internship evaluator perspective |
| [docs/COMMIT_HISTORY_SUGGESTIONS.md](docs/COMMIT_HISTORY_SUGGESTIONS.md) | Suggested Git timeline |
| [docs/PROJECT_DESCRIPTIONS.md](docs/PROJECT_DESCRIPTIONS.md) | Resume / LinkedIn / GitHub blurbs |

---

## Testing

- Automated: `./gradlew connectedDebugAndroidTest`
- Manual checklist: [DAY5_TEST_MATRIX.md](DAY5_TEST_MATRIX.md)
- Full guide: [docs/TESTING.md](docs/TESTING.md)

---

## Future Enhancements

- [ ] Live train status via IRCTC / third-party API
- [ ] Firebase Cloud Messaging for push delay alerts
- [ ] Dark theme & Material 3 polish
- [ ] Offline cached routes and last-known status
- [ ] AI chat assistant for platform / coach queries
- [ ] Real GPS train tracking on map (not mock marker)
- [ ] Seat availability / PNR lookup integration

---

## Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`.
3. Commit with clear messages (see [docs/COMMIT_HISTORY_SUGGESTIONS.md](docs/COMMIT_HISTORY_SUGGESTIONS.md)).
4. Open a Pull Request describing changes and test steps.

Please do not commit secrets (`google-services.json`, API keys, keystores).

---

## License

This project was developed as an internship / academic portfolio piece.  
Specify your license here (e.g. MIT) before public release:

```text
MIT License — Copyright (c) 2025 Your Name
```

---

## Acknowledgments

- South Western Railway commuters and Karnataka station codes for regional context
- Firebase & Google Maps platform documentation
- Demo seed data: `day1_firebase_seed.json`
