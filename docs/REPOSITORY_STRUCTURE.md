# Repository Structure Recommendation

## Ideal public GitHub layout

```text
namma-railu-buddy/
├── .gitignore
├── README.md
├── LICENSE
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── APP_FLOW.md
│   ├── TESTING.md
│   ├── screenshots/
│   └── ...
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── google-services.json.example
│   └── src/
│       ├── main/
│       ├── test/
│       └── androidTest/
├── day1_firebase_seed.json
├── DAY5_DEMO_SCRIPT.md
└── DAY5_TEST_MATRIX.md
```

## Do NOT commit

| Path | Reason |
|------|--------|
| `.idea/` | IDE-specific |
| `.gradle/` | Gradle cache |
| `build/`, `app/build/` | Generated artifacts |
| `local.properties` | SDK path on your machine |
| `app/google-services.json` | Firebase secrets |
| `*.apk`, `*.aab` | Binaries |
| `captures/` | Android Studio screenshots cache |
| `*.keystore` (except debug if needed) | Signing secrets |

## Optional additions

| File | Purpose |
|------|---------|
| `.github/workflows/android.yml` | CI build on push |
| `LICENSE` | MIT / Apache-2.0 |
| `CONTRIBUTING.md` | Contributor guidelines |
| `CHANGELOG.md` | Version history |

## Before first push

```bash
# From project root (after git init)
git add .
git status   # verify no google-services.json, build/, .idea/
git commit -m "Add comprehensive README, docs, and Android gitignore"
```

If `google-services.json` was ever committed, rotate Firebase keys and remove from history with `git filter-repo` or BFG.
