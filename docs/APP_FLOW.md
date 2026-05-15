# Application Flow — Namma Railu Buddy

## 1. Cold start

```
App launch
    → LoginActivity (launcher)
        → if FirebaseAuth.currentUser != null
              → MainActivity (skip login)
        → else show login form
```

## 2. Authentication flow

### Registration

```
RegisterActivity
    → User enters name, email, password (≥ 6 chars)
    → FirebaseAuth.createUserWithEmailAndPassword()
    → on success → MainActivity, finish RegisterActivity
    → on failure → Toast with error message
```

### Login

```
LoginActivity
    → User enters email, password
    → FirebaseAuth.signInWithEmailAndPassword()
    → on success → MainActivity
    → "Go to Register" → RegisterActivity (finish Login)
```

### Logout

```
ProfileFragment → btnLogout
    → FirebaseAuth.signOut()
    → LoginActivity + finish MainActivity
```

### Debug note

`setAppVerificationDisabledForTesting(true)` is enabled in Login/Register — **remove for production** builds.

## 3. Main app navigation

```
MainActivity.onCreate()
    → Default fragment: HomeFragment
    → BottomNavigationView listener:
          Home     → HomeFragment
          Trains   → TrainsFragment
          Alerts   → AlertsFragment
          Profile  → ProfileFragment
```

Each selection calls `replaceFragment()` on `R.id.nav_host_fragment`.

## 4. Home — train status flow

```
Select station (AutoComplete) 
    → loadTrainsForStation() from Firebase stations/{code}/trains
Select train
    → load coach sequence from Train.coachPosition
Tap Fetch / Live action
    → refreshTrainStatus()
    → enrichWithPlatformPing() listens to platformPings/{station}_{trainId}
    → updateUIWithTrainData()
```

**Fallback:** If Firebase fails, local station list (SBC, YPR, MYS, …) and status message *"No live data, showing last update"*.

## 5. Platform ping (crowd verification)

```
User taps Verify
    → Dialog for platform number
    → Cooldown check (15s per station+train in SharedPreferences)
    → Firebase transaction on platformPings/{stationId}_{trainId}
    → Increments count, sets latestPlatform, lastUpdated, lastUserId (session UUID)
    → Home UI updates platform + vote count via ValueEventListener
```

## 6. Destination alarm flow

```
User selects destination station + trigger radius (1–50 km)
    → saveAlarmConfig()
    → Write alarmSessions/{uid}/{alarmId} with state "armed"
    → Request location + notification permissions
    → startLocationPolling() every ~20s
    → When distance to destination ≤ triggerRadiusKm:
          → Notification + vibration
          → state → "triggered" in Firebase
          → alarmTriggered = true (stops polling)
```

Station coordinates come from Firebase `stations/{code}/latitude|longitude` or `defaultStationCoordinates()` fallback map.

## 7. Trains tab flow

```
TrainsFragment.onViewCreated()
    → loadProfessionalDatabase() (immediate local list)
    → setupFirebase() ValueEventListener on stations/
    → Merge Firebase trains when available
    → Search filters list by name or destination
```

## 8. Alerts tab flow

```
AlertsFragment
    → Listen to stations/
    → Fetch platformPings/
    → rebuildAlerts(): include trains where:
          - delay != "on time", OR
          - latest platform ping differs from scheduled platform
    → If empty → showFallbackAlerts() demo row
```

## 9. Map flow

```
HomeFragment → btnTrack → MapActivity
    → SupportMapFragment async
    → onMapReady: marker at Bengaluru City (12.9783, 77.5693) — demo/mock
    → FAB back → finish()
```

## 10. External services

| Service | Endpoint / SDK | Used for |
|---------|------------------|----------|
| Firebase Auth | SDK | Login, register, logout |
| Firebase RTDB | `nammarailubuddy-default-rtdb.asia-southeast1...` | Stations, trains, pings, alarms |
| Google Maps | Maps SDK | MapActivity |
| Fused Location | Play Services Location | Proximity alarm |
| (Future) FCM | Firebase Messaging | Push notifications (dependency present) |

No REST API layer is implemented in v1.0; all dynamic data is Firebase RTDB.
