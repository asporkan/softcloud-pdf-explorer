package com.rejowan.pdfreaderpro.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.rejowan.pdfreaderpro.domain.model.AppLanguage

object AppLocaleManager {

    fun apply(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun currentFromDelegate(): AppLanguage {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isBlank()) return AppLanguage.SYSTEM
        return when {
            tags.startsWith("tr", ignoreCase = true) -> AppLanguage.TURKISH
            tags.startsWith("en", ignoreCase = true) -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }
}
