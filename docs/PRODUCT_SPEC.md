# Student Toolkit

> **A lightweight all-in-one Android utility suite for everyday student needs.**

Student Toolkit adalah aplikasi **Android-only** yang dirancang untuk membantu mahasiswa menyelesaikan berbagai kebutuhan kecil namun berulang dalam kehidupan perkuliahan.

Aplikasi ini menggabungkan berbagai utility praktis dalam satu aplikasi Android, seperti:

- kalkulator nilai
- kalkulator IPS / IPK
- perhitungan nilai minimum
- GPA / IPK target planner
- deadline tracker
- task planner
- Pomodoro
- randomizer kelompok
- randomizer presentasi
- pembagian tugas
- QR generator
- unit converter
- percentage calculator
- timer
- stopwatch
- catatan sederhana
- dan utility mahasiswa lainnya

Tujuan utamanya bukan membuat sistem akademik universitas.

Tujuan utamanya adalah:

> **Membuat pekerjaan kecil mahasiswa menjadi lebih cepat, sederhana, dan praktis langsung dari HP Android.**

---

# 1. PLATFORM TARGET — SANGAT PENTING

Student Toolkit adalah aplikasi:

> **KHUSUS ANDROID.**

Target utama:

```text
Android Smartphone
Android Tablet
```

Format distribusi utama:

```text
.apk
```

Aplikasi **TIDAK dibuat untuk Windows desktop**.

Aplikasi **TIDAK dibuat untuk macOS**.

Aplikasi **TIDAK dibuat untuk Linux**.

Aplikasi **TIDAK mendukung iPhone / iOS**.

Aplikasi ini bukan:

```text
Windows EXE
Windows Desktop App
macOS App
Linux App
iOS App
iPhone App
```

Fokus pengembangan harus sepenuhnya pada pengalaman Android.

---

# 2. Distribution Strategy

Distribusi utama:

> **Android APK**

Contoh:

```text
StudentToolkit-v1.0.0.apk
```

Untuk development:

```text
StudentToolkit-debug.apk
```

Untuk release:

```text
StudentToolkit-release.apk
```

Future distribution dapat menggunakan Google Play Store apabila diperlukan.

Namun untuk tahap awal, aplikasi dapat didistribusikan secara langsung melalui:

- APK
- website portfolio
- GitHub Release
- Google Drive
- platform distribusi APK yang sesuai

---

# 3. Kenapa Android?

Mahasiswa jauh lebih sering membawa HP daripada laptop.

Kebutuhan seperti:

- melihat deadline
- menghitung nilai
- menggunakan Pomodoro
- mengacak kelompok
- membuat QR
- mencatat sesuatu
- mengecek IPK

dapat terjadi kapan saja.

Oleh karena itu:

> **Student Toolkit harus dirancang sebagai utility yang selalu tersedia di HP mahasiswa.**

Contoh:

```text
Dosen tiba-tiba bertanya:
"Kalau nilai UAS kamu 85, nilai akhirnya berapa?"

Buka Student Toolkit.
Hitung.
Selesai.
```

Atau:

```text
Teman:
"Acak kelompok dong."

Buka Student Toolkit.
Randomize.
Selesai.
```

---

# 4. Android-First Philosophy

Jangan membuat aplikasi desktop kemudian "dipindahkan" ke Android.

Student Toolkit harus:

> **Android-first dari awal.**

Artinya UI, navigation, interaction, storage, notification, permission, dan lifecycle harus mengikuti kebiasaan penggunaan Android.

---

# 5. Android UX Principles

Aplikasi harus memperhatikan:

- touch interface
- responsive layout
- berbagai ukuran layar
- portrait orientation
- landscape apabila diperlukan
- Android back button
- keyboard / soft keyboard
- safe areas
- notification
- app lifecycle
- permission
- battery usage
- screen density

Jangan membuat UI yang terasa seperti aplikasi Windows yang diperkecil.

---

# 6. Target Android Version

Target Android version harus ditentukan berdasarkan kebutuhan framework yang digunakan.

AI developer harus:

1. memeriksa versi Android SDK yang digunakan;
2. memilih minimum supported Android version secara realistis;
3. menggunakan API modern ketika tersedia;
4. menyediakan fallback jika dibutuhkan.

Jangan mengklaim kompatibilitas Android tertentu tanpa benar-benar menguji.

---

# 7. Architecture

Teknologi aktual boleh dipilih berdasarkan kebutuhan project.

Contoh opsi:

```text
Native Android
Kotlin
Jetpack Compose
```

atau framework Android-compatible lain jika ada alasan kuat.

Yang paling penting:

> aplikasi harus menghasilkan APK Android native/Android-compatible yang stabil dan mudah dipelihara.

Jangan memilih framework hanya karena terlihat modern.

---

# 8. Product Definition

Student Toolkit adalah:

> **Personal Student Utility Suite for Android**

Konsep:

```text
                    STUDENT TOOLKIT
                           │
          ┌────────────────┼────────────────┐
          │                │                │
       Academic       Productivity       Classroom
          │                │                │
     Grade Calc          Tasks            Groups
     GPA / IPK          Deadline          Randomizer
     Score Planner      Pomodoro           Presenter
          │                │                │
          └────────────────┼────────────────┘
                           │
                        Utilities
                           │
             ┌─────────────┼─────────────┐
             │             │             │
             QR        Converter       Timer
             │             │             │
             └─────────────┼─────────────┘
                           │
                       Notes / Data
```

---

# 9. Masalah yang Ingin Diselesaikan

Mahasiswa sering membutuhkan berbagai alat kecil:

> "Berapa nilai UAS yang harus saya dapat?"

> "IP semester ini berapa?"

> "Kalau target IPK 3,5, semester depan harus dapat berapa?"

> "Tugas ini deadline kapan?"

> "Bagi kelompok secara acak."

> "Siapa yang presentasi duluan?"

> "Timer belajar 25 menit."

> "Buat QR Wi-Fi."

> "Berapa persen 78 dari 100?"

> "Catat sesuatu sebentar."

Biasanya semua dilakukan menggunakan:

- kalkulator
- browser
- website randomizer
- Google Keep / notes
- timer
- spreadsheet
- aplikasi lain

Student Toolkit bertujuan membuat semuanya tersedia dalam satu aplikasi Android.

---

# 10. Positioning

Student Toolkit adalah:

> **An everyday student utility app for Android.**

Bukan:

- LMS
- aplikasi kampus resmi
- aplikasi absensi
- social media
- chat app
- online classroom
- full project-management app
- Notion clone
- AI tutor penuh
- cloud storage
- calendar replacement

---

# 11. Target Users

## Primary

Mahasiswa perguruan tinggi.

## Secondary

- siswa SMA/SMK
- mahasiswa baru
- organisasi mahasiswa
- tutor
- kelompok tugas
- dosen yang membutuhkan utility sederhana

---

# 12. Main Navigation

Mobile navigation harus sederhana.

Recommended:

```text
Home
Academic
Tasks
Tools
Settings
```

atau bottom navigation:

```text
┌───────────────────────────────────┐
│                                   │
│           APP CONTENT             │
│                                   │
├───────────────────────────────────┤
│ Home | Academic | Tasks | Tools  │
└───────────────────────────────────┘
```

Gunakan navigation Android yang natural.

Jangan membuat sidebar desktop-style sebagai navigasi utama jika tidak diperlukan.

---

# 13. Home Dashboard

Home harus menjadi pusat aplikasi.

Contoh:

```text
Good afternoon 👋

Today
8 October 2026

──────────────

Due Soon
⚠ Basis Data
2 days left

🟡 PBO
5 days left

──────────────

Academic
GPA
3.67

──────────────

Quick Tools

[ Grade ]
[ GPA ]
[ Pomodoro ]
[ Randomizer ]
[ QR ]
```

Tujuan dashboard:

> User dapat langsung melihat apa yang penting hari ini dan mengakses utility yang sering dipakai.

---

# 14. Quick Actions

Home menyediakan shortcut ke fitur populer:

```text
Calculate Grade
Calculate GPA
Add Task
Start Pomodoro
Randomize Group
Generate QR
```

Quick Actions dapat diubah berdasarkan penggunaan di masa depan.

---

# 15. Academic Module

Fitur utama:

```text
Grade Calculator
GPA / IPS Calculator
IPK Calculator
Score Target Planner
Grade Converter
Academic History
```

---

# 16. Grade Calculator

User dapat membuat komponen penilaian.

Contoh:

```text
Attendance
10%

Assignment
20%

Midterm
30%

Final
40%
```

Nilai:

```text
Attendance
100

Assignment
85

Midterm
78

Final
90
```

Output:

```text
Final Score
87.4
```

---

# 17. Dynamic Grade Components

User dapat:

- tambah komponen
- hapus komponen
- edit komponen
- mengubah bobot

Contoh:

```text
+ Add Component
```

---

# 18. Weight Validation

Total bobot harus divalidasi.

Jika:

```text
Total = 100%
```

tampilkan:

```text
✓ Valid
```

Jika:

```text
Total = 90%
```

tampilkan:

```text
⚠ Total weight is 90%.
```

Jika:

```text
Total = 120%
```

jangan izinkan kalkulasi final.

---

# 19. Grade Scale

Default grading scale dapat berupa:

```text
A
AB
B
BC
C
D
E
```

Namun grading scale harus configurable.

Contoh:

```text
A  = 85–100
AB = 80–84
B  = 75–79
BC = 70–74
C  = 60–69
D  = 50–59
E  = 0–49
```

Jangan menganggap semua kampus menggunakan sistem yang sama.

---

# 20. Grade Converter

Input:

```text
Score: 84
```

Output:

```text
Grade: AB
```

Tampilkan range yang digunakan.

---

# 21. Score Target Planner

Ini merupakan salah satu fitur utama.

Contoh:

```text
Assignment
20% → 90

Midterm
30% → 78

Final
50% → ?
```

Target:

```text
A
```

Output:

```text
Required Final Score:
86
```

---

# 22. Impossible Goal Detection

Jika target tidak mungkin:

```text
Current score requires:

104 / 100

Target A cannot be reached.
```

Jangan memberikan hasil negatif atau angka tidak masuk akal sebagai recommendation biasa.

---

# 23. Scenario Simulator

User dapat melihat skenario:

```text
Final = 70
Final Score = 74

Final = 80
Final Score = 79

Final = 90
Final Score = 84
```

Ini dapat menggunakan slider Android.

---

# 24. GPA / IPS Calculator

User memasukkan:

```text
Programming
3 SKS
A

Database
3 SKS
AB

Statistics
2 SKS
B
```

Output:

```text
Total SKS
8

IPS
3.63
```

---

# 25. GPA Point Scale

Default:

```text
A  = 4.00
AB = 3.50
B  = 3.00
BC = 2.50
C  = 2.00
D  = 1.00
E  = 0.00
```

Tetapi configurable.

---

# 26. IPK Calculator

User memasukkan semester.

Contoh:

```text
Semester 1
IPS 3.40
SKS 20

Semester 2
IPS 3.70
SKS 22

Semester 3
IPS 3.60
SKS 21
```

Output:

```text
Cumulative GPA
3.57
```

Perhitungan harus menggunakan bobot SKS.

Jangan sekadar:

```text
(3.40 + 3.70 + 3.60) / 3
```

jika SKS semester berbeda.

---

# 27. GPA Goal Planner

User:

```text
Current GPA
3.42

Completed Credits
80

Remaining Credits
60

Target GPA
3.50
```

Aplikasi menghitung target rata-rata nilai pada sisa SKS.

Jika target tidak mungkin:

```text
Target cannot be reached
under the current assumptions.
```

---

# 28. Academic History

Data semester dapat disimpan.

Contoh:

```text
Semester 1
GPA 3.40
20 SKS

Semester 2
GPA 3.70
22 SKS
```

User dapat membuka detail semester.

---

# 29. Academic Data Privacy

Nilai dan data akademik adalah data pribadi.

Default:

> **data disimpan lokal di perangkat.**

Jangan upload nilai ke server tanpa alasan yang jelas.

Tidak membutuhkan account untuk fitur dasar.

---

# 30. Productivity Module

Fitur:

```text
Tasks
Deadlines
Pomodoro
Focus
```

---

# 31. Task Manager

Task memiliki:

```text
Title
Course
Deadline
Priority
Estimated Time
Notes
Completed
```

Contoh:

```text
Tugas Basis Data
Course: Basis Data
Deadline: 10 Oct
Priority: High
Estimated: 4 hours
```

---

# 32. Task List

Contoh:

```text
TODAY

□ Finish ERD
□ Complete report

UPCOMING

□ PBO assignment
□ Presentation slides
```

---

# 33. Task Sorting

Default sorting dapat mempertimbangkan:

```text
Overdue
↓
Due Today
↓
Deadline
↓
Priority
```

User dapat mengganti sorting.

---

# 34. Task Completion

Task dapat ditandai:

```text
✓ Completed
```

Completed task dapat tetap tersedia dalam history.

---

# 35. Deadline Tracker

Status:

```text
DUE TODAY
1 DAY LEFT
DUE SOON
UPCOMING
OVERDUE
COMPLETED
```

Contoh:

```text
⚠ Database Assignment
Due in 2 days
```

---

# 36. Deadline Notifications

Karena aplikasi Android, notification merupakan fitur yang sangat berguna.

User dapat mengaktifkan:

```text
Reminder:
1 day before

3 hours before
```

atau:

```text
Custom
```

Notification harus menggunakan mekanisme Android yang sesuai.

---

# 37. Android Notification Requirements

Developer harus mempertimbangkan:

- Android notification permission
- notification channels
- battery optimization
- exact vs inexact alarms
- timezone changes
- app restart
- device reboot jika reminder persisten diperlukan

Jangan menjanjikan reminder yang selalu muncul tanpa menguji perilaku di berbagai versi Android.

---

# 38. Offline Reminder

Task dan deadline harus tetap dapat disimpan tanpa internet.

Reminder harus sebisa mungkin menggunakan local Android scheduling.

Tidak membutuhkan server untuk reminder sederhana.

---

# 39. Estimated Time

User dapat memasukkan:

```text
Estimated time:
3h 30m
```

Dashboard dapat menampilkan:

```text
Deadline:
Tomorrow

Estimated Work:
3h 30m
```

---

# 40. Task Breakdown

Task dapat memiliki subtask.

Contoh:

```text
Project Website
├── Requirement
├── Database
├── Backend
├── Frontend
└── Documentation
```

Jangan membuat full project management system.

---

# 41. Pomodoro

Default:

```text
25 minutes focus
5 minutes break
```

Presets:

```text
25 / 5
50 / 10
90 / 15
Custom
```

---

# 42. Pomodoro Screen

Contoh:

```text
      FOCUS

      24:32

   [ Pause ]

Session 2 of 4
```

UI harus sangat sederhana saat timer aktif.

---

# 43. Pomodoro Controls

Support:

```text
Start
Pause
Resume
Reset
Skip
```

---

# 44. Focus Statistics

Contoh:

```text
Today

4 sessions
1h 40m focused
```

Data dapat disimpan lokal.

---

# 45. Android Lifecycle

Timer harus mampu menangani:

- app masuk background
- screen lock
- rotation
- process recreation
- notification
- returning to app

Jangan mengandalkan UI timer yang hanya menghitung update setiap detik.

Gunakan elapsed time yang benar.

---

# 46. Classroom Module

Fokus:

> group / presentation / task randomization.

---

# 47. Group Randomizer

Input:

```text
Andi
Budi
Citra
Dimas
Eka
Fajar
Gilang
Hana
```

Pilih:

```text
3 groups
```

Output:

```text
Group 1
Andi
Fajar
Hana

Group 2
Budi
Citra
Gilang

Group 3
Dimas
Eka
```

---

# 48. Group Randomizer Modes

Support:

```text
Number of groups
```

atau:

```text
People per group
```

Jika distribusi tidak rata:

```text
8 people
3 groups

Result:
3 + 3 + 2
```

Tampilkan informasi tersebut agar tidak membingungkan.

---

# 49. Re-Randomize

Button:

```text
Randomize Again
```

Setiap klik menghasilkan random arrangement baru.

---

# 50. Fair Randomization

Randomizer harus:

- menerima seluruh peserta valid
- tidak menghilangkan peserta
- tidak menggandakan peserta secara tidak sengaja
- membagi secara seimbang apabila memungkinkan
- menggunakan random source yang sesuai

---

# 51. Presenter Randomizer

Input:

```text
Andi
Budi
Citra
Dimas
```

Output:

```text
1. Citra
2. Andi
3. Dimas
4. Budi
```

---

# 52. Task Randomizer

Contoh:

```text
People:
Andi
Budi
Citra

Tasks:
Frontend
Backend
Documentation
```

Output:

```text
Andi → Backend
Budi → Documentation
Citra → Frontend
```

Jika jumlah tidak sama:

```text
5 people
3 tasks
```

user harus memilih aturan:

```text
Allow repeated tasks
OR
Leave some people unassigned
```

Jangan menebak sendiri.

---

# 53. Utilities Module

Fitur:

```text
QR Generator
Unit Converter
Percentage Calculator
Timer
Stopwatch
Notes
```

---

# 54. QR Generator

Support:

```text
Text
URL
Wi-Fi
Email
Phone
```

---

# 55. Wi-Fi QR

Input:

```text
SSID
Password
Security
Hidden Network
```

Generate QR.

User dapat:

```text
Save Image
Share Image
Copy
```

Ini sangat cocok untuk penggunaan langsung dari HP.

---

# 56. QR Privacy

QR Wi-Fi mengandung password.

Aplikasi harus memperingatkan user:

> **This QR code contains your Wi-Fi password. Share carefully.**

Default processing lokal.

Tidak mengupload QR content.

---

# 57. Android Share Sheet

Karena aplikasi Android, hasil QR sebaiknya dapat dibagikan melalui Android share sheet.

Contoh:

```text
Share
→ WhatsApp
→ Telegram
→ Files
→ Nearby Share / Quick Share
→ etc.
```

Jangan bergantung pada satu aplikasi tertentu.

---

# 58. Unit Converter

Categories:

```text
Length
Weight
Temperature
Time
Storage
Data
Speed
Area
Volume
```

---

# 59. Data Storage Converter

Support:

```text
SI / Decimal
IEC / Binary
```

Contoh:

```text
1 GB
=
1000 MB
```

dan:

```text
1 GiB
=
1024 MiB
```

Gunakan istilah satuan yang benar supaya tidak membingungkan.

---

# 60. Temperature Converter

Support:

```text
Celsius
Fahrenheit
Kelvin
```

---

# 61. Percentage Calculator

Support:

```text
Percentage of value
Percentage difference
Discount
Increase
Decrease
```

Contoh:

```text
20% of 150
= 30
```

---

# 62. Timer

General timer:

```text
1 minute
5 minutes
30 minutes
Custom
```

Controls:

```text
Start
Pause
Resume
Reset
```

Timer dapat mengirim Android notification ketika selesai.

---

# 63. Stopwatch

Support:

```text
Start
Pause
Resume
Reset
Lap
```

---

# 64. Notes

Simple notes.

Features:

```text
Title
Content
Created Date
Updated Date
Search
Delete
```

Jangan mencoba menjadi Notion.

---

# 65. Android Keyboard Handling

Input screen harus memperhatikan:

- soft keyboard
- keyboard resize
- focus
- scrolling
- numeric keyboard untuk numeric input
- text keyboard untuk notes

Untuk angka:

> gunakan numeric keyboard apabila sesuai.

---

# 66. Android Back Button

Back behavior harus natural.

Contoh:

```text
Tool Screen
↓
Back
↓
Previous Screen
```

Saat user sedang mengedit sesuatu:

> jangan langsung kehilangan data tanpa peringatan.

---

# 67. Screen Rotation

Jika portrait/landscape didukung, data state tidak boleh hilang.

Jika aplikasi memang ditargetkan portrait-only:

> keputusan tersebut harus disengaja dan didokumentasikan.

Jangan mengunci orientation hanya untuk menghindari bug.

---

# 68. Responsive Layout

Harus mendukung setidaknya:

- small Android phone
- normal phone
- large phone
- tablet jika feasible

UI jangan mengandalkan resolusi tertentu.

---

# 69. Dark / Light Theme

Support:

```text
System
Light
Dark
```

UI harus mengikuti Android design conventions.

---

# 70. Material Design

Jika menggunakan Jetpack Compose / Material, gunakan komponen yang konsisten.

Contoh:

- cards
- buttons
- bottom navigation
- dialogs
- bottom sheets
- snackbars
- date picker
- time picker

Tetap hindari UI yang terlalu ramai.

---

# 71. Home Screen Widgets

Future optional feature:

Android home-screen widget untuk:

```text
Upcoming deadline
Pomodoro
Today's task
GPA
```

Bukan bagian MVP.

---

# 72. Quick Settings

Future optional:

Android Quick Settings tile:

> Start Pomodoro

atau:

> Start Timer

Tetapi jangan dibangun sebelum core application stabil.

---

# 73. Local Data Storage

Data harus disimpan secara lokal.

Jenis data:

```text
Academic
Tasks
Deadlines
Notes
Pomodoro Statistics
Settings
```

Teknologi storage dapat dipilih sesuai kebutuhan.

Contoh:

```text
Room
DataStore
JSON
SQLite
```

Jangan memilih database kompleks untuk data yang sebenarnya sederhana.

---

# 74. Storage Architecture

Pisahkan:

```text
UI
↓
ViewModel / State
↓
Repository
↓
Local Data Source
```

Contoh:

```text
TaskScreen
↓
TaskViewModel
↓
TaskRepository
↓
TaskDatabase
```

---

# 75. Data Model

Possible conceptual models:

```text
StudentProfile
Course
Grade
Semester
Task
Deadline
Note
PomodoroSession
Settings
```

Jangan menyimpan semuanya sebagai satu JSON raksasa jika akan menyulitkan query/update.

Pilih storage berdasarkan kebutuhan aktual.

---

# 76. Student Profile

Optional.

Data:

```text
Name
Major
University
Current Semester
```

Semua optional.

User tidak harus mengisi identitas pribadi.

---

# 77. Course Model

```text
Course Name
Course Code
Credits
Lecturer
Semester
```

Lecturer optional.

---

# 78. Academic History

Semester:

```text
Semester 1
Semester 2
Semester 3
```

Setiap semester memiliki course dan grade.

---

# 79. Privacy

Default:

> **Local-first.**

Aplikasi tidak membutuhkan cloud untuk core features.

Tidak mengirim:

- nilai
- IPK
- deadline
- notes
- personal data

ke server secara otomatis.

---

# 80. No Mandatory Account

MVP:

```text
Open app
↓
Use app
```

Tidak:

```text
Sign up
↓
Verify email
↓
Login
↓
Use app
```

untuk fitur dasar.

---

# 81. Backup

User dapat membuat backup.

Format:

```text
StudentToolkitBackup.json
```

Backup dapat berisi:

```text
Settings
Academic
Tasks
Notes
Pomodoro
```

---

# 82. Android Share Backup

User dapat membagikan file backup melalui Android Share Sheet.

Contoh:

```text
Export Backup
↓
Share
↓
Google Drive
Files
WhatsApp
Telegram
etc.
```

Jangan mengirim data ke cloud otomatis.

---

# 83. Restore

User:

```text
Import Backup
```

Aplikasi harus:

- validate backup
- show backup version
- warn about overwrite
- preserve current data where possible
- fail safely

---

# 84. Schema Version

Backup harus memiliki version:

```json
{
  "schemaVersion": 1
}
```

Future migrations:

```text
v1
↓
migration
↓
v2
```

---

# 85. Notifications

Notification channels dapat digunakan:

```text
Deadlines
Pomodoro
General
```

User dapat mengatur notification dari Settings.

---

# 86. Android Permission Philosophy

Request only permissions that are actually needed.

Possible permissions may include:

- notifications
- exact alarm, if truly required
- storage access, depending on implementation
- camera, only if QR scanning is ever added

Jangan meminta:

```text
Contacts
Location
Microphone
Camera
Storage
```

hanya untuk terlihat lengkap.

---

# 87. QR Generation Permissions

QR generation sendiri seharusnya tidak membutuhkan camera permission.

Camera hanya diperlukan jika nantinya ada:

> QR Scanner.

---

# 88. Internet Permission

Core Student Toolkit sebisa mungkin tidak membutuhkan internet.

Jika AI/cloud/update checker ditambahkan:

```text
INTERNET
```

permission dapat digunakan secara terbatas.

---

# 89. Optional AI

AI tidak dibutuhkan untuk MVP.

Future AI features:

### Study Planner

User:

> "Saya punya tiga tugas minggu ini."

AI dapat membantu membuat urutan.

### Assignment Breakdown

User:

> "Saya harus membuat aplikasi kasir."

AI membantu memecah tugas.

### Grade Explanation

AI menjelaskan perhitungan nilai.

### Study Summary

Jika user memberikan materi, AI dapat membantu merangkum.

Namun:

> **AI harus tetap optional.**

---

# 90. Deterministic vs AI

AI tidak boleh menghitung:

```text
GPA
Grade
Percentage
Deadline
Randomization
Unit Conversion
```

Semua itu harus deterministic.

AI hanya:

```text
Explain
Suggest
Summarize
Plan
```

---

# 91. AI Privacy

Jika user memakai AI:

- jelaskan data yang akan dikirim
- minta user memulai action
- jangan mengirim seluruh database user secara otomatis
- kirim hanya data yang relevan

Contoh:

```text
AI Study Planner

The selected task information will be
sent to the configured AI service.

[ Continue ]
[ Cancel ]
```

---

# 92. No Background AI

Jangan menjalankan AI secara otomatis di background.

AI hanya aktif ketika user meminta.

---

# 93. App Performance

Student Toolkit harus:

- startup cepat
- memory usage wajar
- tidak menguras baterai
- tidak menjalankan task background tanpa alasan
- tidak melakukan polling berlebihan
- tetap responsif

---

# 94. Battery Awareness

Karena Android:

Hindari:

```text
continuous polling
```

untuk fitur yang tidak membutuhkannya.

Untuk timer gunakan elapsed-time logic.

Untuk deadline gunakan Android scheduling.

---

# 95. Offline-first

Core features harus bekerja offline.

Offline:

```text
Grade
GPA
IPK
Tasks
Deadlines
Pomodoro
Randomizer
QR
Converters
Timer
Stopwatch
Notes
Backup
```

Internet hanya diperlukan untuk optional:

```text
AI
Cloud Sync
Update Check
```

---

# 96. Empty States

Contoh:

```text
No tasks yet.

Add your first task.

[ + Add Task ]
```

Academic:

```text
No courses added.

Add a course to calculate your GPA.

[ + Add Course ]
```

Notes:

```text
No notes yet.

[ + New Note ]
```

---

# 97. Confirmation Dialogs

Confirmation diperlukan untuk:

- delete notes
- delete tasks
- delete course
- restore backup
- replace data

Tidak perlu confirmation untuk setiap action.

---

# 98. Undo

Jika memungkinkan:

```text
Task deleted.

[ Undo ]
```

Ini sangat cocok untuk mobile UX.

---

# 99. Snackbars

Gunakan snackbar untuk feedback ringan:

```text
✓ Course added
✓ Task completed
✓ Backup created
```

Jangan gunakan dialog untuk semuanya.

---

# 100. Academic Calculation Integrity

Weighted grade:

```text
Final =
Σ(score × weight)
```

GPA:

```text
GPA =
Σ(gradePoint × credits)
÷
Σ(credits)
```

Semua formula harus ditulis secara eksplisit di code/domain layer.

---

# 101. Rounding

Define rounding rules.

Contoh:

```text
84.5678
```

dapat ditampilkan:

```text
84.57
```

atau:

```text
84.6
```

berdasarkan settings.

Jangan membulatkan intermediate calculation secara sembarangan.

---

# 102. GPA Precision

User dapat mengatur:

```text
2 decimal places
3 decimal places
```

Default:

> 2 decimal places.

---

# 103. Deadline Timezone

Gunakan timezone perangkat.

Jangan hardcode:

```text
Asia/Jakarta
```

karena aplikasi dapat dipakai orang lain.

Namun default pengguna Indonesia tentu mengikuti waktu lokal perangkat mereka.

---

# 104. Date and Time

Gunakan Android date/time APIs.

Handle:

- midnight
- month change
- year change
- leap year
- timezone
- device clock changes

---

# 105. Randomization

Randomizer harus menggunakan random source yang tepat.

Tidak boleh:

```text
sort by name
```

kemudian mengklaim sebagai random.

---

# 106. Fair Groups

Jika jumlah tidak merata:

```text
10 people
3 groups
```

hasil ideal:

```text
4
3
3
```

bukan:

```text
5
5
0
```

unless user explicitly selects a different behavior.

---

# 107. QR Generation

QR generation harus:

- validate content
- display preview
- allow save
- allow share
- handle long text
- fail gracefully

---

# 108. Notes

Notes harus:

- autosave
- safe save
- searchable
- editable
- deletable

Jangan membuat data hilang ketika user menekan Android back.

---

# 109. Navigation State

Ketika user berpindah:

```text
Grade Calculator
↓
Home
↓
Grade Calculator
```

state yang belum selesai harus ditangani secara predictable.

---

# 110. Android App Lifecycle

AI developer wajib memperhatikan:

- configuration changes
- process death
- background/foreground
- rotation
- notification return
- activity recreation

Jangan menyimpan data penting hanya di memory UI.

---

# 111. Testing Device Matrix

Minimal test:

### Android Phone

- small screen
- normal screen
- large screen

### Android Versions

- minimum supported version
- current stable version

### Theme

- light
- dark

### Screen

- portrait
- landscape if supported

---

# 112. Academic Testing

Test:

```text
0
25
50
75
100
```

Test:

```text
decimal score
```

Test:

```text
negative input
```

Test:

```text
weight > 100
```

Test:

```text
weight total != 100
```

---

# 113. GPA Testing

Test:

```text
0 course
1 course
many courses
different SKS
different grade
custom grade point
```

---

# 114. Deadline Testing

Test:

```text
today
tomorrow
yesterday
next week
next month
next year
leap year
midnight
```

---

# 115. Randomizer Testing

Test:

```text
1 person
2 people
3 people
uneven groups
duplicate names
empty names
very long names
large participant list
```

---

# 116. Backup Testing

Test:

```text
valid backup
invalid backup
old schema
future schema
empty file
corrupted file
duplicate import
large dataset
```

---

# 117. Notification Testing

Test:

```text
permission denied
permission granted
app closed
app background
device restarted
timezone changed
deadline changed
```

---

# 118. Error Philosophy

When something fails:

```text
Tell user
+
Preserve current data
+
Log technical detail
+
Provide action
```

Never silently delete or overwrite user data.

---

# 119. Security

Do not:

- execute arbitrary imported data
- trust backup blindly
- store sensitive data in logs
- upload user data without permission
- request unnecessary permissions

---

# 120. Architecture

Recommended conceptual architecture:

```text
StudentToolkit
│
├── UI
│   ├── Home
│   ├── Academic
│   ├── Productivity
│   ├── Classroom
│   └── Tools
│
├── Domain
│   ├── Academic
│   ├── Tasks
│   ├── Randomizer
│   └── Utilities
│
├── Data
│   ├── Database
│   ├── Preferences
│   └── Backup
│
├── Notifications
│
├── AI
│   └── Optional
│
└── Core
    ├── Models
    ├── Utilities
    └── ErrorHandling
```

Actual folder structure may differ.

---

# 121. Separation of Concerns

Do not place business logic directly inside Composables/Activities/Fragments.

Recommended:

```text
UI
↓
ViewModel
↓
Use Case / Domain
↓
Repository
↓
Local Data
```

For example:

```text
GradeScreen
↓
GradeViewModel
↓
CalculateFinalGrade
↓
GradeResult
↓
UI
```

---

# 122. Database Choice

If the project uses relational persistent data such as:

- courses
- grades
- tasks
- notes

Room is a reasonable option.

For simple preferences:

> DataStore.

Do not put everything into one large JSON file if the application begins to require structured querying.

---

# 123. State Management

UI state must be predictable.

Examples:

```text
Loading
Success
Error
Empty
```

Do not rely on random mutable global state.

---

# 124. Feature Independence

Each module should be as independent as practical.

Example:

```text
Academic
```

should not depend on:

```text
Pomodoro
```

Similarly:

```text
Randomizer
```

should not depend on:

```text
GPA
```

This makes future maintenance easier.

---

# 125. Future Module Expansion

Possible future:

```text
Developer Tools
│
├── Binary Converter
├── Hex Converter
├── ASCII
├── JSON Formatter
├── Regex Tester
├── SQL Formatter
└── Timestamp Converter
```

This is especially relevant for Informatics students.

However:

> Do not implement every developer tool before core Student Toolkit is stable.

---

# 126. Student Toolkit for Informatics Students

Because the project is also intended to be useful to Informatics students, future tools can include:

```text
Binary Calculator
Decimal / Hex Converter
ASCII Table
Unicode Viewer
JSON Formatter
Regex Tester
Timestamp Converter
SQL Formatter
HTTP Status Reference
Color Converter
```

These are optional expansions.

---

# 127. Future Android Features

Possible:

### Home Screen Widget

Display:

```text
Next Deadline
```

### Quick Settings Tile

> Start Pomodoro

### Share Extensions

Share selected text into Student Toolkit.

### Floating Utility

Future only if genuinely useful.

### Clipboard Tools

Quick conversion or text utilities.

These are not MVP requirements.

---

# 128. Cloud Sync

Future feature only.

Possible:

```text
Phone
↓
Account
↓
Cloud
↓
Backup / Sync
```

But core app must remain functional without it.

---

# 129. Google Account / Login

Do not require login in MVP.

If cloud sync is introduced later:

> account should become optional until cloud data is actually required.

---

# 130. Monetization

Initial goal:

> useful personal project + portfolio.

Possible future:

### Free

Core utilities.

### Pro

Possible:

- advanced planner
- advanced academic statistics
- cloud sync
- premium themes
- advanced study tools
- AI assistance

But:

> do not lock basic calculations behind payment.

---

# 131. Portfolio Value

Student Toolkit demonstrates:

- Android development
- mobile UI/UX
- state management
- local database
- notifications
- date/time handling
- mathematical logic
- randomization
- file import/export
- backup/restore
- Android lifecycle
- permission handling

This makes it a strong portfolio project despite its relatively simple concept.

---

# 132. Relationship with Other Projects

Student Toolkit is independent from:

```text
TGO
PC Quick Checker
WiFi Speed + Ping Tester
WiFi Diagnostic Tool
Auto Absen Reminder
Portfolio Website
```

However, all projects can appear on the developer's portfolio.

---

# 133. Student Toolkit vs Auto Absen Reminder

These should remain distinct.

### Auto Absen Reminder

Focused application:

> attendance reminder / notification.

### Student Toolkit

Broader:

> academic + productivity + classroom + everyday student utilities.

The attendance reminder can remain a separate app.

Future possibility:

> Student Toolkit may link to it as another project, but does not need to absorb it.

---

# 134. Product Personality

The product should feel like:

> **"Aplikasi kecil buatan mahasiswa, untuk kebutuhan mahasiswa."**

It should be:

- friendly
- practical
- fast
- lightweight
- modern
- not intimidating

---

# 135. Home Dashboard Example

```text
┌───────────────────────────────────┐
│ Hello 👋                          │
│                                   │
│ 2 deadlines this week             │
│                                   │
│ ┌───────────────────────────────┐ │
│ │ GPA                           │ │
│ │ 3.67                          │ │
│ └───────────────────────────────┘ │
│                                   │
│ Upcoming                          │
│ ⚠ Database Assignment             │
│   2 days                          │
│                                   │
│ Quick Tools                       │
│ [Grade] [GPA] [Timer]             │
│ [Random] [QR] [Notes]             │
└───────────────────────────────────┘
```

---

# 136. Mobile UX Principle

Jangan membuat setiap fitur membutuhkan banyak screen.

Contoh Grade Calculator:

```text
Select Course
↓
Enter Components
↓
Calculate
```

Jangan:

```text
Screen 1
↓
Screen 2
↓
Screen 3
↓
Screen 4
↓
Screen 5
```

untuk kalkulasi sederhana.

---

# 137. Bottom Sheets

Gunakan bottom sheet apabila cocok untuk:

- add task
- choose grade scale
- select timer
- choose randomizer mode

Ini terasa lebih natural di Android.

---

# 138. Floating Action Button

Optional.

Contoh:

```text
+
```

untuk:

- add task
- add note
- add course

Jangan gunakan FAB untuk semua hal.

---

# 139. Search

Future global search:

```text
Search Student Toolkit
```

Dapat menemukan:

- tasks
- notes
- courses
- utilities

---

# 140. Settings

Settings:

```text
Appearance
Notifications
Academic Scale
GPA Scale
Data
Backup
AI
About
```

---

# 141. About

Contoh:

```text
Student Toolkit

Small tools for your everyday student life.

Version 1.0.0

Android App

[Portfolio]
[GitHub]
```

---

# 142. No Windows Version

Ini harus dipertahankan sebagai product constraint.

Jangan membuat:

```text
StudentToolkit.exe
```

Student Toolkit **bukan Windows application**.

Jika suatu hari ingin membuat desktop version:

> itu harus menjadi project berbeda atau keputusan produk baru.

Jangan otomatis melakukan cross-platform desktop port.

---

# 143. No iOS Version

Student Toolkit **tidak menargetkan iPhone / iOS**.

Tidak perlu:

- Swift version
- iOS project
- IPA
- Xcode project

Fokus:

> Android APK.

---

# 144. MVP Feature Set

MVP recommended:

## Academic

```text
✓ Grade Calculator
✓ GPA / IPS
✓ IPK
✓ Score Target Planner
✓ Custom Grade Scale
```

## Productivity

```text
✓ Task List
✓ Deadline
✓ Notifications
✓ Pomodoro
```

## Classroom

```text
✓ Group Randomizer
✓ Presenter Randomizer
✓ Task Randomizer
```

## Utilities

```text
✓ QR Generator
✓ Unit Converter
✓ Percentage Calculator
✓ Timer
✓ Stopwatch
✓ Simple Notes
```

## Data

```text
✓ Local Storage
✓ Backup
✓ Restore
```

---

# 145. MVP Should NOT Include

Do not include initially:

```text
✗ Social network
✗ Chat
✗ Online classroom
✗ Cloud sync
✗ Mandatory account
✗ AI tutor
✗ LMS integration
✗ University API
✗ Complex calendar synchronization
✗ Desktop version
✗ iOS version
✗ Subscription system
```

---

# 146. Version Roadmap

## v0.1 — Android Foundation

- project setup
- navigation
- theme
- local data
- core architecture

## v0.2 — Academic

- grade calculator
- GPA
- IPK
- target score

## v0.3 — Productivity

- tasks
- deadlines
- notifications
- Pomodoro

## v0.4 — Classroom

- group randomizer
- presenter randomizer
- task randomizer

## v0.5 — Utilities

- QR
- converter
- percentage
- timer
- stopwatch
- notes

## v0.6 — Data Safety

- backup
- restore
- migration
- validation

## v0.7 — Polish

- UI polish
- animation where appropriate
- accessibility
- performance
- Android compatibility

## v0.8 — Optional AI

- study planner
- explanation
- task breakdown

## v1.0 — Public APK

Stable Android release.

---

# 147. Release APK

Release output:

```text
StudentToolkit-v1.0.0-release.apk
```

Before release:

```text
Build
↓
Test
↓
Install on real Android device
↓
Test all core flows
↓
Build release APK
↓
Install release APK
↓
Final validation
```

---

# 148. APK Signing

Release APK should be properly signed.

Do not expose signing credentials or keystore passwords in source control.

AI developer must not commit:

```text
keystore
password
private key
secret
```

to Git.

---

# 149. Build Variants

Possible:

```text
debug
release
```

Debug build may include logging.

Release build must:

- use release configuration
- not expose debug UI
- minimize unnecessary logs
- be signed

---

# 150. Android Permissions Audit

Before release, inspect:

```text
AndroidManifest.xml
```

and verify every permission is justified.

Avoid unnecessary permissions.

---

# 151. Battery Audit

Before release check:

- timers
- notifications
- alarms
- background work
- database queries
- network calls

Student Toolkit should not noticeably drain battery when idle.

---

# 152. Crash Resistance

Test common failures:

- app killed while editing
- screen rotation
- notification click
- permission denied
- invalid backup
- corrupted data
- no storage
- very large note
- empty task
- malformed input

---

# 153. Error UX

User should see:

```text
Something went wrong.

Your existing data was not changed.

Try again.
```

Technical logging remains separate.

---

# 154. AI Developer Instructions

When an AI coding agent receives this project:

> **Do not start coding immediately.**

First inspect:

- repository
- Android project
- Gradle configuration
- SDK configuration
- architecture
- navigation
- UI
- persistence
- notification implementation
- current features
- current bugs
- tests

Then:

```text
Understand
↓
Audit
↓
Classify
↓
Plan
↓
Implement
↓
Build
↓
Install
↓
Test
↓
Validate
```

---

# 155. AI Must Distinguish

```text
BUG
```

from:

```text
REFACTOR
```

from:

```text
NEW FEATURE
```

from:

```text
FUTURE IDEA
```

Do not rebuild the app because something could be cleaner.

---

# 156. Preserve Working Functionality

Before modifying working code:

```text
What currently works?
What depends on it?
Why change it?
What can break?
How will it be tested?
```

Do not make unnecessary rewrites.

---

# 157. Mobile-Specific AI Rules

AI developer must explicitly consider:

- Android lifecycle
- process death
- permission behavior
- notification behavior
- background limits
- battery use
- screen size
- soft keyboard
- configuration changes
- APK signing
- release build

These are part of the product, not afterthoughts.

---

# 158. Definition of Done

A feature is complete only when:

```text
Code implemented
+
UI works
+
Logic tested
+
Persistence works
+
Android lifecycle considered
+
Error handling works
+
Real device tested
+
Build succeeds
+
Release APK validated
```

---

# 159. Real Device Requirement

Emulator testing is not enough for final validation.

At least one real Android device should be tested for:

- UI
- notification
- timer
- performance
- keyboard
- storage
- sharing
- APK installation

---

# 160. Product Success Criteria

Student Toolkit succeeds if a student can:

```text
Open app
↓
Immediately understand dashboard
↓
Calculate grades
↓
Check GPA
↓
Track deadlines
↓
Receive reminders
↓
Focus with Pomodoro
↓
Randomize groups
↓
Generate QR
↓
Use small utilities
```

without needing internet or an account for core functionality.

---

# 161. Scope Control

The biggest risk is feature creep.

When considering a new feature:

```text
CORE
USEFUL
NICE TO HAVE
FUTURE
```

Do not implement everything immediately.

---

# 162. Product Expansion

Long-term:

```text
                    STUDENT TOOLKIT
                           │
          ┌────────────────┼────────────────┐
          │                │                │
       Academic       Productivity       Classroom
          │                │                │
          └────────────────┼────────────────┘
                           │
                         Tools
                           │
                     Developer Tools
                           │
                     Optional AI
                           │
                    Optional Cloud
```

But the core app must remain useful without these expansions.

---

# 163. Possible Android Ecosystem

Future:

```text
Student Toolkit
│
├── Android App
├── Home Screen Widget
├── Quick Settings Tile
├── Share Integration
└── Optional Cloud Sync
```

There is no requirement to create a Windows desktop counterpart.

---

# 164. Portfolio Value

Student Toolkit demonstrates:

- Android development
- Kotlin / Android framework knowledge
- mobile UI/UX
- state management
- Room / local data
- DataStore
- notifications
- Android lifecycle
- permission handling
- mathematical logic
- randomization
- backup/restore
- APK release process

This is an excellent portfolio complement to a Windows project such as TGO.

---

# 165. Relationship to Other Projects

Current personal-project ecosystem:

```text
TGO
│
└── Windows PC Performance Platform

PC Quick Checker
│
└── Windows PC Utility

WiFi Speed + Ping Tester
│
└── Windows Network Utility

WiFi Diagnostic Tool
│
└── Windows Network Diagnostic Utility

Auto Absen Reminder
│
└── Android Utility

Student Toolkit
│
└── Android Student Utility

Portfolio Website
│
└── Showcase / Personal Branding
```

Each project has a clear identity.

---

# 166. Student Toolkit vs Auto Absen Reminder

Keep them separate.

## Auto Absen Reminder

Focus:

> attendance reminder.

## Student Toolkit

Focus:

> everyday academic + productivity + classroom utilities.

Student Toolkit may mention the attendance reminder in the developer's portfolio, but it does not have to absorb it.

---

# 167. Product Personality

Student Toolkit should feel:

> **“Aplikasi yang benar-benar dibuat untuk dipakai mahasiswa setiap hari.”**

Not:

> "A giant enterprise academic system."

It should feel:

- quick
- friendly
- practical
- lightweight
- useful
- mobile-native

---

# 168. Example Daily Usage

Morning:

```text
Open Home
↓
See today's deadlines
```

Before class:

```text
Grade Calculator
```

During group work:

```text
Randomizer
```

During studying:

```text
Pomodoro
```

Need to share Wi-Fi:

```text
QR Generator
```

Need to calculate something:

```text
Percentage Calculator
```

Need to remember something:

```text
Notes
```

Everything happens on the same Android app.

---

# 169. Final Product Statement

> **Student Toolkit is an Android-only, offline-first utility suite designed to help students calculate, plan, organize, randomize, focus, and handle everyday academic tasks from one lightweight mobile app.**

---

# 170. One-Line Description

> **Student Toolkit — Small tools for your everyday student life, right on Android.**

Alternative:

> **Calculate. Plan. Randomize. Focus. Study. Anywhere.**

---

# 171. Critical Platform Constraint

This project MUST remain:

```text
ANDROID ONLY
```

Supported output:

```text
.apk
```

Not supported:

```text
.exe
.dmg
.deb
.ipa
```

Not targeted:

```text
Windows
macOS
Linux
iOS
iPhone
```

Do not introduce desktop or iOS projects into this repository.

---

# 172. Final Development Principle

> **Build for the phone, not for the desktop.**

The interface, interaction, storage, notification, performance, lifecycle, and distribution must all follow Android-first principles.

---

# 173. Ultimate Product Vision

Long-term:

```text
                         STUDENT TOOLKIT
                                │
               ANDROID STUDENT UTILITY PLATFORM
                                │
       ┌────────────────────────┼────────────────────────┐
       │                        │                        │
    ACADEMIC               PRODUCTIVITY              CLASSROOM
       │                        │                        │
 Grade Calculator            Tasks                  Groups
 GPA / IPK                  Deadline                Randomizer
 Score Planner              Pomodoro                Presenter
 Grade Converter            Focus                  Assignment
       │                        │                        │
       └────────────────────────┼────────────────────────┘
                                │
                             TOOLS
                                │
                    QR / Converter / Timer
                    Stopwatch / Notes
                                │
                                ▼
                         OPTIONAL AI
                                │
                                ▼
                       OPTIONAL CLOUD
```

The core principle remains:

> **A student should be able to open one lightweight Android app whenever they have a small academic or productivity problem — and solve it in seconds.**

**End of README**