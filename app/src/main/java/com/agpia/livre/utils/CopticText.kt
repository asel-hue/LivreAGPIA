package com.agpia.livre.utils

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.agpia.livre.R
import com.google.android.material.color.MaterialColors

/**
 * Mise en forme des séquences en écriture copte (bloc U+2C80–U+2CFF et lettres coptes du bloc grec
 * U+03E2–U+03EF, avec leurs signes combinants) : police Noto Sans Coptic (OFL, `res/font`),
 * corps légèrement plus grand et couleur `colorPrimary`, pour distinguer la ligne copte de sa traduction.
 */
object CopticText {

    /**
     * Une séquence copte : commence et finit par une lettre copte (ou un signe combinant),
     * et peut contenir des espaces et la ponctuation courante entre les mots.
     */
    private val COPTIC_RUN = Regex(
        "[\\u2C80-\\u2CFF\\u03E2-\\u03EF]" +
            "(?:[\\u2C80-\\u2CFF\\u03E2-\\u03EF\\u0300-\\u036F\\uFE24-\\uFE26 ,.;:·'’-]*" +
            "[\\u2C80-\\u2CFF\\u03E2-\\u03EF\\u0300-\\u036F\\uFE24-\\uFE26])?"
    )

    private var typeface: Typeface? = null

    private fun copticTypeface(context: Context): Typeface? {
        typeface?.let { return it }
        return ResourcesCompat.getFont(context, R.font.noto_sans_coptic).also { typeface = it }
    }

    /** Vrai si le texte contient au moins une lettre copte. */
    fun containsCoptic(text: CharSequence): Boolean = COPTIC_RUN.containsMatchIn(text)

    /**
     * Applique le style copte à chaque séquence copte de [text]. Retourne le texte inchangé
     * s'il n'en contient aucune (pas d'allocation inutile pour la grande majorité des parties).
     * @param extraSizeSp agrandissement du copte par rapport au corps de texte (sp).
     */
    fun style(context: Context, text: CharSequence, extraSizeSp: Float = 1f): CharSequence {
        if (!containsCoptic(text)) return text
        val font = copticTypeface(context)
        val color = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, 0)
        val extraPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, extraSizeSp, context.resources.displayMetrics
        )
        val spannable = SpannableString(text)
        COPTIC_RUN.findAll(text).forEach { match ->
            spannable.setSpan(
                CopticSpan(font, color, extraPx),
                match.range.first, match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    /**
     * Span personnalisée (API 24 : TypefaceSpan(Typeface) n'existe qu'à partir de l'API 28)
     * qui change police, taille et couleur d'une séquence copte.
     */
    private class CopticSpan(
        private val font: Typeface?,
        private val color: Int,
        private val extraSizePx: Float
    ) : MetricAffectingSpan() {

        override fun updateMeasureState(paint: TextPaint) = apply(paint)

        override fun updateDrawState(paint: TextPaint) {
            apply(paint)
            if (color != 0) paint.color = color
        }

        private fun apply(paint: TextPaint) {
            if (font != null) {
                // Conserve le gras éventuel du titre.
                val style = paint.typeface?.style ?: Typeface.NORMAL
                paint.typeface = Typeface.create(font, style)
            }
            paint.textSize += extraSizePx
        }
    }
}
