# Code Quality & Security Recommendations

## Naming conventions

| Current | Suggestion |
|---------|------------|
| Package `greatingcard` | Consider `greetingcard` if typo — only change if rebranding |
| `mMap` in MapActivity | `googleMap` (Kotlin style) |
| Mixed `btn` / `txt` prefixes | Consistent with existing XML IDs — keep for layout binding |

Generally follows Android Kotlin conventions: Activities end with `Activity`, fragments with `Fragment`, adapters with `Adapter`.

## Modularization (priority order)

1. **`FirebaseRepository`** — single class for RTDB URL, `stations`, `platformPings`, `alarmSessions` listeners.
2. **`HomeViewModel`** — extract alarm + location logic from `HomeFragment` (~700 lines).
3. **`constants/FirebasePaths.kt`** — path strings and default station coordinates.
4. **`ui/home/` package** — group Home-specific views/helpers.

## Comments

- Keep comments for **non-obvious** logic (transactions, cooldown, regional DB URL).
- Remove emoji-only section headers before internship submission if evaluators prefer formal style.
- Add KDoc on `Train` and public repository methods if you extract a data layer.

## Reusable components

| Opportunity | Benefit |
|-------------|---------|
| Shared `FirebaseDatabaseProvider` | One RTDB instance, one URL |
| `CoachSequenceView` custom view | Reuse coach strip outside Home |
| `EmptyStateView` | Trains/Alerts empty states |
| Base fragment with `_binding` cleanup pattern | Less boilerplate |

## Performance

- **Detach Firebase listeners** in `onDestroyView` — HomeFragment already does for platform pings; apply same pattern in Trains/Alerts.
- **Limit `addValueEventListener` scope** — TrainsFragment listens to entire `stations` tree; consider querying single station on Home only.
- **Debounce search** in `TrainsFragment` (300ms) to reduce adapter updates while typing.
- **Use `viewLifecycleOwner`** for coroutine/Flow collectors if migrating off raw listeners.

## Firebase security

### Critical (before GitHub)

1. **Remove `google-services.json` from Git** — use `.gitignore` + `google-services.json.example`.
2. **Tighten Realtime Database rules** — see README Firebase section.
3. **Remove debug auth bypass** in production:

```kotlin
// Remove from LoginActivity & RegisterActivity for release:
auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
```

4. **Rotate exposed keys** if manifest Maps key was ever pushed publicly.

### Maps API key

**Current risk:** API key in `AndroidManifest.xml` meta-data.

**Recommended pattern:**

`local.properties`:

```properties
MAPS_API_KEY=AIza...
```

`app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["MAPS_API_KEY"] = project.findProperty("MAPS_API_KEY") as String? ?: ""
    }
}
```

`AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

Restrict key in Google Cloud Console: Android app + SHA-1 + package name.

## Error handling

- Replace generic `Toast` with Snackbar + retry for network failures on Fetch.
- Log `DatabaseError.code` for easier Firebase rule debugging.
- Validate `trainId` non-null before Firebase child paths (already mostly done).

## Testing improvements

- ViewModel unit tests with fake repository for alarm distance logic.
- Espresso tests for login navigation and bottom nav tab switches.
- Firebase Emulator Suite for CI without production data.

## UI polish (evaluation impact)

- Dark theme (`values-night/`)
- Consistent typography from `res/font/`
- Real privacy policy URL in ProfileFragment (currently opens google.com placeholder)
