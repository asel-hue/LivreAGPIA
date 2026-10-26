package com.agpia.livre.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "agpia_prefs"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_NIGHT_MODE = "night_mode" // ancien booléen, conservé pour migration
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val KEY_LAST_CHAPTER = "last_chapter"
        private const val KEY_CONTENT_LANGUAGE = "content_language" // ancien réglage fr/ar/both, migré
        private const val KEY_PRIMARY_LANGUAGE = "primary_language"
        private const val KEY_SECONDARY_LANGUAGE = "secondary_language"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_CONTENT_BASE_URL_OVERRIDE = "content_base_url_override"
        private const val KEY_EXPAND_ALL = "expand_all"
        private const val KEY_SECTION_TITLE_ALIGNMENT = "section_title_alignment"

        // Alignement des titres de parties
        const val ALIGN_START = "start"
        const val ALIGN_CENTER = "center"

        // Tailles de police (sp)
        const val DEFAULT_FONT_SIZE = 18f
        const val MIN_FONT_SIZE = 12f
        const val MAX_FONT_SIZE = 28f
        const val FONT_SIZE_STEP = 1f

        // Modes de thème
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        // Codes de langue connus (le manifeste du contenu peut en déclarer d'autres).
        const val LANG_FR = "fr"
        const val LANG_AR = "ar"
        private const val LEGACY_LANG_BOTH = "both"

        /** Locales pour lesquelles l'interface est traduite (`values/`, `values-ar/`). */
        val SUPPORTED_UI_LOCALES = setOf(LANG_FR, LANG_AR)
    }

    // ---- Taille de police ----

    fun getFontSize(): Float = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)

    fun setFontSize(size: Float) {
        prefs.edit { putFloat(KEY_FONT_SIZE, size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)) }
    }

    // ---- Alignement des titres de parties (au début = à droite en arabe seul) ----

    fun getSectionTitleAlignment(): String =
        if (prefs.getString(KEY_SECTION_TITLE_ALIGNMENT, ALIGN_START) == ALIGN_CENTER) ALIGN_CENTER else ALIGN_START

    fun setSectionTitleAlignment(alignment: String) {
        prefs.edit {
            putString(KEY_SECTION_TITLE_ALIGNMENT, if (alignment == ALIGN_CENTER) ALIGN_CENTER else ALIGN_START)
        }
    }

    fun isSectionTitleCentered(): Boolean = getSectionTitleAlignment() == ALIGN_CENTER

    // ---- Thème ----

    fun getThemeMode(): Int {
        if (prefs.contains(KEY_THEME_MODE)) {
            return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
        }
        // Migration depuis l'ancien réglage booléen "mode sombre"
        return if (prefs.getBoolean(KEY_NIGHT_MODE, false)) THEME_DARK else THEME_SYSTEM
    }

    fun setThemeMode(mode: Int) {
        prefs.edit {
            putInt(KEY_THEME_MODE, mode)
            remove(KEY_NIGHT_MODE)
        }
    }

    fun applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(
            when (getThemeMode()) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    // ---- Langues du contenu : principale (pilote l'interface et le sens de lecture) + secondaire
    //      optionnelle (seconde colonne). Au plus deux langues affichées. ----

    /** Migre l'ancien réglage `content_language` (fr / ar / both) vers principale + secondaire. */
    private fun migrateLegacyLanguage() {
        if (!prefs.contains(KEY_CONTENT_LANGUAGE)) return
        val legacy = prefs.getString(KEY_CONTENT_LANGUAGE, LANG_FR)
        prefs.edit {
            when (legacy) {
                LANG_AR -> { putString(KEY_PRIMARY_LANGUAGE, LANG_AR); putString(KEY_SECONDARY_LANGUAGE, "") }
                LEGACY_LANG_BOTH -> { putString(KEY_PRIMARY_LANGUAGE, LANG_FR); putString(KEY_SECONDARY_LANGUAGE, LANG_AR) }
                else -> { putString(KEY_PRIMARY_LANGUAGE, LANG_FR); putString(KEY_SECONDARY_LANGUAGE, "") }
            }
            remove(KEY_CONTENT_LANGUAGE)
        }
    }

    /** Code de la langue principale (défaut « fr » ; la validité par rapport au manifeste est vérifiée par le dépôt). */
    fun getPrimaryLanguage(): String {
        migrateLegacyLanguage()
        return prefs.getString(KEY_PRIMARY_LANGUAGE, LANG_FR)?.takeIf { it.isNotBlank() } ?: LANG_FR
    }

    /** Code de la langue secondaire, ou null si aucune. Jamais égale à la principale. */
    fun getSecondaryLanguage(): String? {
        migrateLegacyLanguage()
        val value = prefs.getString(KEY_SECONDARY_LANGUAGE, "")?.takeIf { it.isNotBlank() } ?: return null
        return if (value == getPrimaryLanguage()) null else value
    }

    /** Change la langue principale ; si la secondaire devient identique, elle est retirée. */
    fun setPrimaryLanguage(code: String) {
        prefs.edit {
            putString(KEY_PRIMARY_LANGUAGE, code)
            if (prefs.getString(KEY_SECONDARY_LANGUAGE, "") == code) putString(KEY_SECONDARY_LANGUAGE, "")
        }
    }

    /** Change la langue secondaire (null ou vide = aucune ; ignorée si égale à la principale). */
    fun setSecondaryLanguage(code: String?) {
        val value = code?.takeIf { it.isNotBlank() && it != getPrimaryLanguage() } ?: ""
        prefs.edit { putString(KEY_SECONDARY_LANGUAGE, value) }
    }

    /**
     * Locale de l'interface pour la langue principale : la locale déclarée dans le manifeste si
     * l'interface est traduite dans cette locale, sinon le français.
     */
    fun uiLocaleTag(declaredUiLocale: String): String {
        val base = declaredUiLocale.substringBefore('-').substringBefore('_').lowercase()
        return if (base in SUPPORTED_UI_LOCALES) base else LANG_FR
    }

    /**
     * Aligne la locale de l'interface sur la langue principale (chaînes traduites, sens RTL).
     * AppCompat recrée les activités si la locale change ; la valeur est persistée par le
     * système (API 33+) ou par AppCompat via AppLocalesMetadataHolderService (API < 33).
     * @param declaredUiLocale locale d'interface déclarée dans le manifeste pour la langue principale.
     * @return vrai si la locale a changé (les activités vont être recréées).
     */
    fun applyAppLocale(declaredUiLocale: String): Boolean {
        val tag = uiLocaleTag(declaredUiLocale)
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (current == tag) return false
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        return true
    }

    // ---- Mises à jour de contenu ----

    fun getLastUpdateCheck(): Long = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)

    fun setLastUpdateCheck(timestamp: Long) {
        prefs.edit { putLong(KEY_LAST_UPDATE_CHECK, timestamp) }
    }

    /** Surcharge de l'URL de contenu (tests en debug uniquement ; null si absente). */
    fun getContentBaseUrlOverride(): String? = prefs.getString(KEY_CONTENT_BASE_URL_OVERRIDE, null)?.takeIf { it.isNotBlank() }

    fun setContentBaseUrlOverride(url: String?) {
        prefs.edit {
            if (url.isNullOrBlank()) remove(KEY_CONTENT_BASE_URL_OVERRIDE) else putString(KEY_CONTENT_BASE_URL_OVERRIDE, url.trim())
        }
    }

    // ---- Tout agrandir / tout réduire : à l'ouverture d'une heure, vrai = toutes les parties dépliées,
    //      faux = état par défaut (psaumes repliés, autres parties dépliées) ----

    fun isExpandAll(): Boolean = prefs.getBoolean(KEY_EXPAND_ALL, false)

    fun setExpandAll(expandAll: Boolean) {
        prefs.edit { putBoolean(KEY_EXPAND_ALL, expandAll) }
    }

    // ---- Dernière heure lue (partagée entre les langues) ----

    fun getLastChapter(): Int = prefs.getInt(KEY_LAST_CHAPTER, 1)

    fun setLastChapter(chapterNumber: Int) {
        prefs.edit { putInt(KEY_LAST_CHAPTER, chapterNumber) }
    }

    // ---- Marque-pages (format : "chapitre:section" séparés par ",", partagés entre les langues) ----

    fun getBookmarks(): Set<String> {
        val bookmarksString = prefs.getString(KEY_BOOKMARKS, "") ?: ""
        return if (bookmarksString.isEmpty()) emptySet() else bookmarksString.split(",").toSet()
    }

    fun addBookmark(chapterNumber: Int, sectionId: Int) {
        val bookmarks = getBookmarks().toMutableSet()
        bookmarks.add("$chapterNumber:$sectionId")
        prefs.edit { putString(KEY_BOOKMARKS, bookmarks.joinToString(",")) }
    }

    fun removeBookmark(chapterNumber: Int, sectionId: Int) {
        val bookmarks = getBookmarks().toMutableSet()
        bookmarks.remove("$chapterNumber:$sectionId")
        prefs.edit { putString(KEY_BOOKMARKS, bookmarks.joinToString(",")) }
    }

    fun isBookmarked(chapterNumber: Int, sectionId: Int): Boolean =
        getBookmarks().contains("$chapterNumber:$sectionId")
}
