# Architecture — Namma Railu Buddy

## Overview

Single-module Android app (`:app`) using a **classic Activity + Fragment** pattern with **View Binding**. There is no separate domain/repository layer; Firebase Realtime Database is accessed directly from fragments.

## Application layers

| Layer | Responsibility |
|-------|----------------|
| **Presentation** | Activities, Fragments, XML layouts, adapters |
| **Data** | Firebase Auth, Firebase Realtime Database, SharedPreferences |
| **Platform** | Location services, notifications, Google Maps |

## Activities

| Class | Role | Launcher |
|-------|------|----------|
| `LoginActivity` | Email login, session check | Yes |
| `RegisterActivity` | Account creation (min 6-char password) | No |
| `MainActivity` | Hosts bottom nav + fragment container | No |
| `MapActivity` | Full-screen map with marker | No |

## Fragments (MainActivity)

| Fragment | Primary responsibility |
|----------|------------------------|
| `HomeFragment` | Station/train lookup, coach UI, platform ping, destination alarm |
| `TrainsFragment` | RecyclerView list + search filter |
| `AlertsFragment` | Delay / platform-change alerts |
| `ProfileFragment` | User info, logout |

## Navigation

- **Auth flow:** `LoginActivity` ↔ `RegisterActivity` → `MainActivity`
- **In-app:** `MainActivity` `BottomNavigationView` swaps fragments via `FragmentManager.replace()`
- **Map:** `HomeFragment` → `MapActivity` (explicit `Intent`)

Navigation Component libraries are on the classpath but bottom nav is implemented manually in `MainActivity`.

## Data model

### `Train` (Kotlin data class)

Fields: `name`, `platform`, `delay`, `destination`, `eta`, `latitude`, `longitude`, `source`, `platformVotes`, `coachPosition`, `statusMessage`, `lastVerified`, `crowdDensity`.

Mapped from Firebase with `getValue(Train::class.java)`.

## Firebase Realtime Database schema

```
/
├── stations/
│   └── {stationCode}/          # e.g. SBC, YPR, MYS
│       ├── latitude
│       ├── longitude
│       └── trains/
│           └── {trainId}/
│               └── (Train fields)
├── platformPings/
│   └── {stationId}_{trainId}/
│       ├── stationId
│       ├── trainId
│       ├── latestPlatform
│       ├── count
│       ├── lastUpdated
│       └── lastUserId
└── alarmSessions/
    └── {userId}/
        └── {alarmId}/
            ├── trainId
            ├── sourceStationId
            ├── destinationStationId
            ├── triggerKm
            ├── state          # armed | triggered
            └── createdAt
```

**Database region:** `asia-southeast1` (URL hardcoded in `HomeFragment`, `TrainsFragment`, `AlertsFragment`).

## Adapters

| Adapter | Used by |
|---------|---------|
| `TrainAdapter` | `TrainsFragment` |
| `AlertAdapter` | `AlertsFragment` |

## Permissions

| Permission | Usage |
|------------|--------|
| `INTERNET` | Firebase, Maps |
| `ACCESS_FINE_LOCATION` / `COARSE` | Destination proximity alarm |
| `POST_NOTIFICATIONS` | Alarm notification (API 33+) |
| `VIBRATE` | Alarm feedback |

## Dependencies (summary)

From `app/build.gradle.kts`:

- AndroidX: Core KTX, AppCompat, Material, ConstraintLayout, Activity
- Navigation Fragment/UI 2.8.5
- Play Services: Location 21.3.0, Maps 19.0.0
- Firebase BOM 33.7.0: Auth, Database, Messaging

## Build configuration

| Setting | Value |
|---------|-------|
| `applicationId` | `com.greatingcard.nammarailubuddy` |
| `minSdk` | 24 |
| `compileSdk` / `targetSdk` | 36 |
| View Binding | Enabled |

## Known architectural trade-offs

1. **Large `HomeFragment`** — alarm, location, Firebase, and UI logic in one class; candidate for ViewModel + repository split.
2. **Duplicate Firebase URL** — should live in a single `FirebaseConfig` object or `BuildConfig`.
3. **Mixed live + mock data** — `TrainsFragment` supplements Firebase with hardcoded Karnataka trains for demo richness.
4. **Maps key in manifest** — security risk for public GitHub; move to Gradle secrets.
