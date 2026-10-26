# Guide de mise à jour depuis l'ancienne version

Ce guide vous aidera à migrer le contenu de votre ancienne application Android vers cette nouvelle version modernisée.

## 🔄 Comparaison avec l'ancienne version

### Points à vérifier

1. **Structure des données**
   - Vérifier comment les chapitres et versets étaient stockés dans l'ancienne version
   - S'assurer que tous les chapitres sont présents dans la nouvelle version
   - Vérifier que la numérotation des versets est identique

2. **Contenu textuel**
   - Comparer chapitre par chapitre avec l'ancienne version
   - Vérifier qu'aucun texte n'a été modifié ou perdu
   - S'assurer que la ponctuation et la mise en forme sont correctes

3. **Fonctionnalités**
   - Vérifier que toutes les fonctionnalités de l'ancienne version sont présentes
   - Tester la navigation entre les chapitres
   - Vérifier l'affichage sur différents appareils

## 📝 Processus de migration du contenu

### Étape 1 : Extraire le contenu de l'ancienne version

Si vous avez accès à l'ancienne application :

1. **Méthode 1 : Code source**
   - Ouvrir le projet de l'ancienne application
   - Localiser où le contenu du livre est stocké (peut être dans des fichiers XML, JSON, ou directement dans le code)
   - Copier le contenu

2. **Méthode 2 : Base de données**
   - Si l'ancienne version utilisait une base de données SQLite
   - Extraire les données avec un outil comme DB Browser for SQLite
   - Exporter en format CSV ou JSON

3. **Méthode 3 : Fichiers de ressources**
   - Chercher dans `res/raw/` ou `assets/`
   - Les fichiers peuvent être en format texte, XML, ou JSON

### Étape 2 : Formater le contenu

Le contenu doit être formaté selon la structure suivante :

```kotlin
Chapter(
    number = [NUMÉRO_DU_CHAPITRE],
    title = "[TITRE_DU_CHAPITRE]",
    verses = listOf(
        Verse(1, "[TEXTE_DU_VERSET_1]"),
        Verse(2, "[TEXTE_DU_VERSET_2]"),
        // ...
    )
)
```

### Étape 3 : Vérification

1. **Vérification quantitative**
   - Compter le nombre de chapitres dans l'ancienne version
   - Compter le nombre de versets par chapitre
   - Comparer avec la nouvelle version

2. **Vérification qualitative**
   - Lire quelques chapitres au hasard
   - Comparer mot par mot avec l'ancienne version
   - Vérifier la ponctuation et les espaces

3. **Test de l'application**
   - Compiler et installer la nouvelle application
   - Parcourir tous les chapitres
   - Vérifier l'affichage et la navigation

## 🔍 Points d'attention spécifiques

### Formatage du texte

- **Espaces** : Vérifier qu'il n'y a pas d'espaces en trop ou manquants
- **Retours à la ligne** : Les versets sont séparés par des retours à la ligne dans l'affichage
- **Caractères spéciaux** : Vérifier que les caractères spéciaux (accents, apostrophes, etc.) sont correctement encodés

### Numérotation

- **Chapitres** : S'assurer que la numérotation commence à 1 et est continue
- **Versets** : Vérifier que chaque chapitre commence à 1 et que la numérotation est continue

### Titres des chapitres

- Vérifier que les titres des chapitres sont corrects
- S'assurer qu'ils correspondent à l'ancienne version

## 📋 Checklist de migration

- [ ] Contenu extrait de l'ancienne version
- [ ] Contenu formaté selon la nouvelle structure
- [ ] Nombre de chapitres vérifié
- [ ] Nombre de versets par chapitre vérifié
- [ ] Échantillon de chapitres vérifié mot par mot
- [ ] Application compilée sans erreur
- [ ] Tous les chapitres s'affichent correctement
- [ ] Navigation entre chapitres fonctionne
- [ ] Test sur différents appareils Android
- [ ] Comparaison finale avec l'ancienne version

## 🆘 Aide supplémentaire

Si vous rencontrez des difficultés :

1. **Problème de formatage**
   - Vérifier que les guillemets sont correctement échappés dans Kotlin
   - Utiliser des chaînes de caractères brutes (triple guillemets) si nécessaire

2. **Problème d'encodage**
   - S'assurer que les fichiers sont en UTF-8
   - Vérifier que les caractères spéciaux s'affichent correctement

3. **Problème de performance**
   - Si le livre est très volumineux, considérer l'utilisation d'une base de données
   - Pour l'instant, le contenu est chargé en mémoire

## 📞 Support

Si vous avez besoin d'aide pour la migration, assurez-vous d'avoir :
- L'ancienne version du code source (si disponible)
- Le texte original du livre AGPIA
- Les informations sur la structure de données de l'ancienne version

