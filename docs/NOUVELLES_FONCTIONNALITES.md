# Nouvelles fonctionnalités ajoutées

## ✅ Fonctionnalités implémentées

Toutes les fonctionnalités demandées ont été ajoutées à l'application Android AGPIA :

### 1. 🔍 Recherche dans le texte
- **Activité de recherche** (`SearchActivity`) avec recherche en temps réel
- Recherche dans tous les chapitres et versets
- Affichage des résultats avec contexte autour des correspondances
- Navigation directe vers le verset trouvé
- Accessible depuis le menu principal et l'écran de lecture

### 2. 📑 Marque-pages
- **Système de marque-pages** avec stockage local (SharedPreferences)
- Ajout/suppression de marque-pages depuis l'écran de lecture
- **Activité des marque-pages** (`BookmarksActivity`) pour voir tous les marque-pages
- Les versets marqués sont mis en évidence en couleur dans le texte
- Navigation directe depuis la liste des marque-pages
- Accessible depuis le menu principal

### 3. 🔤 Ajustement de la taille de police
- **Slider de taille de police** dans les paramètres (12sp à 28sp)
- Taille par défaut : 18sp
- Application immédiate sur le texte de lecture
- Préférence sauvegardée entre les sessions

### 4. 🌙 Mode sombre/clair
- **Switch pour activer/désactiver le mode sombre** dans les paramètres
- Utilisation de `AppCompatDelegate.MODE_NIGHT_YES/NO`
- Application immédiate avec rechargement de l'activité
- Préférence sauvegardée entre les sessions

### 5. 📤 Partage de versets
- **Bouton de partage** dans le menu de lecture
- Partage du verset actuel avec titre du chapitre
- Utilisation de l'Intent standard Android `ACTION_SEND`
- Compatible avec toutes les applications de partage (SMS, email, réseaux sociaux, etc.)

### 6. 📝 Notes personnelles
- **Dialog pour ajouter/modifier des notes** sur les versets
- Notes sauvegardées localement (SharedPreferences)
- Affichage et édition des notes existantes
- Suppression des notes (en laissant vide)
- Accessible depuis le menu de lecture

## 📁 Fichiers créés/modifiés

### Nouveaux fichiers Kotlin
- `app/src/main/java/com/agpia/livre/utils/PreferencesManager.kt` - Gestion des préférences
- `app/src/main/java/com/agpia/livre/ui/SearchActivity.kt` - Activité de recherche
- `app/src/main/java/com/agpia/livre/ui/SearchAdapter.kt` - Adaptateur pour les résultats de recherche
- `app/src/main/java/com/agpia/livre/ui/SettingsActivity.kt` - Activité des paramètres
- `app/src/main/java/com/agpia/livre/ui/BookmarksActivity.kt` - Activité des marque-pages
- `app/src/main/java/com/agpia/livre/ui/BookmarksAdapter.kt` - Adaptateur pour les marque-pages
- `app/src/main/java/com/agpia/livre/ui/NoteDialog.kt` - Dialog pour les notes

### Nouveaux fichiers XML
- `app/src/main/res/layout/activity_search.xml` - Layout de recherche
- `app/src/main/res/layout/item_search_result.xml` - Item de résultat de recherche
- `app/src/main/res/layout/activity_settings.xml` - Layout des paramètres
- `app/src/main/res/layout/activity_bookmarks.xml` - Layout des marque-pages
- `app/src/main/res/layout/item_bookmark.xml` - Item de marque-page
- `app/src/main/res/layout/dialog_note.xml` - Layout du dialog de note
- `app/src/main/res/menu/main_menu.xml` - Menu principal
- `app/src/main/res/drawable/ic_bookmark_border.xml` - Icône marque-page vide
- `app/src/main/res/drawable/ic_bookmark_filled.xml` - Icône marque-page rempli

### Fichiers modifiés
- `app/src/main/java/com/agpia/livre/MainActivity.kt` - Ajout du menu et navigation
- `app/src/main/java/com/agpia/livre/ReadingActivity.kt` - Intégration de toutes les fonctionnalités
- `app/src/main/res/menu/reading_menu.xml` - Menu enrichi avec toutes les options
- `app/src/main/res/values/strings.xml` - Nouvelles chaînes de caractères
- `app/src/main/res/values/colors.xml` - Couleur pour les marque-pages
- `app/src/main/AndroidManifest.xml` - Déclaration des nouvelles activités

## 🎯 Utilisation

### Recherche
1. Cliquer sur l'icône de recherche dans le menu principal ou l'écran de lecture
2. Taper votre recherche dans le champ
3. Les résultats s'affichent en temps réel
4. Cliquer sur un résultat pour naviguer vers le verset

### Marque-pages
1. Dans l'écran de lecture, cliquer sur l'icône de marque-page dans le menu
2. Le verset est marqué (icône change et texte coloré)
3. Accéder à tous les marque-pages via le menu principal
4. Supprimer un marque-page depuis la liste ou l'écran de lecture

### Taille de police
1. Ouvrir les paramètres depuis le menu de lecture
2. Ajuster le slider de taille de police
3. La taille s'applique immédiatement

### Mode sombre
1. Ouvrir les paramètres depuis le menu de lecture
2. Activer/désactiver le switch "Mode sombre"
3. L'application se recharge avec le nouveau thème

### Partage
1. Dans l'écran de lecture, cliquer sur l'icône de partage
2. Choisir l'application de destination
3. Le verset est partagé avec le titre du chapitre

### Notes
1. Dans l'écran de lecture, cliquer sur "Ajouter une note" dans le menu
2. Taper votre note (max 500 caractères)
3. Enregistrer - la note est associée au verset
4. Réouvrir le dialog pour modifier ou supprimer la note

## 🔧 Architecture technique

### Stockage des données
- **SharedPreferences** pour toutes les préférences utilisateur
- Format simple et efficace pour les marque-pages et notes
- Pas de base de données nécessaire pour cette application

### Navigation
- Utilisation d'Intents pour la navigation entre activités
- `startActivityForResult` pour la navigation depuis recherche/marque-pages
- Support du scroll automatique vers un verset spécifique

### Thème
- Support du mode sombre via `AppCompatDelegate`
- Application immédiate avec `recreate()` de l'activité
- Thème Material Design 3

## ⚠️ Notes importantes

1. **Les marque-pages** sont actuellement associés au premier verset du chapitre. Pour une version future, on pourrait permettre de marquer n'importe quel verset individuellement.

2. **Les notes** sont également associées au premier verset. Même remarque pour une amélioration future.

3. **Le partage** partage actuellement le premier verset du chapitre. On pourrait améliorer pour permettre de sélectionner un verset spécifique.

4. **La recherche** est insensible à la casse et recherche dans tout le texte.

5. Toutes les données sont stockées localement sur l'appareil. En cas de désinstallation, les données seront perdues.

## 🚀 Prochaines améliorations possibles

- [ ] Sélection de verset individuel pour marque-pages et notes
- [ ] Export/import des marque-pages et notes
- [ ] Synchronisation cloud (optionnelle)
- [ ] Historique de lecture
- [ ] Statistiques de lecture
- [ ] Filtres avancés dans la recherche
- [ ] Recherche par chapitre ou verset

---

**Toutes les fonctionnalités sont opérationnelles et prêtes à être testées !** 🎉


