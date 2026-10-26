package com.agpia.livre.data

import android.content.Context
import android.util.Log
import com.agpia.livre.data.chapters.chapter1
import com.agpia.livre.data.chapters.chapter10
import com.agpia.livre.data.chapters.chapter2
import com.agpia.livre.data.chapters.chapter3
import com.agpia.livre.data.chapters.chapter4
import com.agpia.livre.data.chapters.chapter5
import com.agpia.livre.data.chapters.chapter6
import com.agpia.livre.data.chapters.chapter7
import com.agpia.livre.data.chapters.chapter8
import com.agpia.livre.data.chapters.chapter9
import com.agpia.livre.utils.PreferencesManager
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

// Définit la structure des données ; le contenu français est dans data/chapters/ChapterN.kt,
// le contenu arabe dans assets/book/ar/chapterN.json.

/** Séparateur des titres bilingues (« Psaume 50 · مزمور 50 »). */
const val BILINGUAL_SEPARATOR = " · "

/** Compose un titre bilingue ; les parties absentes sont ignorées. */
fun bilingualTitle(french: String?, arabic: String?): String =
    listOfNotNull(french?.takeIf { it.isNotBlank() }, arabic?.takeIf { it.isNotBlank() })
        .joinToString(BILINGUAL_SEPARATOR)

data class Chapter(
    val number: Int,
    val title: String,
    val sections: List<Section>,
    /** Langue réelle du texte ("fr" ou "ar") : permet le sens de lecture même en cas de repli. */
    val language: String = PreferencesManager.LANG_FR
) {
    /**
     * Titre court pour la barre d'outils et le tiroir (comme dans l'application Agpia d'origine :
     * « Aube », « Troisième heure », « Fermeture du voile »…). Sans effet sur les titres arabes ;
     * dans un titre bilingue, seule la partie française est raccourcie.
     */
    val shortTitle: String
        get() = title.split(BILINGUAL_SEPARATOR).joinToString(BILINGUAL_SEPARATOR) { part ->
            part.removePrefix("Prière de la ")
                .removePrefix("Prière de l'")
                .removePrefix("Prière de ")
                .removeSuffix(" de chaque heure")
                .replaceFirstChar { it.titlecase() }
        }

    val isRtl: Boolean
        get() = language == PreferencesManager.LANG_AR
}

data class Section(
    val id: Int,
    val title: String,
    val content: String
) {
    /**
     * Toutes les parties sont repliables ; les psaumes sont simplement repliés par défaut
     * au premier affichage d'une heure (comme dans l'application Agpia d'origine).
     * Reconnaît « Psaume … » (français) et « مزمور … » / « المزمور … » (arabe).
     */
    val isPsalm: Boolean
        get() {
            val t = title.trimStart()
            return t.startsWith("Psaume", ignoreCase = true) ||
                t.startsWith("المزمور") ||
                t.startsWith("مزمور")
        }
}

/**
 * Une partie telle qu'affichée : texte français et/ou arabe, alignés par identifiant.
 * - mode français : [french] seul ; mode arabe : [arabic] seul (ou [french] si l'asset manque) ;
 * - mode bilingue : les deux, [arabic] pouvant être null si la section n'existe pas en arabe.
 */
data class SectionPair(
    val id: Int,
    val french: Section?,
    val arabic: Section?
) {
    /** Texte de référence (français de préférence) pour la recherche, le partage, les marque-pages. */
    val primary: Section
        get() = french ?: requireNotNull(arabic) { "SectionPair $id sans contenu" }

    val title: String
        get() = bilingualTitle(french?.title, arabic?.title)

    val isPsalm: Boolean
        get() = primary.isPsalm

    /**
     * Faux si le contenu est vide ou blanc dans toutes les langues affichées
     * (ex. « Kyrie Eleison (41 fois) ») : la partie n'a alors ni corps ni repli.
     */
    val hasBody: Boolean
        get() = listOfNotNull(french, arabic).any { it.content.isNotBlank() }
}

/**
 * Groupes de parties (« services » de la prière de minuit) : une partie d'en-tête de service suivie
 * des parties qui lui appartiennent. Règle retenue : un service commence à son en-tête et s'étend
 * jusqu'au prochain en-tête ou, au plus tard, jusqu'au premier « Notre Père » rencontré (chaque
 * service de minuit se clôt par Kyrie / Trisagion / Notre Père) ; ce qui suit le dernier « Notre Père »
 * du dernier service (Évangile de Luc 2, Acte de Foi, Absolution, Conclusion) est commun à l'heure
 * et reste hors groupe.
 */
object SectionGroups {
    private val GROUP_HEADER_TITLES = setOf(
        "Premier service", "Second service", "Troisième service",
        "الخدمة الأولى", "الخدمة الثانية", "الخدمة الثالثة"
    )
    private val GROUP_END_TITLES = setOf("Notre Père", "الصلاة الربانية")

    /** Vrai pour un titre d'en-tête de service exact (« Premier service – Première oraison » ne l'est pas). */
    fun isGroupHeader(title: String): Boolean = title.trim() in GROUP_HEADER_TITLES

    /** Vrai pour le titre qui clôt un service (dernière partie incluse dans le groupe). */
    fun isGroupEnd(title: String): Boolean = title.trim() in GROUP_END_TITLES

    /**
     * Calcule l'appartenance aux groupes d'une liste de parties, d'après le titre de référence
     * (français, ou arabe en mode arabe seul). Retourne (identifiants des en-têtes, membre -> en-tête).
     */
    fun compute(sections: List<SectionPair>): Grouping {
        val headers = LinkedHashSet<Int>()
        val parentOf = HashMap<Int, Int>()
        var current: Int? = null
        for (section in sections) {
            val title = section.primary.title
            when {
                isGroupHeader(title) -> {
                    headers.add(section.id)
                    current = section.id
                }
                current != null -> {
                    parentOf[section.id] = current
                    if (isGroupEnd(title)) current = null
                }
            }
        }
        return Grouping(headers, parentOf)
    }

    class Grouping(val headerIds: Set<Int>, private val parentOf: Map<Int, Int>) {
        fun isHeader(sectionId: Int): Boolean = sectionId in headerIds
        /** Identifiant de l'en-tête du groupe auquel appartient la partie, ou null si hors groupe. */
        fun parentOf(sectionId: Int): Int? = parentOf[sectionId]
        fun isEmpty(): Boolean = headerIds.isEmpty()

        companion object {
            val EMPTY = Grouping(emptySet(), emptyMap())
        }
    }
}

/** Contenu d'une heure prêt à l'affichage selon le mode de langue choisi. */
data class ChapterView(
    /** Chapitre « principal » (titre déjà bilingue en mode bilingue). */
    val chapter: Chapter,
    val sections: List<SectionPair>,
    /** Deux colonnes (français à gauche, arabe à droite). */
    val bilingual: Boolean
) {
    val number: Int get() = chapter.number
    val isRtl: Boolean get() = !bilingual && chapter.isRtl
}

/**
 * Dépôt du contenu, par langue :
 * - français : fichiers Kotlin `data/chapters/ChapterN.kt` ;
 * - arabe : `assets/book/ar/chapterN.json` (schéma {"number","title","sections":[{"id","title","content"}]}),
 *   lus à la demande, mis en cache, avec repli silencieux sur le français si l'asset manque ou est invalide ;
 * - bilingue : paire (FR, AR) alignée par identifiant de section via [getChapterPair] / [getChapterView].
 *
 * [init] doit être appelé une fois (depuis [com.agpia.livre.LivreAgpiaApp]) pour accéder aux assets
 * et à la langue choisie.
 */
object BookRepository {
    private const val TAG = "BookRepository"
    private const val ASSET_DIR_AR = "book/ar"

    private var appContext: Context? = null
    private var prefs: PreferencesManager? = null

    /** Cache arabe : numéro d'heure -> chapitre (null = asset absent/invalide, ne pas réessayer). */
    private val arabicCache = HashMap<Int, Chapter?>()

    private val frenchChapters: List<Chapter> by lazy {
        listOf(
            chapter1,
            chapter2,
            chapter3,
            chapter4,
            chapter5,
            chapter6,
            chapter7,
            chapter8,
            chapter9,
            chapter10
        )
    }

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        prefs = PreferencesManager(app)
    }

    /** Langue de contenu courante ("fr" par défaut tant que [init] n'a pas été appelé). */
    val language: String
        get() = prefs?.getContentLanguage() ?: PreferencesManager.LANG_FR

    /**
     * Chapitres dans le mode courant : français, arabe (repli français si l'asset manque),
     * ou français avec titres bilingues en mode bilingue (le texte de recherche reste le français).
     */
    fun getChapters(): List<Chapter> = frenchChapters.map { displayChapter(it) }

    fun getChapter(chapterNumber: Int): Chapter? =
        frenchChapters.find { it.number == chapterNumber }?.let { displayChapter(it) }

    fun getSection(chapterNumber: Int, sectionId: Int): Section? =
        getChapter(chapterNumber)?.sections?.find { it.id == sectionId }

    fun getTotalChapters(): Int = frenchChapters.size

    /** Titre français d'une heure (pour les partages, indépendamment de la langue affichée). */
    fun getFrenchChapter(chapterNumber: Int): Chapter? =
        frenchChapters.find { it.number == chapterNumber }

    /** Paire (français, arabe) d'une heure ; arabe null si l'asset manque ou est invalide. */
    fun getChapterPair(chapterNumber: Int): Pair<Chapter, Chapter?>? {
        val fr = frenchChapters.find { it.number == chapterNumber } ?: return null
        return fr to loadArabic(fr.number)
    }

    /** Heure prête à l'affichage dans le mode courant (sections alignées par identifiant en bilingue). */
    fun getChapterView(chapterNumber: Int): ChapterView? {
        val (fr, ar) = getChapterPair(chapterNumber) ?: return null
        return when (language) {
            PreferencesManager.LANG_AR -> {
                val shown = ar ?: fr
                ChapterView(shown, shown.sections.map { s ->
                    if (ar != null) SectionPair(s.id, null, s) else SectionPair(s.id, s, null)
                }, bilingual = false)
            }
            PreferencesManager.LANG_BOTH -> {
                val arabicById = ar?.sections?.associateBy { it.id }.orEmpty()
                ChapterView(
                    displayChapter(fr),
                    fr.sections.map { s -> SectionPair(s.id, s, arabicById[s.id]) },
                    bilingual = ar != null
                )
            }
            else -> ChapterView(fr, fr.sections.map { s -> SectionPair(s.id, s, null) }, bilingual = false)
        }
    }

    // ---- Chargement arabe ----

    private fun displayChapter(french: Chapter): Chapter = when (language) {
        PreferencesManager.LANG_AR -> loadArabic(french.number) ?: french
        PreferencesManager.LANG_BOTH -> {
            val ar = loadArabic(french.number)
            if (ar == null) french else french.copy(title = bilingualTitle(french.title, ar.title))
        }
        else -> french
    }

    @Synchronized
    private fun loadArabic(chapterNumber: Int): Chapter? {
        if (arabicCache.containsKey(chapterNumber)) return arabicCache[chapterNumber]
        val context = appContext
        val chapter = if (context == null) null else parseAsset(context, chapterNumber)
        arabicCache[chapterNumber] = chapter
        return chapter
    }

    private fun parseAsset(context: Context, chapterNumber: Int): Chapter? {
        val path = "$ASSET_DIR_AR/chapter$chapterNumber.json"
        val json = try {
            context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            Log.w(TAG, "Asset arabe absent ($path) : repli sur le français")
            return null
        }
        return try {
            parseChapter(json, chapterNumber)
        } catch (e: JSONException) {
            Log.w(TAG, "Asset arabe invalide ($path) : repli sur le français", e)
            null
        }
    }

    /** Analyse un chapitre JSON ; lève [JSONException] si le schéma n'est pas respecté. */
    internal fun parseChapter(json: String, expectedNumber: Int): Chapter? {
        val root = JSONObject(json)
        val number = root.optInt("number", expectedNumber)
        val title = root.getString("title").trim()
        val array = root.getJSONArray("sections")
        val sections = ArrayList<Section>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            sections.add(
                Section(
                    id = obj.optInt("id", i + 1),
                    title = obj.getString("title").trim(),
                    content = obj.getString("content")
                )
            )
        }
        if (title.isEmpty() || sections.isEmpty()) return null
        return Chapter(number, title, sections, PreferencesManager.LANG_AR)
    }
}
