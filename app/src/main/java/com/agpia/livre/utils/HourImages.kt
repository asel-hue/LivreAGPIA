package com.agpia.livre.utils

import android.annotation.SuppressLint
import android.content.Context

/**
 * Identifiant de l'illustration d'une heure : `res/drawable-nodpi/hour_N.webp`
 * (fournie séparément). Retourne 0 si la ressource n'existe pas (l'appelant masque l'image).
 */
@SuppressLint("DiscouragedApi")
fun Context.hourImageRes(chapterNumber: Int): Int =
    resources.getIdentifier("hour_$chapterNumber", "drawable", packageName)
