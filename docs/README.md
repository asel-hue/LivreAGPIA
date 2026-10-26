# Application Android - Livre AGPIA

Application Android moderne pour la lecture du livre de l'AGPIA, conforme aux dernières normes Android.

## 📋 Caractéristiques

- ✅ **Conforme aux dernières versions d'Android** (API 24+, Target SDK 34)
- ✅ **Material Design 3** pour une interface moderne
- ✅ **Architecture moderne** avec Kotlin et ViewBinding
- ✅ **Navigation intuitive** entre les chapitres
- ✅ **Interface utilisateur responsive** et accessible

## 🚀 Installation

### Prérequis

- Android Studio Hedgehog (2023.1.1) ou plus récent
- JDK 17 ou plus récent
- Android SDK avec API 34

### Étapes d'installation

1. **Ouvrir le projet dans Android Studio**
   - File → Open → Sélectionner le dossier du projet

2. **Synchroniser Gradle**
   - Android Studio devrait synchroniser automatiquement
   - Sinon : File → Sync Project with Gradle Files

3. **Compiler et exécuter**
   - Connecter un appareil Android ou lancer un émulateur
   - Cliquer sur Run (▶️) ou appuyer sur `Shift+F10`

## 📝 Mise à jour du contenu du livre

### ⚠️ IMPORTANT : Vérification du texte original

Avant de publier l'application, il est **essentiel** de vérifier et mettre à jour le contenu avec le texte original du livre AGPIA.

### Étapes pour mettre à jour le contenu

1. **Localiser le fichier de contenu**
   - Ouvrir : `app/src/main/java/com/agpia/livre/data/BookContent.kt`

2. **Obtenir le texte original**
   - Récupérer le texte original du livre AGPIA (format texte, PDF, ou autre source fiable)

3. **Remplacer le contenu d'exemple**
   - Dans la fonction `getChapters()`, remplacer les chapitres d'exemple par le contenu réel
   - Structure à respecter :
     ```kotlin
     Chapter(
         number = 1,
         title = "Titre du chapitre",
         verses = listOf(
             Verse(1, "Texte du verset 1"),
             Verse(2, "Texte du verset 2"),
             // ...
         )
     )
     ```

4. **Vérification chapitre par chapitre**
   - Comparer chaque chapitre avec le texte original
   - Vérifier la numérotation des versets
   - S'assurer que tout le texte est présent et correct

5. **Test de l'application**
   - Compiler et tester l'application
   - Vérifier que tous les chapitres s'affichent correctement
   - Tester la navigation entre les chapitres

### Exemple de structure complète

```kotlin
fun getChapters(): List<Chapter> {
    return listOf(
        Chapter(
            number = 1,
            title = "Premier chapitre",
            verses = listOf(
                Verse(1, "Texte complet du premier verset..."),
                Verse(2, "Texte complet du deuxième verset..."),
                // Continuer pour tous les versets
            )
        ),
        Chapter(
            number = 2,
            title = "Deuxième chapitre",
            verses = listOf(
                // Versets du chapitre 2
            )
        ),
        // Ajouter tous les chapitres du livre
    )
}
```

## 🔧 Configuration

### Modifier les couleurs et le thème

Les couleurs peuvent être modifiées dans :
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/themes.xml`

### Modifier les textes de l'interface

Les textes de l'interface sont dans :
- `app/src/main/res/values/strings.xml`

## 📱 Fonctionnalités

### Écran principal (MainActivity)
- Liste de tous les chapitres du livre
- Affichage du nombre de versets par chapitre
- Navigation vers chaque chapitre

### Écran de lecture (ReadingActivity)
- Affichage complet d'un chapitre avec tous ses versets
- Navigation vers le chapitre précédent/suivant
- Boutons de navigation en bas de l'écran
- Défilement fluide du contenu

## 🆕 Améliorations futures possibles

- [ ] Recherche dans le texte
- [ ] Marque-pages
- [ ] Taille de police ajustable
- [ ] Mode sombre/clair
- [ ] Partage de versets
- [ ] Notes personnelles
- [ ] Synchronisation de la position de lecture

## 📦 Structure du projet

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/agpia/livre/
│   │   │   ├── MainActivity.kt          # Écran principal avec liste des chapitres
│   │   │   ├── ReadingActivity.kt       # Écran de lecture
│   │   │   ├── data/
│   │   │   │   └── BookContent.kt       # ⚠️ CONTENU DU LIVRE (À METTRE À JOUR)
│   │   │   └── ui/
│   │   │       └── ChapterAdapter.kt    # Adaptateur pour la liste des chapitres
│   │   ├── res/
│   │   │   ├── layout/                  # Layouts XML
│   │   │   ├── values/                  # Ressources (strings, colors, themes)
│   │   │   └── menu/                    # Menus
│   │   └── AndroidManifest.xml
│   └── build.gradle                      # Configuration Gradle
```

## 🔍 Vérification de conformité

### Checklist avant publication

- [ ] Contenu du livre vérifié et mis à jour avec le texte original
- [ ] Tous les chapitres sont présents
- [ ] Tous les versets sont présents et corrects
- [ ] L'application compile sans erreur
- [ ] L'application fonctionne sur différentes versions d'Android (testé sur API 24+)
- [ ] L'interface est responsive et fonctionne sur différentes tailles d'écran
- [ ] Les textes sont lisibles et bien formatés

## 📄 Licence

À définir selon vos besoins.

## 👤 Support

Pour toute question ou problème, veuillez vérifier :
1. Que le contenu du livre est correctement formaté dans `BookContent.kt`
2. Que toutes les dépendances sont installées
3. Que l'environnement de développement est correctement configuré

---

**Note importante** : Cette application est une version mise à jour et modernisée. Assurez-vous de comparer le contenu avec l'ancienne version pour vérifier qu'aucun texte n'a été perdu lors de la migration.
