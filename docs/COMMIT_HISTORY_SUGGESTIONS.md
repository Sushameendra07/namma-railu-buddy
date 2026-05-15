# Suggested Git Commit Timeline

Use these messages when initializing Git or rewriting history for a **feature-wise narrative**. Commit in chronological order (oldest first).

```text
Initial Android project setup with Kotlin and Gradle

Configure app module, View Binding, and Material theme

Add XML layouts for login, register, and main shell

Implement Firebase project integration and google-services plugin

Add Firebase Authentication email/password flow

Create LoginActivity with session redirect to main app

Create RegisterActivity with validation and progress UI

Implement MainActivity with bottom navigation host

Add HomeFragment with station and train autocomplete

Integrate Firebase Realtime Database for stations and trains

Add Train data model and RecyclerView TrainAdapter

Implement TrainsFragment with search filter and local fallback data

Build coach sequence visualization with type-based styling

Add platform ping crowd verification with Firebase transactions

Implement destination proximity alarm with location services

Add notification channel and vibration for alarm trigger

Create AlertsFragment for delay and platform-change detection

Add AlertAdapter and alerts RecyclerView UI

Implement ProfileFragment with logout and user display

Add MapActivity with Google Maps SDK and station marker

Wire quick actions and cross-tab navigation from Home

Add Karnataka regional station codes and default coordinates

Include day1 Firebase seed JSON for demo stations and trains

Configure release build and fix lint theme parent for assembleRelease

Add Day 5 demo script and manual test matrix documentation

Add comprehensive README, docs, and Android gitignore

Prepare repository for internship submission (secrets excluded)
```

## Tips for evaluators

- **1 feature ≈ 1 commit** when possible; avoid `final project` single commits.
- Use imperative mood: *Add*, *Fix*, *Implement*, not *Added*.
- Reference issue numbers if using GitHub Issues.
- Do not commit `google-services.json`, `local.properties`, or `build/` folders.

## If the repo already exists with messy history

Option A: Keep history, add new commits for docs only (safest).  
Option B: New orphan branch with replayed commits (advanced; only for local portfolio).  
Option C: Squash to 5–8 logical commits with `git rebase` (intermediate).
