# Day 5 Test Matrix Checklist

## Automated Evidence
- [PASS] `assembleDebug` completed successfully.
- [PASS] `assembleRelease` completed successfully after lint-fix.
- [PASS] `installDebug` completed; app installed on `V2205 - 14`.
- [PASS] `connectedDebugAndroidTest` passed (1/1 on `V2205 - 14`).

## Functional Matrix
- [MANUAL PENDING] Login success/fail
  - Steps: Use valid and invalid credentials on login screen.
  - Expected: valid login navigates to app; invalid login shows error and stays on login.
- [MANUAL PENDING] Station/train selection
  - Steps: Select station `SBC`, choose `Gol Gumbaz Express`, tap Fetch.
  - Expected: train card populated with source/destination/platform/ETA.
- [MANUAL PENDING] Platform ping live update
  - Steps: Submit ping for `SBC_SBC_T02` as platform `5`.
  - Expected: success toast; platform reflects latest ping; votes increment.
- [MANUAL PENDING] Alarm trigger
  - Steps: Set destination `MYS`, trigger radius `3`, allow permissions, use mock location near destination.
  - Expected: alarm state becomes triggered with notification/vibration.
- [MANUAL PENDING] No-data fallback
  - Steps: Select seeded station `ZZZ` (no trains).
  - Expected: no train options and fallback text shown.

## Seed Data Prepared
- Updated `day1_firebase_seed.json` with:
  - Deterministic alert case (`SBC_T02`: delayed + platform mismatch 4 -> 5).
  - `platformPings.SBC_SBC_T02` strong live value (`latestPlatform=5`, `count=21`).
  - Empty station `ZZZ` for no-data fallback demo.
  - Triggered alarm session example under `alarmSessions.demoUser`.
  - Lat/long coordinates on key stations for reliable alarm-distance behavior.
