# Changelog

All notable changes to Student Toolkit. Versions follow [Semantic Versioning](https://semver.org/);
0.x releases are betas. Download any version from
[GitHub Releases](https://github.com/tehgeii/student-toolkit/releases).

## [0.11.0] — 2026-10-16

### Added
- **Grade calculator per course:** keep as many saved calculations as you like (one per course), pick the course
  from your semesters, and the chosen target grade is remembered. Your old calculator inputs move over automatically.
- **Course code and lecturer** (optional) for every course in a semester.
- **Home screen widget** with your next three deadlines; tap one to open it.
- **Quick Settings tile** to start, pause or resume the Pomodoro from the notification shade.
- **Search** (Home → 🔍) across tasks, notes, courses, lecturers, grade calculations and tools.
- **Developer tools:** number bases (BIN/OCT/DEC/HEX), Unix timestamps, JSON formatter/validator,
  color converter (HEX/RGB/HSL) and a regex tester that stops patterns that would hang.
- **Settings:** score decimal places (1–3), *Check for updates*, privacy policy link, and a **crash report**
  that stays on the phone until you choose to share or delete it.
- Privacy policy ([`docs/PRIVACY.md`](docs/PRIVACY.md)) and Play Store preparation notes ([`docs/PLAY_STORE.md`](docs/PLAY_STORE.md)).

### Changed
- Toolchain: Android Gradle Plugin 9.4, Gradle 9.6, Kotlin 2.4, compileSdk 37, Compose BOM 2026.09.
- Backups use schema v2 (adds saved grade calculations). Older backups still import.
- Light screen cross-fade and list animations; snackbars no longer cover the floating buttons.

### Accessibility
- Section titles are announced as headings, decorative emoji are skipped by TalkBack,
  checkboxes say which task or step they belong to, and timer digits stay inside the dial with very large fonts.

### Developer
- CI runs Android Lint on every push and builds a signed Play Store bundle (`.aab`) for releases.

## [0.10.0] — 2026-10-16

### Added
- Language setting: follow system, English or Bahasa Indonesia — saved and applied instantly, notifications included.
- Custom reminder times (for example "2 hours 15 minutes before").
- Open-source licenses in *Settings → About*.

### Changed
- Deadline reminders are rescheduled every time the app starts, so they return after a force stop or a strict battery saver.
- Only the app's own notification routes are accepted from outside the app.
- Copied Wi-Fi QR content is marked sensitive (hidden from clipboard previews).
- A semester that was added but never filled in is removed again.
- Source-available license (all rights reserved).

## [0.9.0] — 2026-10-09

First public build (MVP): grade calculator with target planner and scenario simulator, IPS/IPK, GPA target planner,
grade converter and grading scale; tasks with deadlines, priorities, steps and reminders; Pomodoro, timer and stopwatch;
group randomizer, presentation order and task assigner; QR generator, unit and percentage converters, notes;
local JSON storage with backup export/import; signed APKs on GitHub Releases.

[0.11.0]: https://github.com/tehgeii/student-toolkit/releases/tag/v0.11.0
[0.10.0]: https://github.com/tehgeii/student-toolkit/releases/tag/v0.10.0
[0.9.0]: https://github.com/tehgeii/student-toolkit/releases/tag/v0.9.0
