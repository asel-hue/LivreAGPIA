package com.agpia.livre.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.agpia.livre.R
import com.copte.calendar.LiturgicalCalendar
import com.copte.calendar.LiturgyContract
import java.time.LocalDate

/**
 * Jour copte de l'accueil, via le contrat d'intégration de `coptic-calendar-core` :
 * - la date copte est calculée localement (aucune dépendance à KatamerosCopte) ;
 * - le titre du jour (fête, sinon temps liturgique) est lu dans le ContentProvider de
 *   KatamerosCopte quand elle est installée et signée avec la même clé ;
 * - « Lectures du jour » ouvre KatamerosCopte par lien `copte://jour/{date}`.
 */
object CopticToday {
    private const val TAG = "CopticToday"

    /** Langues proposées par le fournisseur de KatamerosCopte. */
    private val PROVIDER_LANGS = setOf("fr", "ar", "en")

    /** Date copte du jour, mois traduit : « 16 Hatour 1743 ap. M. » / « ١٦ هاتور ١٧٤٣ ش ». */
    fun copticDateLabel(context: Context, date: LocalDate = LocalDate.now()): String {
        val coptic = LiturgicalCalendar.dayOf(date).coptic
        val months = context.resources.getStringArray(R.array.coptic_months)
        return context.getString(
            R.string.coptic_date_format,
            coptic.day,
            months[coptic.month.number - 1],
            coptic.year,
        )
    }

    /** Vrai si KatamerosCopte (fournisseur du jour liturgique) est installée et visible. */
    fun isKatamerosInstalled(context: Context): Boolean =
        context.packageManager.resolveContentProvider(LiturgyContract.AUTHORITY, 0) != null

    /**
     * Titre du jour lu chez KatamerosCopte, dans la langue de l'interface, ou null :
     * application absente (curseur null), signature différente (SecurityException) ou titre vide.
     * À appeler hors du thread principal.
     */
    fun liturgicalTitle(context: Context): String? {
        val lang = context.resources.configuration.locales[0].language
            .takeIf { it in PROVIDER_LANGS } ?: "fr"
        return try {
            context.contentResolver
                .query(Uri.parse(LiturgyContract.todayUri(lang)), null, null, null, null)
                ?.use { c ->
                    if (!c.moveToFirst()) return null
                    val i = c.getColumnIndex(LiturgyContract.Columns.TITLE)
                    if (i < 0) null else c.getString(i)?.takeIf { it.isNotBlank() }
                }
        } catch (e: SecurityException) {
            // KatamerosCopte n'est pas signée avec la même clé : pas d'accès au jour liturgique.
            Log.i(TAG, "Fournisseur KatamerosCopte refusé : ${e.message}")
            null
        } catch (e: RuntimeException) {
            Log.w(TAG, "Lecture du jour liturgique impossible", e)
            null
        }
    }

    /** Ouvre les lectures du jour dans KatamerosCopte. Faux si aucune application ne gère le lien. */
    fun openReadings(context: Context, date: LocalDate = LocalDate.now()): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LiturgyContract.dayLink(date))))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
