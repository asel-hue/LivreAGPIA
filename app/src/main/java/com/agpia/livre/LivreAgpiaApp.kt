package com.agpia.livre

import android.app.Application
import com.agpia.livre.data.BookRepository
import com.agpia.livre.utils.PreferencesManager

/**
 * Applique le thème (clair / sombre / système) une seule fois au démarrage,
 * afin que toutes les activités soient cohérentes, et donne au dépôt de contenu
 * l'accès aux assets (contenu arabe) et à la langue choisie.
 */
class LivreAgpiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BookRepository.init(this)
        PreferencesManager(this).applyNightMode()
    }
}
