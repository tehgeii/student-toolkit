# Student Toolkit

> **Small tools for your everyday student life, right on Android.**
> Aplikasi kecil buatan mahasiswa, untuk kebutuhan mahasiswa.

Student Toolkit is an **Android-only, offline-first** utility app that bundles the small things students do
every day: grade and GPA (IPS/IPK) calculations, deadlines with reminders, Pomodoro, group randomizers,
QR codes, converters and quick notes. No account, no internet permission, no tracking: data stays on the phone.
The app speaks **English and Bahasa Indonesia** — pick one in Settings (the choice is saved).

The full product definition lives in [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md).

## Download

Get the latest signed APK from **[Releases](https://github.com/tehgeii/student-toolkit/releases/latest)**
(Android 8.0+). Open the `.apk` on your phone, allow *Install unknown apps* for your browser or file manager if asked,
then tap **Install**. Later versions update in place.

## Features

| Area | What you get |
|------|--------------|
| **Home** | Greeting, today's date, *Due soon* deadlines, IPK and focus-today cards, quick tools |
| **Academic** | Grade calculator with dynamic components and weight validation (✓ 100%, ⚠ under, ✕ over blocks the result) · score target planner with impossible-goal detection · scenario simulator (slider) · IPS per semester (per course or entered directly) · IPK weighted by SKS · GPA target planner · grade converter · configurable grading scale and grade points |
| **Tasks** | Title, course, deadline, priority, estimated time, steps (subtasks), notes · sections Overdue → Today → Upcoming → No deadline → Completed · sorting (smart, deadline, priority, newest) · reminders (presets from 10 min to 1 week before, or custom such as "2 h 15 min before"; combinable) · rescheduled automatically after reboot, app update or force stop · undo on complete/delete |
| **Focus** | Pomodoro (25/5, 50/10, 90/15, custom; session x of 4; start/pause/resume/reset/skip) with daily and weekly stats · timer with presets and custom duration · stopwatch with laps |
| **Classroom** | Fair group randomizer (number of groups *or* people per group, balanced sizes such as 4+3+3) · presentation order · task assigner that **asks** what to do when people ≠ tasks |
| **Utilities** | QR generator (text, URL, Wi-Fi with password warning, email, phone) with save/share/copy · unit converter (length, weight, temperature, time, SI vs IEC storage, data rate, speed, area, volume) · percentage calculator · notes with autosave and search |
| **Data** | Local storage · export/share/import `StudentToolkitBackup.json` (schema-versioned, validated, merge or replace, safety copy before restore) |
| **Settings** | Language (follow system / English / Bahasa Indonesia), optional profile, System/Light/Dark theme, Material You colors (Android 12+), GPA decimals (2/3), grading scale, notification and precise-alarm status, default reminders, about & open-source licenses — all saved permanently |

## Built to be light

* **Kotlin + Jetpack Compose (Material 3)**, single activity, no XML layouts.
* **Small dependency list:** AndroidX Core, Activity Compose, Lifecycle (runtime/viewmodel compose), Compose UI/Foundation/Material3,
  `material-icons-core`, and [`io.nayuki:qrcodegen`](https://github.com/nayuki/QR-Code-generator) (≈40 KB) for QR codes.
  No Room, no DI framework, no navigation library, no AppCompat, no serialization library, no `material-icons-extended`.
* **Storage:** one JSON file per area (`tasks.json`, `notes.json`, `academic.json`, `focus.json`) written atomically with
  `AtomicFile` on a single background thread, plus `SharedPreferences` for settings. Student-sized data (hundreds of items)
  loads in milliseconds, so a database would only add size and build time.
* **Battery:** no background services and no polling. Timers are computed from timestamps (`elapsedRealtime`) and one
  `AlarmManager` alarm fires at the end; deadline reminders are local alarms rebuilt after reboot.
* **Release build:** R8 minification + resource shrinking, vector-only icons (no PNGs).

## Architecture

```
UI (Compose screens)  →  ViewModel  →  domain (pure Kotlin, unit-tested)  →  repositories  →  local JSON / SharedPreferences
```

```
app/src/main/java/com/tehgeii/studenttoolkit/
├── core/        formatting & input parsing (display-only rounding)
├── domain/      academic, tasks, classroom, tools, focus, notes — no Android imports
├── data/        codecs, atomic JSON stores, repositories, backup (schema v1 + migration hook)
├── i18n/        English / Bahasa Indonesia texts, side by side: t("Tasks", "Tugas")
├── notify/      notification channels, AlarmManager scheduling, receivers, focus engine
└── ui/          navigation, theme, components and one package per feature
```

Formulas are written out in the domain layer, for example
`Final = Σ(score × weight) ÷ 100` and `GPA = Σ(gradePoint × SKS) ÷ Σ SKS` (IPK is always SKS-weighted).

## Android decisions

| Topic | Decision |
|-------|----------|
| Min / target SDK | `minSdk 26` (Android 8.0: notification channels, `java.time`, adaptive icons natively) · `targetSdk 36`, `compileSdk 36` |
| Orientation | Portrait and landscape both supported; state lives in ViewModels / `rememberSaveable`, content is width-limited on tablets |
| Back button | Natural back stack; editors with unsaved changes ask before discarding; notes autosave |
| Process death | Navigation stack, editor forms and calculator drafts are saved; timers resume from stored timestamps (also across reboot) |
| Keyboard | Numeric keyboards for numbers, `imePadding` everywhere, decimal comma accepted (`3,5`) |
| Time zones | Deadlines are stored as instants and shown in the device time zone (nothing hard-coded to Asia/Jakarta) |
| Cloud backup | `allowBackup="false"`: nothing is sent to the cloud automatically; backups are manual |
| Language | In-app choice stored with the other settings; switching is instant (no restart) and also updates notification texts |

### Permissions

| Permission | Why |
|------------|-----|
| `POST_NOTIFICATIONS` | Deadline, Pomodoro and timer alerts. Requested in context, never at startup |
| `RECEIVE_BOOT_COMPLETED` | Alarms are cleared on reboot; reminders are rescheduled from local data |
| `SCHEDULE_EXACT_ALARM` | Optional precise timer alerts on Android 12+. Without it the app falls back to inexact alarms and explains the possible delay |

No internet, camera, storage, contacts or location permissions.

### Security & privacy

* **No network access at all** (no `INTERNET` permission), so grades, tasks and notes cannot leave the phone unless you share a backup yourself.
* Backups are parsed as untrusted data: size limits, schema check, full validation before anything is changed, and a safety copy before restore.
* Only the launcher activity and the boot receiver (system broadcasts only) are exported; alarms use immutable `PendingIntent`s;
  the `FileProvider` exposes just the share cache folder.
* Screens opened from outside the app are limited to the routes the app's own notifications use.
* Wi-Fi QR content (which contains the password) is marked sensitive when copied, so it is hidden from clipboard previews.
* Release APKs are minified with R8 and signed with a key that lives only in GitHub secrets.

## Build

Requirements: JDK 17 and the Android SDK (platform 36). Android Studio works out of the box.

```bash
./gradlew testDebugUnitTest   # unit tests (domain logic, backup validation)
./gradlew assembleDebug       # app/build/outputs/apk/debug/StudentToolkit-v<version>-debug.apk
./gradlew assembleRelease     # minified release APK (unsigned unless a keystore is configured)
```

GitHub Actions (`.github/workflows/android.yml`) runs the tests and builds the APKs on every push and uploads the debug APK
as an artifact.

### Release signing

Signing material is **never** committed. Either create a git-ignored `keystore.properties` in the project root:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

or set `ST_KEYSTORE_PATH`, `ST_KEYSTORE_PASSWORD`, `ST_KEY_ALIAS`, `ST_KEY_PASSWORD`.
GitHub Actions signs releases with the repository secrets `ST_KEYSTORE_BASE64` (the keystore, base64-encoded),
`ST_KEYSTORE_PASSWORD`, `ST_KEY_ALIAS` and `ST_KEY_PASSWORD`. To publish a new version:

1. Bump `appVersionName` / `appVersionCode` in `app/build.gradle.kts` and push to `main`.
2. On GitHub open **Releases → Draft a new release**, type the tag `v1.0.0` (*Create new tag on publish*, target `main`)
   and publish. Pushing the tag from the command line (`git tag v1.0.0 && git push origin v1.0.0`) works too.
3. The workflow checks that the tag matches `appVersionName`, builds and verifies the signed APK, and attaches
   `StudentToolkit-v1.0.0.apk` and its SHA-256 to the GitHub Release.

Keep a private backup of the keystore and its passwords: every update must be signed with the same key.

## Status

**v0.10.0** — feature-complete MVP (roadmap v0.1 – v0.6) plus Bahasa Indonesia, custom reminders and reliability/security hardening.
Domain logic, translations and backup validation are covered by unit tests, and the signed release APK installs and runs on a
real device. Before tagging **v1.0**, finish the spec's device pass: small/large phones and a tablet, the minimum Android version
(8.0), rotation, and notifications with the app closed, after a reboot and after a force stop.

Not in scope for the MVP (by design): accounts, cloud sync, AI features, widgets, Quick Settings tile, iOS or desktop builds.

## License

Student Toolkit is **source-available, not open source**. You may view, download and review the code and use the official
APK for free, but you may **not** copy or modify the code, sell the app or the code, or publish it under another name.
See [`LICENSE`](LICENSE) for the full terms (English, with an Indonesian summary).

Third-party libraries keep their own licenses (Apache 2.0 / MIT); they are listed in the app under
*Settings → About → Open-source licenses*.

---

© 2026–present [tehgeii](https://github.com/tehgeii) (TGI) · [Portfolio](https://tgopremium.my.id) · All rights reserved.
