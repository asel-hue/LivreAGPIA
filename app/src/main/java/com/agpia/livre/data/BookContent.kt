package com.agpia.livre.data

import android.content.Context
import android.util.Log
import com.agpia.livre.utils.PreferencesManager
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

// Modèle du livre. Tout le contenu (toutes langues) est en JSON : `assets/book/<lang>/chapterN.json`,
// décrit par `assets/book/manifest.json` ; une version téléchargée peut le remplacer (`filesDir/book/`).

/** Séparateur des titres bilingues (« Psaume 50 · مزمور 50 »). */
const val BILINGUAL_SEPARATOR = " · "

/** Compose un titre bilingue ; les parties absentes sont ignorées. */
fun bilingualTitle(primary: String?, secondary: String?): String =
    listOfNotNull(primary?.takeIf { it.isNotBlank() }, secondary?.takeIf { it.isNotBlank() })
        .joinToString(BILINGUAL_SEPARATOR)

data class Chapter(
    val number: Int,
    val title: String,
    val sections: List<Section>,
    /** Code de la langue réelle du texte (« fr », « ar »…), pour le sens de lecture même en cas de repli. */
    val language: String = PreferencesManager.LANG_FR,
    /** Sens de lecture de la langue (déclaré dans le manifeste). */
    val isRtl: Boolean = false
) {
    /**
     * Titre court pour la barre d'outils et le tiroir (comme dans l'application Agpia d'origine :
     * « Aube », « Troisième heure », « Fermeture du voile »…). Sans effet sur les titres arabes ;
     * dans un titre bilingue, chaque partie est raccourcie indépendamment.
     */
    val shortTitle: String
        get() = title.split(BILINGUAL_SEPARATOR).joinToString(BILINGUAL_SEPARATOR) { part ->
            part.removePrefix("Prière de la ")
                .removePrefix("Prière de l'")
                .removePrefix("Prière de ")
                .removeSuffix(" de chaque heure")
                .replaceFirstChar { it.titlecase() }
        }
}

data class Section(
    val id: Int,
    val title: String,
    val content: String,
    /** « psalm », « group » ou null (champ optionnel du JSON). */
    val kind: String? = null,
    /** Pour un en-tête de groupe : identifiant de la dernière partie du groupe. */
    val groupEnd: Int? = null
) {
    /**
     * Les psaumes sont repliés par défaut au premier affichage d'une heure. Source : champ `kind` ;
     * à défaut, préfixe du titre (« Psaume … », « مزمور … » / « المزمور … »).
     */
    val isPsalm: Boolean
        get() = when (kind) {
            KIND_PSALM -> true
            null -> {
                val t = title.trimStart()
                t.startsWith("Psaume", ignoreCase = true) || t.startsWith("المزمور") || t.startsWith("مزمور")
            }
            else -> false
        }

    val isGroupHeader: Boolean get() = kind == KIND_GROUP

    companion object {
        const val KIND_PSALM = "psalm"
        const val KIND_GROUP = "group"
    }
}

/**
 * Une partie telle qu'affichée : texte de la langue principale et, en mode bilingue, de la langue
 * secondaire (alignés par identifiant ; [secondary] null si la section manque dans cette langue).
 */
data class SectionPair(
    val id: Int,
    val primary: Section,
    val secondary: Section?
) {
    val title: String
        get() = bilingualTitle(primary.title, secondary?.title)

    val isPsalm: Boolean
        get() = primary.isPsalm

    /**
     * Faux si le contenu est vide ou blanc dans toutes les langues affichées
     * (ex. « Kyrie Eleison (41 fois) ») : la partie n'a alors ni corps ni repli.
     */
    val hasBody: Boolean
        get() = listOfNotNull(primary, secondary).any { it.content.isNotBlank() }
}

/**
 * Groupes de parties (« services » de la prière de minuit) : une partie d'en-tête de service suivie
 * des parties qui lui appartiennent. Source de vérité : `kind = "group"` + `groupEnd` dans le JSON.
 * Repli (contenu sans ces champs) : détection par titre — un service commence à son en-tête exact et
 * s'étend jusqu'au prochain en-tête ou, au plus tard, jusqu'au premier « Notre Père » rencontré.
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

    /** Calcule l'appartenance aux groupes d'une liste de parties (d'après la section principale). */
    fun compute(sections: List<SectionPair>): Grouping {
        val headers = LinkedHashSet<Int>()
        val parentOf = HashMap<Int, Int>()
        val declared = sections.any { it.primary.kind != null }
        var current: Int? = null
        var currentEnd: Int? = null
        for (section in sections) {
            val s = section.primary
            val header = if (declared) s.isGroupHeader else isGroupHeader(s.title)
            when {
                header -> {
                    headers.add(s.id)
                    current = s.id
                    currentEnd = if (declared) s.groupEnd else null
                }
                current != null -> {
                    parentOf[s.id] = current
                    val end = if (declared) currentEnd != null && s.id >= currentEnd else isGroupEnd(s.title)
                    if (end) {
                        current = null
                        currentEnd = null
                    }
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

/** Contenu d'une heure prêt à l'affichage : langue principale, plus la secondaire en mode bilingue. */
data class ChapterView(
    /** Chapitre principal (titre déjà bilingue en mode bilingue). */
    val chapter: Chapter,
    val sections: List<SectionPair>,
    /** Deux colonnes : principale en première colonne, secondaire en seconde. */
    val bilingual: Boolean,
    /** Sens de lecture de la langue secondaire (colonne 2). */
    val secondaryRtl: Boolean
) {
    val number: Int get() = chapter.number
    /** Sens de la mise en page : celui de la langue principale. */
    val isRtl: Boolean get() = chapter.isRtl
}

/** Analyse et validation stricte d'un chapitre JSON (même schéma que `scripts/build_content_manifest.py`). */
object ChapterParser {
    private val KEYS = setOf("id", "title", "content", "kind", "groupEnd")

    /**
     * @param strict vrai pour refuser toute déviation du schéma (contenu téléchargé) ; faux pour tolérer
     *   les clés inconnues (contenu embarqué, à ne jamais bloquer).
     */
    @Throws(JSONException::class)
    fun parse(json: String, expectedNumber: Int, language: String, rtl: Boolean, strict: Boolean): Chapter {
        val root = JSONObject(json)
        val number = root.optInt("number", expectedNumber)
        if (number != expectedNumber) throw JSONException("number = $number, attendu $expectedNumber")
        val title = root.getString("title").trim()
        if (title.isEmpty()) throw JSONException("Titre de chapitre vide")
        val array = root.getJSONArray("sections")
        if (array.length() == 0) throw JSONException("Chapitre sans section")
        val sections = ArrayList<Section>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.optInt("id", i + 1)
            if (id != i + 1) throw JSONException("Section #${i + 1} : id $id (ids contigus attendus)")
            if (strict) {
                for (key in obj.keys()) if (key !in KEYS) throw JSONException("Section $id : clé inconnue $key")
            }
            val sectionTitle = obj.getString("title").trim()
            if (sectionTitle.isEmpty()) throw JSONException("Section $id : titre vide")
            val kind = if (obj.has("kind") && !obj.isNull("kind")) obj.getString("kind") else null
            if (kind != null && kind != Section.KIND_PSALM && kind != Section.KIND_GROUP) {
                throw JSONException("Section $id : kind inconnu $kind")
            }
            val groupEnd = if (obj.has("groupEnd") && !obj.isNull("groupEnd")) obj.getInt("groupEnd") else null
            if (groupEnd != null) {
                if (kind != Section.KIND_GROUP) throw JSONException("Section $id : groupEnd réservé à kind=group")
                if (groupEnd <= id || groupEnd > array.length()) throw JSONException("Section $id : groupEnd $groupEnd hors limites")
            }
            sections.add(Section(id, sectionTitle, obj.getString("content"), kind, groupEnd))
        }
        return Chapter(number, title, sections, language, rtl)
    }
}

/**
 * Dépôt du contenu, entièrement JSON et multilingue :
 * - source : `filesDir/book/` (contenu téléchargé) s'il existe et est valide et au moins aussi récent
 *   que l'embarqué, sinon `assets/book/` ; une APK plus récente l'emporte et purge le cache téléchargé ;
 * - `manifest.json` liste les langues (code, nom, sens, locale d'interface) ;
 * - chapitres lus à la demande et mis en cache par langue ; repli silencieux sur « fr » si un chapitre
 *   manque dans la langue principale, colonne absente s'il manque dans la langue secondaire.
 *
 * [init] doit être appelé une fois (depuis [com.agpia.livre.LivreAgpiaApp]).
 */
object BookRepository {
    private const val TAG = "BookRepository"
    const val ASSET_DIR = "book"
    const val DOWNLOAD_DIR = "book"
    const val FALLBACK_LANGUAGE = PreferencesManager.LANG_FR

    /** D'où vient le contenu actif. */
    enum class Source { EMBEDDED, DOWNLOADED }

    private var appContext: Context? = null
    private var prefs: PreferencesManager? = null

    /** Manifeste embarqué dans l'APK (toujours disponible). */
    @Volatile
    lateinit var embeddedManifest: ContentManifest
        private set

    /** Manifeste du contenu actif. */
    @Volatile
    lateinit var manifest: ContentManifest
        private set

    @Volatile
    var source: Source = Source.EMBEDDED
        private set

    /** Cache : langue -> (numéro -> chapitre ou null si absent/invalide). */
    private val cache = HashMap<String, HashMap<Int, Chapter?>>()

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        prefs = PreferencesManager(app)
        embeddedManifest = readEmbeddedManifest(app)
        selectSource(app)
    }

    /**
     * Relit les manifestes et vide le cache mémoire (après une mise à jour ou une restauration).
     * Les écrans déjà affichés gardent leur contenu ; le nouveau est servi au prochain chargement
     * d'une heure.
     */
    @Synchronized
    fun reload() {
        val app = appContext ?: return
        cache.clear()
        selectSource(app)
    }

    fun downloadDir(context: Context): File = File(context.filesDir, DOWNLOAD_DIR)

    private fun readEmbeddedManifest(context: Context): ContentManifest {
        val json = context.assets.open("$ASSET_DIR/${ContentManifest.FILE_NAME}")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        return ContentManifest.parse(json)
    }

    private fun selectSource(context: Context) {
        val dir = downloadDir(context)
        val downloaded = readDownloadedManifest(dir)
        if (downloaded != null && downloaded.contentVersion > embeddedManifest.contentVersion) {
            manifest = downloaded
            source = Source.DOWNLOADED
        } else {
            if (downloaded != null) {
                Log.i(TAG, "Contenu embarqué (v${embeddedManifest.contentVersion}) ≥ téléchargé (v${downloaded.contentVersion}) : purge du cache")
            }
            if (dir.exists()) dir.deleteRecursively()
            manifest = embeddedManifest
            source = Source.EMBEDDED
        }
    }

    /** Manifeste téléchargé si le dossier est complet et intègre (tailles et sha256), sinon null. */
    private fun readDownloadedManifest(dir: File): ContentManifest? {
        val manifestFile = File(dir, ContentManifest.FILE_NAME)
        if (!manifestFile.isFile) return null
        return try {
            val parsed = ContentManifest.parse(manifestFile.readText(Charsets.UTF_8))
            if (parsed.schemaVersion > ContentManifest.SUPPORTED_SCHEMA_VERSION) {
                Log.w(TAG, "Contenu téléchargé : schéma ${parsed.schemaVersion} non supporté"); return null
            }
            for ((path, entry) in parsed.files) {
                val f = File(dir, path)
                if (!f.isFile || f.length() != entry.size || sha256(f) != entry.sha256) {
                    Log.w(TAG, "Contenu téléchargé corrompu ($path) : ignoré"); return null
                }
            }
            parsed
        } catch (e: JSONException) {
            Log.w(TAG, "Manifeste téléchargé invalide : ignoré", e); null
        } catch (e: IOException) {
            Log.w(TAG, "Manifeste téléchargé illisible : ignoré", e); null
        }
    }

    // ---- Langues ----

    /** Langues disponibles, dans l'ordre du manifeste. */
    val languages: List<LanguageInfo> get() = manifest.languages

    fun languageInfo(code: String): LanguageInfo? = manifest.language(code)

    /** Langue principale effective (repli « fr » ou première langue du manifeste si inconnue). */
    val primaryLanguage: String
        get() {
            val wanted = prefs?.getPrimaryLanguage() ?: FALLBACK_LANGUAGE
            return if (manifest.language(wanted) != null) wanted
            else manifest.language(FALLBACK_LANGUAGE)?.code ?: manifest.languages.first().code
        }

    /** Langue secondaire effective (null si aucune, inconnue ou égale à la principale). */
    val secondaryLanguage: String?
        get() {
            val wanted = prefs?.getSecondaryLanguage() ?: return null
            if (wanted == primaryLanguage || manifest.language(wanted) == null) return null
            return wanted
        }

    fun isRtl(language: String): Boolean = manifest.language(language)?.rtl ?: false

    /** Locale d'interface déclarée pour une langue (ou « fr »). */
    fun uiLocaleFor(language: String): String = manifest.language(language)?.uiLocale ?: FALLBACK_LANGUAGE

    // ---- Chapitres ----

    fun getTotalChapters(): Int = manifest.chapterCount

    /** Chapitre dans une langue donnée (null si absent ou invalide), mis en cache. */
    @Synchronized
    fun getChapterIn(language: String, chapterNumber: Int): Chapter? {
        val byNumber = cache.getOrPut(language) { HashMap() }
        if (byNumber.containsKey(chapterNumber)) return byNumber[chapterNumber]
        val chapter = loadChapter(language, chapterNumber)
        byNumber[chapterNumber] = chapter
        return chapter
    }

    /** Chapitre « principal » : langue principale, repli silencieux sur « fr ». */
    private fun primaryChapter(chapterNumber: Int): Chapter? =
        getChapterIn(primaryLanguage, chapterNumber)
            ?: if (primaryLanguage != FALLBACK_LANGUAGE) getChapterIn(FALLBACK_LANGUAGE, chapterNumber) else null

    /**
     * Chapitres tels qu'affichés dans les listes (accueil, tiroir, recherche) : langue principale,
     * titre bilingue si une langue secondaire est choisie.
     */
    fun getChapters(): List<Chapter> = (1..manifest.chapterCount).mapNotNull { getChapter(it) }

    fun getChapter(chapterNumber: Int): Chapter? {
        val primary = primaryChapter(chapterNumber) ?: return null
        val secondary = secondaryLanguage?.let { getChapterIn(it, chapterNumber) } ?: return primary
        return primary.copy(title = bilingualTitle(primary.title, secondary.title))
    }

    fun getSection(chapterNumber: Int, sectionId: Int): Section? =
        primaryChapter(chapterNumber)?.sections?.find { it.id == sectionId }

    /** Heure prête à l'affichage (sections alignées par identifiant en mode bilingue). */
    fun getChapterView(chapterNumber: Int): ChapterView? {
        val primary = primaryChapter(chapterNumber) ?: return null
        val secondaryCode = secondaryLanguage
        val secondary = secondaryCode?.let { getChapterIn(it, chapterNumber) }
        val secondaryById = secondary?.sections?.associateBy { it.id }.orEmpty()
        val sections = primary.sections.map { s -> SectionPair(s.id, s, secondaryById[s.id]) }
        return ChapterView(
            chapter = if (secondary != null) primary.copy(title = bilingualTitle(primary.title, secondary.title)) else primary,
            sections = sections,
            bilingual = secondary != null,
            secondaryRtl = secondary?.isRtl ?: false
        )
    }

    // ---- Chargement ----

    private fun loadChapter(language: String, chapterNumber: Int): Chapter? {
        val context = appContext ?: return null
        val info = manifest.language(language) ?: return null
        val path = manifest.chapterPath(language, chapterNumber)
        val json = try {
            openContent(context, path)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        } catch (e: IOException) {
            Log.w(TAG, "Chapitre illisible ($path)", e); null
        } ?: run {
            Log.w(TAG, "Chapitre absent ($path)"); return null
        }
        return try {
            ChapterParser.parse(json, chapterNumber, language, info.rtl, strict = false)
        } catch (e: JSONException) {
            Log.w(TAG, "Chapitre invalide ($path)", e); null
        }
    }

    /** Flux d'un fichier de contenu de la source active, ou null s'il n'existe pas. */
    private fun openContent(context: Context, relativePath: String): InputStream? = when (source) {
        Source.DOWNLOADED -> File(downloadDir(context), relativePath).takeIf { it.isFile }?.inputStream()
        Source.EMBEDDED -> openEmbedded(context, relativePath)
    }

    /** Flux d'un fichier embarqué dans l'APK, ou null s'il n'existe pas. */
    fun openEmbedded(context: Context, relativePath: String): InputStream? = try {
        context.assets.open("$ASSET_DIR/$relativePath")
    } catch (e: IOException) {
        null
    }

    fun sha256(file: File): String = file.inputStream().use { sha256(it) }

    fun sha256(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
