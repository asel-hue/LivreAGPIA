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
        private const val KEY_CONTENT_LANGUAGE = "content_language"
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

        // Modes de contenu : français, arabe, ou les deux côte à côte (interface en français).
        const val LANG_FR = "fr"
        const val LANG_AR = "ar"
        const val LANG_BOTH = "both"

        /** Ordre du cycle de l'action rapide « Changer de langue » de la barre de lecture. */
        val LANGUAGE_CYCLE = listOf(LANG_FR, LANG_AR, LANG_BOTH)
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

    // ---- Langue du contenu (français / arabe / bilingue) ----

    fun getContentLanguage(): String =
        when (prefs.getString(KEY_CONTENT_LANGUAGE, LANG_FR)) {
            LANG_AR -> LANG_AR
            LANG_BOTH -> LANG_BOTH
            else -> LANG_FR
        }

    fun setContentLanguage(language: String) {
        val value = if (language == LANG_AR || language == LANG_BOTH) language else LANG_FR
        prefs.edit { putString(KEY_CONTENT_LANGUAGE, value) }
    }

    /** Vrai uniquement en mode arabe seul (interface RTL, texte plus grand). */
    fun isArabic(): Boolean = getContentLanguage() == LANG_AR

    fun isBilingual(): Boolean = getContentLanguage() == LANG_BOTH

    /** Mode suivant du cycle fr → ar → bilingue → fr. */
    fun nextContentLanguage(): String {
        val index = LANGUAGE_CYCLE.indexOf(getContentLanguage())
        return LANGUAGE_CYCLE[(index + 1) % LANGUAGE_CYCLE.size]
    }

    /** Locale de l'interface : arabe en mode arabe seul, français sinon (y compris bilingue). */
    fun uiLocaleTag(): String = if (isArabic()) LANG_AR else LANG_FR

    /**
     * Aligne la locale de l'interface sur la langue du contenu (chaînes traduites, sens RTL).
     * AppCompat recrée les activités si la locale change ; la valeur est persistée par le
     * système (API 33+) ou par AppCompat via AppLocalesMetadataHolderService (API < 33).
     * @return vrai si la locale a changé (les activités vont être recréées).
     */
    fun applyAppLocale(): Boolean {
        val tag = uiLocaleTag()
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (current == tag) return false
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        return true
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
