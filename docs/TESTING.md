# Testing Documentation — Namma Railu Buddy

## Automated tests

### Unit tests

```bash
gradlew.bat testDebugUnitTest
```

Location: `app/src/test/` (JUnit)

### Instrumented tests

```bash
gradlew.bat connectedDebugAndroidTest
```

Location: `app/src/androidTest/` (Espresso)

**Last known status (Day 5):** `connectedDebugAndroidTest` passed 1/1 on physical device `V2205 - 14`.

### Build verification

```bash
gradlew.bat assembleDebug assembleRelease
```

Release build requires lint-clean theme configuration (fixed in Day 5 per `DAY5_TEST_MATRIX.md`).

---

## Manual test matrix

| ID | Area | Steps | Expected |
|----|------|-------|----------|
| T1 | Login success | Valid email/password → Login | Navigate to MainActivity |
| T2 | Login fail | Invalid password | Error toast, stay on login |
| T3 | Register | New user, password ≥ 6 chars | Account created → MainActivity |
| T4 | Station select | Home → pick `SBC` | Train dropdown populates |
| T5 | Train fetch | Select train → Fetch | Result card: ETA, platform, status badge |
| T6 | Platform ping | Verify → enter platform `5` | Success toast; votes increment |
| T7 | Alerts | After ping with platform change | Alert shows platform change line |
| T8 | Alarm arm | Destination `MYS`, radius `3` km → Set Alarm | Status "Armed"; permissions requested |
| T9 | Alarm trigger | Mock location within radius of destination | Notification, vibration, state `triggered` |
| T10 | No data | Station `ZZZ` (empty seed) | No trains; fallback message |
| T11 | Trains search | Type "Chennai" in search | Filtered list |
| T12 | Logout | Profile → Logout | Returns to LoginActivity |
| T13 | Map | Home → Track | Map opens with marker |

Full demo script: [DAY5_DEMO_SCRIPT.md](../DAY5_DEMO_SCRIPT.md)  
Checklist: [DAY5_TEST_MATRIX.md](../DAY5_TEST_MATRIX.md)

---

## Bug testing checklist

### Authentication

- [ ] Empty email/password blocked
- [ ] Weak password (< 6) rejected on register
- [ ] Session persists after app restart
- [ ] Logout clears session

### Firebase / network

- [ ] App handles airplane mode gracefully (fallback messages)
- [ ] RTDB rules deny unauthorized writes (after securing rules)
- [ ] Seed data visible after import

### Location & alarms

- [ ] Permission denied → alarm does not claim "armed" falsely
- [ ] Radius validation: reject < 1 or > 50 km
- [ ] Alarm does not re-trigger after first fire
- [ ] Notification channel created on API 26+

### UI / UX

- [ ] Bottom nav preserves expected tab on back from Map
- [ ] Coach chips show correct colors by type
- [ ] Keyboard does not cover login fields (`adjustResize`)

### Security (pre-release)

- [ ] `google-services.json` not in Git
- [ ] Maps API key restricted by package + SHA-1
- [ ] `setAppVerificationDisabledForTesting` removed
- [ ] Firebase rules require `auth != null`

---

## Test data

Import [day1_firebase_seed.json](../day1_firebase_seed.json) for:

- `SBC` trains including platform ping demo case
- Empty station `ZZZ` for no-data tests
- Sample `alarmSessions/demoUser`

---

## Reporting issues

When filing bugs, include:

1. Device model & Android version  
2. Build variant (debug/release)  
3. Steps to reproduce  
4. Logcat tag filter: `HomeFragment`, `TrainsFragment`, `AlertsFragment`
