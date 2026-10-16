# Publishing on Google Play — checklist

Everything the app needs for Google Play is ready in this repository. The steps that need **your** Google account,
payment or identity can only be done by you in the Play Console. Ringkasan Bahasa Indonesia ada di bagian bawah.

## 1. What is already prepared

| Item | Where |
|------|-------|
| Signed **App Bundle** (`.aab`) | Built by the *Signed release APK* job for every release. Open the run in the **Actions** tab → *Artifacts* → `StudentToolkit-vX.Y.Z-play-bundle` (kept 90 days). |
| Package name | `com.tehgeii.studenttoolkit` (cannot change after the first upload) |
| Target API | `targetSdk 36` (meets the Play requirement for new apps and updates in 2026) |
| Privacy policy URL | `https://github.com/tehgeii/student-toolkit/blob/main/docs/PRIVACY.md` |
| Permissions | Notifications, boot completed, optional exact alarms — no Internet, location, camera, contacts or storage |
| Ads / in-app purchases | None |

## 2. What you need to do

1. **Developer account** — create one at <https://play.google.com/console> (one-time fee of US$25, identity verification).
2. **Personal accounts created after November 2023** must run a **closed test with at least 12 testers for 14 days**
   before production access is granted. Friends or classmates can join through an opt-in link.
3. **Create the app:** name *Student Toolkit*, default language Indonesian (or English), *App*, *Free*.
4. **App signing (important):** when the Console sets up Play App Signing, choose to change the app signing key and pick
   the option to **export and upload a key from a Java keystore**, using the same keystore as the GitHub releases
   (the Console guides you through Google's PEPK tool). Then Play and GitHub APKs
   have the same signature and users can switch between them without uninstalling. If you let Google generate a new
   key instead, Play builds and GitHub builds can no longer update each other.
5. Upload the `.aab` from step 1 to *Testing → Closed testing* (later *Production*).
6. Fill in *App content* with the answers in section 3, and the store listing with section 4.
7. Add graphics: app icon 512 × 512 PNG, feature graphic 1024 × 500, and at least 2 phone screenshots
   (take them on your phone: Home, Grade calculator, Tasks, Pomodoro, Tools).

Every later release: bump the version, publish the GitHub release as usual, then upload that release's `.aab` artifact.

## 3. App content answers

| Question | Answer |
|----------|--------|
| Privacy policy | URL above |
| Ads | No, my app does not contain ads |
| App access | All functionality is available without special access (no login) |
| Content rating (IARC) | Category *Utility, Productivity, Communication or Other*; answer **No** to every content question → rated *Everyone / 3+* |
| Target audience | 18 and over (university students). Choosing ages under 13 would add Families-policy requirements |
| News app | No |
| Data safety → Does your app collect or share user data? | **No** (nothing leaves the device; the app has no Internet permission) |
| Data safety → Is data encrypted in transit? | Not applicable (no data is transmitted) |
| Data safety → Can users request deletion? | Data is stored only on the device; uninstalling or *Clear storage* deletes it |
| Government app / financial features / health | No |
| Exact alarm permission | `SCHEDULE_EXACT_ALARM` is optional and user-granted (no declaration form is needed; `USE_EXACT_ALARM` is not used) |

## 4. Store listing text

**App name:** Student Toolkit

**Short description (EN, ≤ 80):** Grades, GPA, deadlines, Pomodoro & handy tools for students — fully offline.

**Short description (ID, ≤ 80):** Nilai, IPK, deadline, Pomodoro & alat praktis mahasiswa — offline, tanpa akun.

**Full description (ID):**

```
Student Toolkit adalah kumpulan alat kecil untuk kehidupan mahasiswa sehari-hari — ringan, cepat, dan 100% offline.

📊 AKADEMIK
• Kalkulator nilai per mata kuliah: bobot komponen, nilai akhir, target nilai, dan simulasi skenario
• IPS per semester dan IPK berbobot SKS, target IPK, konversi angka ke huruf
• Skala nilai bisa disesuaikan dengan kampusmu

✅ TUGAS & DEADLINE
• Deadline, prioritas, langkah-langkah, catatan, dan pengingat (bisa beberapa sekaligus)
• Widget layar utama berisi 3 deadline terdekat

🍅 FOKUS
• Pomodoro, timer, dan stopwatch dengan lap — plus tile Quick Settings

🎲 KELAS & UTILITAS
• Acak kelompok yang adil, urutan presentasi, pembagian tugas
• Pembuat QR (teks, URL, Wi-Fi, email, telepon), konversi satuan, persentase, catatan
• Alat developer: basis bilangan, timestamp Unix, perapi JSON, konversi warna, uji regex

🔒 PRIVASI
• Tanpa akun, tanpa iklan, tanpa izin Internet — semua data tetap di HP-mu
• Backup manual ke file yang kamu pilih
• Bahasa Indonesia dan English
```

**Full description (EN):**

```
Student Toolkit bundles the small tools students use every day — light, fast and 100% offline.

📊 ACADEMIC
• Grade calculator per course: weighted components, final score, target score and scenarios
• Semester GPA (IPS) and cumulative GPA (IPK) weighted by credits, GPA target planner, score-to-letter converter
• Grading scale you can match to your campus

✅ TASKS & DEADLINES
• Deadlines, priorities, steps, notes and multiple reminders
• Home screen widget with your next three deadlines

🍅 FOCUS
• Pomodoro, timer and stopwatch with laps — plus a Quick Settings tile

🎲 CLASSROOM & UTILITIES
• Fair group randomizer, presentation order, task assigner
• QR generator (text, URL, Wi-Fi, email, phone), unit and percentage converters, notes
• Developer tools: number bases, Unix timestamps, JSON formatter, color converter, regex tester

🔒 PRIVACY
• No account, no ads, no Internet permission — your data stays on your phone
• Manual backups to a file you choose
• English and Bahasa Indonesia
```

**Category:** Education · **Tags:** Education, Productivity · **Contact email:** your public developer email.

---

## Ringkasan (Bahasa Indonesia)

1. Buat akun developer di Play Console (biaya sekali US$25 + verifikasi identitas).
2. Akun pribadi baru wajib **closed testing minimal 12 penguji selama 14 hari** sebelum bisa rilis ke publik.
3. Saat diminta *App signing*, **unggah keystore yang sama** dengan rilis GitHub, supaya APK dari Play dan GitHub bisa saling update.
4. Unggah file `.aab` dari tab **Actions** → run rilis → *Artifacts*.
5. Isi *App content* dan *Store listing* dengan jawaban & teks di atas, lalu tambahkan ikon 512×512, feature graphic 1024×500, dan minimal 2 screenshot dari HP.
