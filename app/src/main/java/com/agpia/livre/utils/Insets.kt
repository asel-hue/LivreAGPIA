package com.agpia.livre.utils

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Helpers edge-to-edge (obligatoire à partir d'Android 15 / targetSdk 35) :
 * ajoute les insets système (barres d'état / de navigation / clavier) au padding initial de la vue.
 */
fun View.applySystemBarInsets(
    left: Boolean = true,
    top: Boolean = false,
    right: Boolean = true,
    bottom: Boolean = false,
    includeIme: Boolean = false
) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        var mask = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        if (includeIme) mask = mask or WindowInsetsCompat.Type.ime()
        val insets: Insets = windowInsets.getInsets(mask)
        view.setPadding(
            initialLeft + if (left) insets.left else 0,
            initialTop + if (top) insets.top else 0,
            initialRight + if (right) insets.right else 0,
            initialBottom + if (bottom) insets.bottom else 0
        )
        windowInsets
    }
}
