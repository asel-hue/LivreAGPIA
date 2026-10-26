# Scripts utilitaires

## Scripts Python

### extract_agpia_content_v2.py ⭐ (RECOMMANDÉ)
**Usage** : Extraction du contenu depuis les fichiers HTML de l'ancienne version
```bash
python scripts/extract_agpia_content_v2.py
```
- Extrait le contenu complet de tous les fichiers `hour*.html`
- Génère `app/src/main/java/com/agpia/livre/data/BookContent.kt`
- Version améliorée avec meilleure extraction

### extract_agpia_content.py
Version initiale du script d'extraction (moins complète)

### convert_book_content.py
Script générique pour convertir différents formats vers Kotlin

## Scripts PowerShell

### clean_all_gradle.ps1 ⭐ (RECOMMANDÉ)
**Usage** : Nettoyage complet du cache Gradle
```powershell
.\scripts\clean_all_gradle.ps1
```
- Arrête les processus Java/Gradle
- Supprime le cache `transforms-3` et `jdks`
- Script interactif avec confirmations

### clean_cache_simple.ps1
Version simple et rapide du nettoyage de cache

### stop_all_java.ps1
Arrête tous les processus Java (sauf Cursor) pour libérer les fichiers

### kill_gradle_processes.ps1
Arrête tous les processus Gradle et Java

### clean_build.ps1
Nettoie le dossier `app/build`

### clean_gradle_cache.ps1
Nettoie le cache Gradle (version alternative)

### fix_gradle_cache.ps1
Corrige le cache Gradle corrompu

## Recommandations

- **Pour nettoyer le cache** : Utilisez `clean_all_gradle.ps1`
- **Pour extraire le contenu** : Utilisez `extract_agpia_content_v2.py`
- **Pour arrêter les processus** : Utilisez `stop_all_java.ps1`

## Notes

- Tous les scripts PowerShell nécessitent que Android Studio soit fermé
- Les scripts Python nécessitent Python 3.x
- Les scripts sont conçus pour Windows PowerShell


