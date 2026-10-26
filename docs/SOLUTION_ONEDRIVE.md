# Solution au problème OneDrive avec Gradle

## Problème
OneDrive interfère avec le processus de build Gradle en créant des fichiers spéciaux ou en verrouillant des fichiers dans le dossier `build`.

## Solutions

### Solution 1 : Nettoyer le projet (RECOMMANDÉ - À ESSAYER EN PREMIER)

Dans Android Studio :
1. **Build → Clean Project**
2. Attendre la fin
3. **File → Invalidate Caches / Restart**
4. Sélectionner **"Invalidate and Restart"**
5. Après le redémarrage : **Build → Rebuild Project**

### Solution 2 : Supprimer manuellement le dossier build

1. **Fermer complètement Android Studio**
2. Ouvrir PowerShell dans le dossier du projet
3. Exécuter : `.\clean_build.ps1`
4. Ou manuellement supprimer le dossier `app\build`
5. Rouvrir Android Studio et recompiler

### Solution 3 : Pause OneDrive temporairement

1. Clic droit sur l'icône OneDrive dans la barre des tâches
2. **Paramètres → Pause la synchronisation → 2 heures**
3. Nettoyer le projet (Solution 1)
4. Recompiler

### Solution 4 : Exclure le dossier build de OneDrive (DÉFINITIF)

1. Clic droit sur le dossier `LivreAGPIA` dans l'explorateur
2. **OneDrive → Paramètres → Synchroniser**
3. **"Choisir les dossiers"**
4. Décocher ou exclure le dossier `build`

**OU** créer/modifier le fichier `.onedriveignore` (déjà créé) :
```
build/
.gradle/
*.iml
.idea/
```

### Solution 5 : Déplacer le projet hors de OneDrive (MEILLEURE SOLUTION)

**C'est la solution la plus fiable pour éviter tous les problèmes :**

1. Fermer Android Studio
2. Copier le dossier `LivreAGPIA` vers un dossier local :
   - `C:\AndroidProjects\LivreAGPIA`
   - `C:\Users\AntoineSELEMAN\Documents\AndroidProjects\LivreAGPIA`
   - Ou tout autre dossier local (pas dans OneDrive)
3. Ouvrir le nouveau projet dans Android Studio
4. OneDrive ne synchronisera plus les fichiers de build

## Pourquoi ce problème ?

OneDrive synchronise tous les fichiers, y compris les fichiers temporaires de build. Gradle a besoin d'un accès exclusif à ces fichiers, ce qui entre en conflit avec OneDrive.

## Recommandation finale

**Déplacer le projet hors de OneDrive** est la meilleure solution car :
- ✅ Évite tous les problèmes de verrouillage
- ✅ Améliore les performances (pas de synchronisation inutile)
- ✅ Réduit l'utilisation de l'espace OneDrive
- ✅ Évite les conflits de fichiers

Les fichiers de code source peuvent être sauvegardés séparément si nécessaire, mais les dossiers `build/` et `.gradle/` ne doivent jamais être synchronisés.


