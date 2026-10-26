# Instructions pour nettoyer le cache Gradle

## Problème
Le cache Gradle peut être corrompu et causer des erreurs de compilation comme :
- `Failed to transform core-for-system-modules.jar`
- `Cannot snapshot ... not a regular file`
- Erreurs liées à `transforms-3`

## Solution : Nettoyer le cache Gradle

### Méthode 1 : Script PowerShell (RECOMMANDÉ)

1. **Fermer complètement Android Studio**
2. Ouvrir PowerShell dans le dossier du projet
3. Exécuter : `.\clean_gradle_cache.ps1`
4. Suivre les instructions à l'écran
5. Rouvrir Android Studio
6. **File → Sync Project with Gradle Files**
7. **Build → Rebuild Project**

### Méthode 2 : Nettoyage manuel

1. **Fermer complètement Android Studio**
2. Ouvrir l'explorateur Windows
3. Aller dans : `C:\Users\AntoineSELEMAN\.gradle\caches`
4. Supprimer le dossier `transforms-3`
5. (Optionnel) Supprimer tout le dossier `caches` pour un nettoyage complet
6. Rouvrir Android Studio
7. **File → Sync Project with Gradle Files**
8. **Build → Rebuild Project**

### Méthode 3 : Dans Android Studio

1. **File → Invalidate Caches / Restart**
2. Sélectionner **"Invalidate and Restart"**
3. Attendre le redémarrage
4. **File → Sync Project with Gradle Files**
5. **Build → Rebuild Project**

## Chemin du cache Gradle

Le cache Gradle se trouve généralement dans :
```
C:\Users\VotreNom\.gradle\caches
```

Le dossier problématique est souvent :
```
C:\Users\VotreNom\.gradle\caches\transforms-3
```

## Important

- ⚠️ **Fermez Android Studio** avant de supprimer le cache
- ⚠️ Le nettoyage du cache peut prendre quelques minutes lors de la prochaine compilation (Gradle doit tout retélécharger)
- ✅ C'est normal et sans danger - Gradle régénérera le cache automatiquement

## Après le nettoyage

La première compilation après le nettoyage sera plus longue car Gradle devra :
- Retélécharger les dépendances
- Régénérer les fichiers transformés
- Reconstruire le cache

C'est normal et ne se produira qu'une fois.


