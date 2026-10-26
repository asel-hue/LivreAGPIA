package com.agpia.livre.data

import org.json.JSONException
import org.json.JSONObject

/** Une langue de contenu déclarée dans `book/manifest.json`. */
data class LanguageInfo(
    val code: String,
    val name: String,
    val nativeName: String,
    val rtl: Boolean,
    /** Locale d'interface à appliquer quand cette langue est la langue principale. */
    val uiLocale: String
)

data class FileEntry(val sha256: String, val size: Long)

/**
 * `book/manifest.json` : version du contenu, langues disponibles et empreintes des fichiers.
 * Schéma (schemaVersion 1) : voir `scripts/build_content_manifest.py`.
 */
data class ContentManifest(
    val schemaVersion: Int,
    val contentVersion: Int,
    val updatedAt: String,
    val chapterCount: Int,
    val languages: List<LanguageInfo>,
    val files: Map<String, FileEntry>
) {
    fun language(code: String): LanguageInfo? = languages.find { it.code == code }

    /** Chemins relatifs (`fr/chapter1.json`) des chapitres d'une langue, dans l'ordre. */
    fun chapterPath(language: String, number: Int): String = "$language/chapter$number.json"

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val FILE_NAME = "manifest.json"

        /** Analyse le manifeste ; lève [JSONException] si le schéma n'est pas respecté. */
        @Throws(JSONException::class)
        fun parse(json: String): ContentManifest {
            val root = JSONObject(json)
            val languagesArray = root.getJSONArray("languages")
            val languages = ArrayList<LanguageInfo>(languagesArray.length())
            for (i in 0 until languagesArray.length()) {
                val obj = languagesArray.getJSONObject(i)
                val code = obj.getString("code").trim()
                if (code.isEmpty()) throw JSONException("Langue sans code")
                languages.add(
                    LanguageInfo(
                        code = code,
                        name = obj.optString("name", code),
                        nativeName = obj.optString("nativeName", code),
                        rtl = obj.optBoolean("rtl", false),
                        uiLocale = obj.optString("uiLocale", code)
                    )
                )
            }
            if (languages.isEmpty()) throw JSONException("Aucune langue déclarée")
            val filesObj = root.getJSONObject("files")
            val files = LinkedHashMap<String, FileEntry>()
            for (key in filesObj.keys()) {
                val entry = filesObj.getJSONObject(key)
                val sha = entry.getString("sha256").lowercase()
                if (!sha.matches(Regex("[0-9a-f]{64}"))) throw JSONException("sha256 invalide pour $key")
                if (key.contains("..") || key.startsWith("/")) throw JSONException("Chemin invalide : $key")
                files[key] = FileEntry(sha, entry.getLong("size"))
            }
            val chapterCount = root.getInt("chapterCount")
            if (chapterCount <= 0) throw JSONException("chapterCount invalide")
            return ContentManifest(
                schemaVersion = root.getInt("schemaVersion"),
                contentVersion = root.getInt("contentVersion"),
                updatedAt = root.optString("updatedAt", ""),
                chapterCount = chapterCount,
                languages = languages,
                files = files
            )
        }
    }
}
