package com.tehgeii.studenttoolkit.i18n

import java.util.Locale

/** Language chosen in Settings. SYSTEM follows the phone's language. */
enum class AppLanguage { SYSTEM, ENGLISH, INDONESIAN }

/**
 * Tiny two-language text helper: every UI text is written as `t("English", "Indonesia")`, so both
 * translations sit side by side and a missing translation is impossible. No resources, no restart:
 * switching the language in Settings updates the UI immediately.
 */
class Lang private constructor(val indonesian: Boolean) {
    val locale: Locale = if (indonesian) Locale.forLanguageTag("id-ID") else Locale.ENGLISH

    operator fun invoke(en: String, id: String): String = if (indonesian) id else en

    /** "1 day" / "3 days" in English; "3 hari" in Indonesian (no plural form). */
    fun count(n: Long, enOne: String, enMany: String, id: String): String = when {
        indonesian -> "$n $id"
        n == 1L -> "1 $enOne"
        else -> "$n $enMany"
    }

    fun count(n: Int, enOne: String, enMany: String, id: String): String = count(n.toLong(), enOne, enMany, id)

    companion object {
        val EN = Lang(false)
        val ID = Lang(true)

        fun resolve(language: AppLanguage, systemLanguage: String = Locale.getDefault().language): Lang = when (language) {
            AppLanguage.ENGLISH -> EN
            AppLanguage.INDONESIAN -> ID
            // Android reports Indonesian as "in" on older versions and "id" on newer ones.
            AppLanguage.SYSTEM -> if (systemLanguage == "in" || systemLanguage == "id") ID else EN
        }
    }
}
