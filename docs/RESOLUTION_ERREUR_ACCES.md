# Résolution de l'erreur AccessDeniedException

## Problème
L'erreur `java.nio.file.AccessDeniedException` se produit généralement parce que :
- OneDrive verrouille des fichiers dans le dossier `build`
- Un processus (Android Studio, antivirus) utilise des fichiers
- Les permissions du dossier sont incorrectes

## Solutions (essayer dans l'ordre)

### Solution 1 : Nettoyer le projet dans Android Studio
1. Dans Android Studio : **Build → Clean Project**
2. Attendre la fin du nettoyage
3. Puis : **Build → Rebuild Project**

### Solution 2 : Fermer Android Studio et supprimer manuellement le dossier build
1. **Fermer complètement Android Studio**
2. Ouvrir l'explorateur Windows
3. Naviguer vers : `C:\Users\AntoineSELEMAN\OneDrive - Sewan\Bureau\LivreAGPIA\app\build`
4. Supprimer le dossier `build` (ou au moins le sous-dossier `intermediates`)
5. Rouvrir Android Studio et recompiler

### Solution 3 : Exclure le dossier build de OneDrive (RECOMMANDÉ)
OneDrive peut verrouiller les fichiers pendant la synchronisation. Exclure le dossier build :

1. Clic droit sur le dossier `LivreAGPIA` dans OneDrive
2. **OneDrive → Paramètres → Synchroniser**
3. Cliquer sur **"Choisir les dossiers"**
4. Décocher le dossier `build` ou exclure les dossiers de build

**OU** créer un fichier `.onedriveignore` dans le dossier du projet :

```
build/
.gradle/
*.iml
.idea/
```

### Solution 4 : Déplacer le projet hors de OneDrive
Le plus simple : déplacer le projet dans un dossier local (ex: `C:\AndroidProjects\LivreAGPIA`)

1. Fermer Android Studio
2. Copier le dossier `LivreAGPIA` vers `C:\AndroidProjects\`
3. Ouvrir le nouveau projet dans Android Studio

### Solution 5 : Vérifier les processus en cours
1. Ouvrir le Gestionnaire des tâches (Ctrl+Shift+Esc)
2. Vérifier si `java.exe` ou `gradle.exe` sont en cours d'exécution
3. Les terminer si nécessaire
4. Réessayer la compilation

### Solution 6 : Exécuter en tant qu'administrateur
1. Fermer Android Studio
2. Clic droit sur Android Studio → **Exécuter en tant qu'administrateur**
3. Réessayer la compilation

## Solution rapide (à essayer en premier)

Dans Android Studio :
1. **File → Invalidate Caches / Restart**
2. Sélectionner **"Invalidate and Restart"**
3. Attendre le redémarrage
4. **Build → Clean Project**
5. **Build → Rebuild Project**

## Prévention future

Ajouter un fichier `.gitignore` ou `.onedriveignore` pour exclure :
- `build/`
- `.gradle/`
- `*.iml`
- `.idea/`

Ces dossiers sont générés automatiquement et ne doivent pas être synchronisés.


