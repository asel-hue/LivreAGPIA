package com.agpia.livre.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.agpia.livre.BuildConfig
import com.agpia.livre.utils.PreferencesManager
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mise à jour du contenu à distance, sans republier l'application.
 *
 * Flux : télécharge `manifest.json` depuis [baseUrl] ; si sa `contentVersion` dépasse la version locale
 * (max(embarquée, téléchargée)), prépare un dossier temporaire avec tous les fichiers du manifeste
 * (copiés depuis la version locale quand le sha256 est identique, téléchargés sinon), vérifie chaque
 * sha256/taille, valide le JSON (schéma, ids contigus, cohérence entre langues), écrit le manifeste puis
 * bascule atomiquement vers `filesDir/book/`. Toute erreur laisse l'ancien contenu en place.
 *
 * Exécution sur un thread dédié ; résultat livré sur le thread principal.
 */
object ContentUpdater {
    private const val TAG = "ContentUpdater"
    private const val TIMEOUT_MS = 10_000
    private const val MAX_FILE_BYTES = 8L * 1024 * 1024
    private const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

    sealed class Result {
        object UpToDate : Result()
        data class Updated(val version: Int) : Result()
        /** [network] vrai si l'échec vient du réseau (hors ligne, délai, HTTP), faux si le contenu est invalide. */
        data class Failed(val reason: String, val network: Boolean) : Result()
    }

    private class ContentException(message: String, val network: Boolean = false, cause: Throwable? = null) :
        Exception(message, cause)

    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "content-updater") }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)

    /** URL de base effective : surcharge de debug (préférence cachée) ou [BuildConfig.CONTENT_BASE_URL]. */
    fun baseUrl(context: Context): String {
        val override = if (BuildConfig.DEBUG) PreferencesManager(context).getContentBaseUrlOverride() else null
        val url = override?.takeIf { it.isNotBlank() } ?: BuildConfig.CONTENT_BASE_URL
        return if (url.endsWith("/")) url else "$url/"
    }

    /** Vérification silencieuse au démarrage, au plus une fois par 24 h. */
    fun checkIfDue(context: Context, callback: (Result) -> Unit) {
        val prefs = PreferencesManager(context)
        val last = prefs.getLastUpdateCheck()
        val now = System.currentTimeMillis()
        if (now - last in 0 until CHECK_INTERVAL_MS) return
        check(context, callback)
    }

    /** Vérification immédiate (bouton des Paramètres). Une seule à la fois. */
    fun check(context: Context, callback: (Result) -> Unit) {
        val app = context.applicationContext
        if (!running.compareAndSet(false, true)) return
        executor.execute {
            val result = try {
                update(app)
            } catch (e: ContentException) {
                Log.w(TAG, "Mise à jour du contenu abandonnée : ${e.message}", e.cause)
                Result.Failed(e.message ?: "", e.network)
            } catch (e: Exception) {
                Log.w(TAG, "Mise à jour du contenu : erreur inattendue", e)
                Result.Failed(e.toString(), network = false)
            } finally {
                PreferencesManager(app).setLastUpdateCheck(System.currentTimeMillis())
                running.set(false)
            }
            mainHandler.post { callback(result) }
        }
    }

    /** Supprime le contenu téléchargé et revient au contenu embarqué. */
    fun restoreEmbedded(context: Context) {
        val dir = BookRepository.downloadDir(context.applicationContext)
        if (dir.exists()) dir.deleteRecursively()
        BookRepository.reload()
    }

    // ---- Travail de fond ----

    private fun update(context: Context): Result {
        val base = baseUrl(context)
        val remoteJson = String(download("$base${ContentManifest.FILE_NAME}"), Charsets.UTF_8)
        val remote = try {
            ContentManifest.parse(remoteJson)
        } catch (e: JSONException) {
            throw ContentException("Manifeste distant invalide", cause = e)
        }
        if (remote.schemaVersion > ContentManifest.SUPPORTED_SCHEMA_VERSION) {
            throw ContentException("Schéma ${remote.schemaVersion} non supporté par cette version de l'application")
        }
        val localVersion = maxOf(
            BookRepository.embeddedManifest.contentVersion,
            if (BookRepository.source == BookRepository.Source.DOWNLOADED) BookRepository.manifest.contentVersion else 0
        )
        if (remote.contentVersion <= localVersion) {
            Log.i(TAG, "Contenu à jour (local v$localVersion, distant v${remote.contentVersion})")
            return Result.UpToDate
        }
        Log.i(TAG, "Nouveau contenu disponible : v${remote.contentVersion} (local v$localVersion)")

        val target = BookRepository.downloadDir(context)
        val staging = File(context.filesDir, "${BookRepository.DOWNLOAD_DIR}_tmp")
        val old = File(context.filesDir, "${BookRepository.DOWNLOAD_DIR}_old")
        staging.deleteRecursively()
        old.deleteRecursively()
        try {
            if (!staging.mkdirs()) throw ContentException("Impossible de créer le dossier temporaire")
            for ((path, entry) in remote.files) {
                val bytes = reuseLocal(context, target, path, entry) ?: downloadVerified("$base$path", entry, path)
                val out = File(staging, path)
                out.parentFile?.mkdirs()
                out.writeBytes(bytes)
            }
            validateContent(staging, remote)
            File(staging, ContentManifest.FILE_NAME).writeText(remoteJson, Charsets.UTF_8)

            // Bascule atomique (renommages de dossiers) avec retour arrière en cas d'échec.
            if (target.exists() && !target.renameTo(old)) throw ContentException("Impossible d'écarter l'ancien contenu")
            if (!staging.renameTo(target)) {
                if (old.exists()) old.renameTo(target)
                throw ContentException("Impossible d'installer le nouveau contenu")
            }
            old.deleteRecursively()
        } finally {
            staging.deleteRecursively()
        }
        BookRepository.reload()
        return Result.Updated(remote.contentVersion)
    }

    /** Octets d'un fichier déjà présent localement (téléchargé ou embarqué) avec la même empreinte, sinon null. */
    private fun reuseLocal(context: Context, downloaded: File, path: String, entry: FileEntry): ByteArray? {
        val candidates = sequence {
            File(downloaded, path).takeIf { it.isFile }?.let { yield(it.readBytes()) }
            BookRepository.openEmbedded(context, path)?.use { yield(it.readBytes()) }
        }
        return candidates.firstOrNull { it.size.toLong() == entry.size && BookRepository.sha256(it.inputStream()) == entry.sha256 }
    }

    private fun downloadVerified(url: String, entry: FileEntry, path: String): ByteArray {
        val bytes = download(url)
        if (bytes.size.toLong() != entry.size) throw ContentException("Taille inattendue pour $path")
        if (BookRepository.sha256(bytes.inputStream()) != entry.sha256) throw ContentException("Empreinte sha256 invalide pour $path")
        return bytes
    }

    private fun download(url: String): ByteArray {
        val connection = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/json, */*")
                setRequestProperty("Cache-Control", "no-cache")
            }
        } catch (e: IOException) {
            throw ContentException("URL invalide : $url", network = true, cause = e)
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw ContentException("HTTP $code pour $url", network = true)
            return connection.inputStream.use { readLimited(it) }
        } catch (e: IOException) {
            throw ContentException("Réseau indisponible ($url)", network = true, cause = e)
        } finally {
            connection.disconnect()
        }
    }

    private fun readLimited(stream: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            total += read
            if (total > MAX_FILE_BYTES) throw ContentException("Fichier trop volumineux")
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    /** Validation complète du contenu préparé : chaque chapitre, puis cohérence structurelle entre langues. */
    private fun validateContent(dir: File, manifest: ContentManifest) {
        val reference = HashMap<Int, List<Triple<Int, String?, Int?>>>()
        for (language in manifest.languages) {
            // Une langue déclarée sans aucun fichier est tolérée (dossier à venir) ; sinon elle doit être complète.
            if (manifest.files.keys.none { it.startsWith("${language.code}/") }) {
                Log.w(TAG, "Langue ${language.code} déclarée sans contenu"); continue
            }
            for (number in 1..manifest.chapterCount) {
                val path = manifest.chapterPath(language.code, number)
                if (path !in manifest.files) {
                    throw ContentException("Fichier manquant dans le manifeste : $path")
                }
                val file = File(dir, path)
                val chapter = try {
                    ChapterParser.parse(file.readText(Charsets.UTF_8), number, language.code, language.rtl, strict = true)
                } catch (e: JSONException) {
                    throw ContentException("$path invalide : ${e.message}", cause = e)
                }
                val signature = chapter.sections.map { Triple(it.id, it.kind, it.groupEnd) }
                val ref = reference[number]
                if (ref == null) reference[number] = signature
                else if (ref != signature) throw ContentException("$path : structure différente des autres langues")
            }
        }
    }
}
