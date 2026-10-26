# Instructions pour migrer le contenu du livre AGPIA

## 📍 Où se trouve le contenu à migrer ?

Vous avez plusieurs options pour récupérer le contenu du livre :

### Option 1 : Ancienne application Android

Si vous avez le code source de l'ancienne application :

1. **Localiser le fichier de contenu**
   - Cherchez dans `res/raw/` ou `assets/`
   - Cherchez des fichiers `.xml`, `.json`, `.txt`
   - Ou cherchez dans le code Java/Kotlin (fichiers `.java` ou `.kt`)

2. **Formats possibles dans l'ancienne version**
   - Fichiers XML dans `res/values/` ou `res/xml/`
   - Fichiers texte dans `assets/`
   - Base de données SQLite
   - Code Java/Kotlin avec le contenu en dur

### Option 2 : Fichier texte externe

Si vous avez le contenu dans un fichier texte :

1. **Format recommandé** :
   ```
   Chapitre 1
   Titre du chapitre 1
   1. Texte du verset 1
   2. Texte du verset 2
   3. Texte du verset 3
   
   Chapitre 2
   Titre du chapitre 2
   1. Texte du verset 1
   2. Texte du verset 2
   ```

2. **Utiliser le script de conversion** :
   ```bash
   python scripts/convert_book_content.py votre_fichier.txt text
   ```

### Option 3 : Fichier JSON

Si vous avez le contenu en JSON :

```json
[
  {
    "number": 1,
    "title": "Chapitre 1",
    "verses": [
      {"number": 1, "text": "Texte du verset 1"},
      {"number": 2, "text": "Texte du verset 2"}
    ]
  }
]
```

Utiliser le script :
```bash
python scripts/convert_book_content.py votre_fichier.json json
```

### Option 4 : Saisie manuelle

Si vous avez le texte original (livre papier, PDF, etc.) :

1. Ouvrir `app/src/main/java/com/agpia/livre/data/BookContent.kt`
2. Remplacer le contenu dans la fonction `getChapters()`
3. Suivre la structure :
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

## 🔍 Comment trouver l'ancienne version ?

### Si vous avez l'APK de l'ancienne version

1. **Décompiler l'APK** (outils : jadx, apktool)
2. **Chercher le contenu** dans les ressources ou le code décompilé
3. **Extraire** le contenu vers un fichier texte ou JSON

### Si vous avez le projet Android Studio

1. Ouvrir le projet dans Android Studio
2. Chercher dans :
   - `app/src/main/res/raw/`
   - `app/src/main/assets/`
   - `app/src/main/java/` ou `app/src/main/kotlin/`
   - Fichiers XML dans `res/values/`

### Si vous avez seulement l'application installée

1. Utiliser `adb` pour extraire les données :
   ```bash
   adb pull /data/data/com.agpia.livre/databases/
   ```
2. Ouvrir la base de données avec DB Browser for SQLite
3. Exporter les données

## 📝 Étapes de migration

Une fois que vous avez localisé le contenu :

1. **Extraire le contenu** de l'ancienne source
2. **Formater** selon la structure requise (utiliser le script si possible)
3. **Vérifier** chapitre par chapitre avec le texte original
4. **Remplacer** le contenu dans `BookContent.kt`
5. **Tester** l'application

## 🆘 Besoin d'aide ?

Si vous pouvez me dire :
- Où se trouve l'ancienne version (dossier, fichier, etc.)
- Quel format utilise l'ancienne version
- Si vous avez des fichiers spécifiques à analyser

Je pourrai vous aider à extraire et convertir le contenu automatiquement !

