# Day 5 Demo Script (3-5 Minutes)

## 0) Pre-demo setup (30-45 sec)
1. Install latest debug build: `./gradlew installDebug`.
2. Confirm Firebase seed is loaded from `day1_firebase_seed.json`.
3. Launch app and sign in with a known demo user.

## 1) Login success/fail (40-50 sec)
1. On login screen, enter valid demo credentials and tap Login.
2. Expected: user enters main app successfully.
3. Sign out (if available) or relaunch login screen.
4. Enter invalid password and tap Login.
5. Expected: error message shown, login denied.

## 2) Station/train selection (35-45 sec)
1. Go to Home.
2. Select station `SBC` from station dropdown.
3. Select train `Gol Gumbaz Express`.
4. Tap Fetch/Live status.
5. Expected: result card appears with train info (destination, ETA, platform, status).

## 3) Platform ping live update (45-60 sec)
1. Keep `SBC` + `Gol Gumbaz Express` selected.
2. Tap Verify/Submit platform ping and enter platform `5`.
3. Expected immediately: success toast.
4. Expected shortly: platform shown as `5` and reliability vote count increases.
5. Optional: open Alerts tab to show platform change alert (`4 -> 5`).

## 4) Alarm trigger path (50-70 sec)
1. In Home, set destination station to `MYS`.
2. Enter trigger radius `3` km and tap Set Alarm.
3. Grant location/notification permissions when prompted.
4. For demo speed, use mocked location near destination.
5. Expected: alarm status moves to Triggered, notification/vibration fires, and session state updates to `triggered`.

## 5) No-data fallback (30-40 sec)
1. Select station `ZZZ` (seeded empty station).
2. Open train dropdown / try fetch.
3. Expected: no trains listed and fallback message shown (`No live data, showing last update`).

## 6) Close (20-30 sec)
1. Mention build readiness: debug + release assemble successful.
2. Mention only Day 5 bug fix: release lint blocker in theme style parent.
