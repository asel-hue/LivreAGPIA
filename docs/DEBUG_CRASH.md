# Débogage du crash au démarrage

## Problème
L'application se ferme immédiatement après l'ouverture.

## Solutions de débogage

### 1. Vérifier les logs Android (RECOMMANDÉ)

#### Via Android Studio :
1. Connecter l'appareil ou lancer un émulateur
2. Dans Android Studio, ouvrir **View → Tool Windows → Logcat**
3. Filtrer par **"MainActivity"** ou **"AndroidRuntime"**
4. Lancer l'application
5. Regarder les erreurs en rouge dans Logcat

#### Via ADB (ligne de commande) :
```bash
adb logcat | grep -i "AndroidRuntime\|MainActivity\|FATAL"
```

### 2. Vérifications courantes

#### ✅ Ressources manquantes
- Vérifier que tous les layouts existent :
  - `activity_main.xml`
  - `item_chapter.xml`
- Vérifier que tous les drawables existent :
  - `ic_bookmark_border.xml`
- Vérifier que tous les strings existent dans `strings.xml`

#### ✅ Code Kotlin
- Vérifier que `BookRepository.getChapters()` retourne bien des données
- Vérifier que `ChapterAdapter` est correctement initialisé
- Vérifier qu'il n'y a pas d'exception dans `onCreate()`

### 3. Modifications appliquées

J'ai ajouté une gestion d'erreur dans `MainActivity.kt` :
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
        // ... code existant ...
    } catch (e: Exception) {
        e.printStackTrace()
        android.util.Log.e("MainActivity", "Erreur lors de l'initialisation", e)
        finish()
    }
}
```

Cela permettra de voir l'erreur exacte dans Logcat.

### 4. Erreurs courantes et solutions

#### Erreur : "ResourceNotFoundException"
**Solution :** Vérifier que toutes les ressources référencées existent

#### Erreur : "NullPointerException"
**Solution :** Vérifier que tous les objets sont initialisés avant utilisation

#### Erreur : "ClassNotFoundException"
**Solution :** Vérifier que toutes les classes sont correctement importées

#### Erreur : "IllegalStateException"
**Solution :** Vérifier que les méthodes sont appelées dans le bon ordre

### 5. Test minimal

Pour isoler le problème, vous pouvez temporairement simplifier `MainActivity.onCreate()` :

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    // Commenter le reste pour voir si le layout se charge
}
```

Si cela fonctionne, le problème vient du code après `setContentView()`.

### 6. Vérification du contenu

Vérifier que `BookRepository.getChapters()` retourne bien des données :
- Ouvrir `BookContent.kt`
- Vérifier que la fonction `getChapters()` retourne une liste non vide
- Vérifier qu'il n'y a pas d'erreur de syntaxe (virgules manquantes, etc.)

## Prochaines étapes

1. **Vérifier les logs** dans Logcat pour voir l'erreur exacte
2. **Partager l'erreur** complète pour une solution ciblée
3. **Tester avec le code simplifié** pour isoler le problème

## Note importante

Les modifications ajoutées permettront de capturer l'erreur et de l'afficher dans Logcat. Cela facilitera grandement le débogage.


