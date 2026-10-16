package com.tehgeii.studenttoolkit.i18n

import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.data.BackupError
import com.tehgeii.studenttoolkit.data.BackupException
import com.tehgeii.studenttoolkit.data.ThemeMode
import com.tehgeii.studenttoolkit.domain.academic.GpaTarget
import com.tehgeii.studenttoolkit.domain.academic.ScaleProblem
import com.tehgeii.studenttoolkit.domain.classroom.AssignRule
import com.tehgeii.studenttoolkit.domain.focus.FocusPhase
import com.tehgeii.studenttoolkit.domain.tasks.DeadlineStatus
import com.tehgeii.studenttoolkit.domain.tasks.Deadlines
import com.tehgeii.studenttoolkit.domain.tasks.Priority
import com.tehgeii.studenttoolkit.domain.tasks.TaskSection
import com.tehgeii.studenttoolkit.domain.tasks.TaskSort
import com.tehgeii.studenttoolkit.domain.tools.PercentMode
import com.tehgeii.studenttoolkit.domain.tools.QrError
import com.tehgeii.studenttoolkit.domain.tools.QrType
import com.tehgeii.studenttoolkit.domain.tools.UnitCategory
import com.tehgeii.studenttoolkit.domain.tools.UnitDef
import com.tehgeii.studenttoolkit.domain.tools.WifiSecurity
import java.time.LocalDate
import java.time.ZoneId

// ---- Dates & durations --------------------------------------------------------------------------

fun Lang.date(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String = Fmt.date(epochMillis, zone, locale)

fun Lang.dateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String = Fmt.dateTime(epochMillis, zone, locale)

fun Lang.longDate(date: LocalDate): String = Fmt.longDate(date, locale)

/** Compact duration: 210 -> "3h 30m" / "3 jam 30 mnt". */
fun Lang.durationShort(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (indonesian) {
        when {
            h == 0 -> "$m mnt"
            m == 0 -> "$h jam"
            else -> "$h jam $m mnt"
        }
    } else {
        when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}

/** Duration in words for reminders: 1440 -> "1 day", 135 -> "2 hours 15 minutes" / "2 jam 15 menit". */
fun Lang.durationWords(minutes: Int): String {
    val week = 7 * 24 * 60
    if (minutes > 0 && minutes % week == 0) return count(minutes / week, "week", "weeks", "minggu")
    val days = minutes / (24 * 60)
    val hours = (minutes % (24 * 60)) / 60
    val mins = minutes % 60
    val parts = buildList {
        if (days > 0) add(count(days, "day", "days", "hari"))
        if (hours > 0) add(count(hours, "hour", "hours", "jam"))
        if (mins > 0 || isEmpty()) add(count(mins, "minute", "minutes", "menit"))
    }
    return parts.joinToString(" ")
}

// ---- Tasks --------------------------------------------------------------------------------------

/** Short relative deadline text, e.g. "Due in 2 days" / "2 hari lagi". */
fun Lang.dueText(deadline: Long, now: Long, zone: ZoneId): String {
    val days = Deadlines.daysUntil(deadline, now, zone)
    val time = Fmt.time(deadline, zone)
    if (deadline < now) {
        val late = -days
        return when {
            late <= 0L -> this("Overdue since $time", "Lewat sejak $time")
            else -> this("Overdue by ", "Terlambat ") + count(late, "day", "days", "hari")
        }
    }
    return when {
        days <= 0L -> this("Due today, $time", "Hari ini, $time")
        days == 1L -> this("Due tomorrow, $time", "Besok, $time")
        else -> this("Due in $days days", "$days hari lagi")
    }
}

fun Lang.status(status: DeadlineStatus): String = when (status) {
    DeadlineStatus.OVERDUE -> this("Overdue", "Terlambat")
    DeadlineStatus.DUE_TODAY -> this("Due today", "Hari ini")
    DeadlineStatus.ONE_DAY_LEFT -> this("1 day left", "Sisa 1 hari")
    DeadlineStatus.DUE_SOON -> this("Due soon", "Segera")
    DeadlineStatus.UPCOMING -> this("Upcoming", "Mendatang")
    DeadlineStatus.NO_DEADLINE -> this("No deadline", "Tanpa deadline")
    DeadlineStatus.COMPLETED -> this("Completed", "Selesai")
}

fun Lang.priority(priority: Priority): String = when (priority) {
    Priority.LOW -> this("Low", "Rendah")
    Priority.MEDIUM -> this("Medium", "Sedang")
    Priority.HIGH -> this("High", "Tinggi")
}

fun Lang.taskSort(sort: TaskSort): String = when (sort) {
    TaskSort.SMART -> this("Smart", "Otomatis")
    TaskSort.DEADLINE -> this("Deadline", "Deadline")
    TaskSort.PRIORITY -> this("Priority", "Prioritas")
    TaskSort.NEWEST -> this("Recently added", "Terbaru")
}

fun Lang.taskSection(section: TaskSection): String = when (section) {
    TaskSection.OVERDUE -> this("Overdue", "Terlambat")
    TaskSection.TODAY -> this("Today", "Hari ini")
    TaskSection.UPCOMING -> this("Upcoming", "Mendatang")
    TaskSection.SOMEDAY -> this("No deadline", "Tanpa deadline")
    TaskSection.COMPLETED -> this("Completed", "Selesai")
}

fun Lang.greeting(hour: Int): String = when (hour) {
    in 4..10 -> this("Good morning", "Selamat pagi")
    in 11..14 -> this("Good afternoon", "Selamat siang")
    in 15..18 -> this("Good evening", "Selamat sore")
    else -> this("Good night", "Selamat malam")
}

// ---- Academic -----------------------------------------------------------------------------------

fun Lang.scaleProblem(problem: ScaleProblem): String = when (problem.kind) {
    ScaleProblem.Kind.EMPTY -> this("Add at least one grade.", "Tambahkan minimal satu nilai huruf.")
    ScaleProblem.Kind.BLANK_LETTER -> this("Every grade needs a letter.", "Setiap baris butuh huruf nilai.")
    ScaleProblem.Kind.DUPLICATE_LETTERS -> this("Duplicate letters: ${problem.detail}.", "Huruf nilai dobel: ${problem.detail}.")
    ScaleProblem.Kind.MIN_OUT_OF_RANGE -> this("Minimum scores must be between 0 and 100.", "Skor minimum harus antara 0 dan 100.")
    ScaleProblem.Kind.NEGATIVE_POINTS -> this("Grade points cannot be negative.", "Bobot nilai tidak boleh negatif.")
    ScaleProblem.Kind.DUPLICATE_MIN -> this("Two grades share the same minimum score.", "Ada dua nilai dengan skor minimum yang sama.")
    ScaleProblem.Kind.LOWEST_NOT_ZERO ->
        this("The lowest grade should start at 0 so every score gets a grade.", "Nilai terendah harus mulai dari 0 agar semua skor punya nilai.")
}

fun Lang.gpaInvalid(reason: GpaTarget.Reason, maxPoints: Double): String = when (reason) {
    GpaTarget.Reason.CURRENT_OUT_OF_RANGE ->
        this("Current GPA must be between 0 and ${Fmt.number(maxPoints)}.", "IPK saat ini harus antara 0 dan ${Fmt.number(maxPoints)}.")
    GpaTarget.Reason.TARGET_OUT_OF_RANGE ->
        this("Target GPA must be between 0 and ${Fmt.number(maxPoints)}.", "Target IPK harus antara 0 dan ${Fmt.number(maxPoints)}.")
    GpaTarget.Reason.NEGATIVE_CREDITS -> this("Credits cannot be negative.", "SKS tidak boleh negatif.")
}

// ---- Classroom ----------------------------------------------------------------------------------

fun Lang.assignRule(rule: AssignRule): String = when (rule) {
    AssignRule.REPEAT_TASKS -> this("Allow repeated tasks", "Boleh ada tugas yang sama")
    AssignRule.LEAVE_PEOPLE_UNASSIGNED -> this("Leave some people unassigned", "Sebagian orang tidak dapat tugas")
    AssignRule.MULTIPLE_TASKS_PER_PERSON -> this("Give some people more than one task", "Sebagian orang dapat lebih dari satu tugas")
    AssignRule.LEAVE_TASKS_UNASSIGNED -> this("Leave some tasks unassigned", "Sebagian tugas tidak dibagikan")
}

// ---- Tools --------------------------------------------------------------------------------------

fun Lang.qrType(type: QrType): String = when (type) {
    QrType.TEXT -> this("Text", "Teks")
    QrType.URL -> "URL"
    QrType.WIFI -> "Wi-Fi"
    QrType.EMAIL -> "Email"
    QrType.PHONE -> this("Phone", "Telepon")
}

fun Lang.wifiSecurity(security: WifiSecurity): String = when (security) {
    WifiSecurity.WPA -> "WPA/WPA2/WPA3"
    WifiSecurity.WEP -> "WEP"
    WifiSecurity.NONE -> this("None (open)", "Tanpa sandi (terbuka)")
}

fun Lang.qrError(error: QrError): String = when (error) {
    QrError.URL_SPACES -> this("A URL cannot contain spaces.", "URL tidak boleh berisi spasi.")
    QrError.URL_INVALID -> this("This doesn't look like a valid URL.", "Sepertinya ini bukan URL yang valid.")
    QrError.WIFI_PASSWORD_MISSING -> this("Enter the Wi-Fi password.", "Masukkan password Wi-Fi.")
    QrError.WIFI_PASSWORD_SHORT -> this("WPA passwords are at least 8 characters.", "Password WPA minimal 8 karakter.")
    QrError.EMAIL_INVALID -> this("Enter a valid email address.", "Masukkan alamat email yang valid.")
    QrError.PHONE_INVALID -> this("Enter a valid phone number.", "Masukkan nomor telepon yang valid.")
    QrError.TOO_LONG -> this("Too long for a QR code.", "Terlalu panjang untuk kode QR.")
}

fun Lang.unitCategory(category: UnitCategory): String = when (category) {
    UnitCategory.LENGTH -> this("Length", "Panjang")
    UnitCategory.WEIGHT -> this("Weight", "Berat")
    UnitCategory.TEMPERATURE -> this("Temperature", "Suhu")
    UnitCategory.TIME -> this("Time", "Waktu")
    UnitCategory.STORAGE -> this("Storage", "Penyimpanan")
    UnitCategory.DATA_RATE -> this("Data rate", "Kecepatan data")
    UnitCategory.SPEED -> this("Speed", "Kecepatan")
    UnitCategory.AREA -> this("Area", "Luas")
    UnitCategory.VOLUME -> "Volume"
}

private val indonesianUnitNames = mapOf(
    "mm" to "Milimeter", "cm" to "Sentimeter", "m" to "Meter", "km" to "Kilometer", "in" to "Inci", "ft" to "Kaki",
    "yd" to "Yard", "mi" to "Mil", "nmi" to "Mil laut",
    "mg" to "Miligram", "g" to "Gram", "ons" to "Ons (100 g)", "kg" to "Kilogram", "t" to "Ton", "oz" to "Ons Inggris (oz)",
    "lb" to "Pon",
    "°C" to "Celsius", "°F" to "Fahrenheit", "K" to "Kelvin",
    "ms" to "Milidetik", "s" to "Detik", "min" to "Menit", "h" to "Jam", "d" to "Hari", "wk" to "Minggu",
    "yr" to "Tahun (365,25 hari)",
    "bit" to "Bit", "B" to "Byte", "kB" to "Kilobyte (SI, 1000 B)", "MB" to "Megabyte (SI)", "GB" to "Gigabyte (SI)",
    "TB" to "Terabyte (SI)", "KiB" to "Kibibyte (IEC, 1024 B)", "MiB" to "Mebibyte (IEC)", "GiB" to "Gibibyte (IEC)",
    "TiB" to "Tebibyte (IEC)",
    "bps" to "Bit per detik", "kbps" to "Kilobit per detik", "Mbps" to "Megabit per detik", "Gbps" to "Gigabit per detik",
    "B/s" to "Byte per detik", "kB/s" to "Kilobyte per detik", "MB/s" to "Megabyte per detik", "MiB/s" to "Mebibyte per detik",
    "m/s" to "Meter per detik", "km/h" to "Kilometer per jam", "mph" to "Mil per jam", "kn" to "Knot", "ft/s" to "Kaki per detik",
    "mm²" to "Milimeter persegi", "cm²" to "Sentimeter persegi", "m²" to "Meter persegi", "ha" to "Hektare",
    "km²" to "Kilometer persegi", "in²" to "Inci persegi", "ft²" to "Kaki persegi", "ac" to "Acre",
    "mL" to "Mililiter", "cm³" to "Sentimeter kubik", "L" to "Liter", "m³" to "Meter kubik", "tsp" to "Sendok teh (AS)",
    "tbsp" to "Sendok makan (AS)", "fl oz" to "Ons cairan (AS)", "cup" to "Cangkir (AS)", "gal" to "Galon (AS)",
)

fun Lang.unitName(unit: UnitDef): String = if (indonesian) indonesianUnitNames[unit.symbol] ?: unit.name else unit.name

fun Lang.percentMode(mode: PercentMode): String = when (mode) {
    PercentMode.PERCENT_OF -> this("X% of Y", "X% dari Y")
    PercentMode.WHAT_PERCENT -> this("X is what % of Y", "X berapa % dari Y")
    PercentMode.CHANGE -> this("% change", "Perubahan %")
    PercentMode.DISCOUNT -> this("Discount", "Diskon")
    PercentMode.INCREASE -> this("Increase by %", "Naik %")
    PercentMode.DECREASE -> this("Decrease by %", "Turun %")
}

/** Labels of the two input fields for each percentage mode. */
fun Lang.percentFields(mode: PercentMode): Pair<String, String> = when (mode) {
    PercentMode.PERCENT_OF -> this("Percent (%)", "Persen (%)") to this("Value", "Nilai")
    PercentMode.WHAT_PERCENT -> this("Part (X)", "Bagian (X)") to this("Whole (Y)", "Total (Y)")
    PercentMode.CHANGE -> this("From", "Dari") to this("To", "Menjadi")
    PercentMode.DISCOUNT -> this("Price", "Harga") to this("Discount (%)", "Diskon (%)")
    PercentMode.INCREASE -> this("Value", "Nilai") to this("Increase (%)", "Kenaikan (%)")
    PercentMode.DECREASE -> this("Value", "Nilai") to this("Decrease (%)", "Penurunan (%)")
}

// ---- Focus --------------------------------------------------------------------------------------

fun Lang.focusPhase(phase: FocusPhase): String = when (phase) {
    FocusPhase.FOCUS -> this("FOCUS", "FOKUS")
    FocusPhase.BREAK -> this("BREAK", "ISTIRAHAT")
}

// ---- Settings & data ----------------------------------------------------------------------------

fun Lang.themeMode(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> this("System", "Sistem")
    ThemeMode.LIGHT -> this("Light", "Terang")
    ThemeMode.DARK -> this("Dark", "Gelap")
}

/** Language names are shown in their own language so anyone can find theirs. */
fun Lang.appLanguage(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> this("Follow system", "Ikuti sistem")
    AppLanguage.ENGLISH -> "English"
    AppLanguage.INDONESIAN -> "Bahasa Indonesia"
}

fun Lang.backupError(e: BackupException): String = when (e.reason) {
    BackupError.EMPTY -> this("The file is empty.", "File-nya kosong.")
    BackupError.TOO_LARGE -> this("The file is too large to be a Student Toolkit backup.", "File terlalu besar untuk backup Student Toolkit.")
    BackupError.NOT_JSON -> this("This file is not a valid backup (it is not readable JSON).", "File ini bukan backup yang valid (bukan JSON).")
    BackupError.NOT_STUDENT_TOOLKIT -> this("This file is not a Student Toolkit backup.", "File ini bukan backup Student Toolkit.")
    BackupError.NO_SCHEMA -> this("The backup has no schema version.", "Backup ini tidak punya versi skema.")
    BackupError.NEWER_SCHEMA -> this(
        "This backup was made by a newer version of Student Toolkit (schema ${e.detail}). Update the app first.",
        "Backup ini dibuat oleh versi Student Toolkit yang lebih baru (skema ${e.detail}). Update aplikasinya dulu.",
    )
    BackupError.UNSUPPORTED_SCHEMA -> this("Unsupported backup schema (${e.detail}).", "Skema backup tidak didukung (${e.detail}).")
    BackupError.DAMAGED -> this("The backup is damaged.", "Backup-nya rusak.") + if (e.detail.isNotEmpty()) " (${e.detail})" else ""
    BackupError.CANNOT_OPEN -> this("Could not open the file.", "File tidak bisa dibuka.")
    BackupError.CANNOT_READ -> this("Could not read the file.", "File tidak bisa dibaca.")
    BackupError.NOT_ALLOWED -> this("Student Toolkit is not allowed to read that file.", "Student Toolkit tidak diizinkan membaca file itu.")
}
