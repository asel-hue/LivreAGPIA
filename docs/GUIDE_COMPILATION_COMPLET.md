# Guide complet de compilation

## Méthode 1 : Compilation via Android Studio (Recommandé) ⭐

### Prérequis
- Android Studio Hedgehog (2023.1.1) ou plus récent
- JDK 17 ou plus récent (inclus avec Android Studio)
- Android SDK avec API 34

### Où trouver les options de compilation

#### Menu Build (Recommandé)
1. Dans la barre de menu en haut, cliquez sur **Build**
2. Options disponibles :
   - **Make Project** (Ctrl+F9) - Compile les fichiers modifiés
   - **Rebuild Project** - Nettoie et recompile tout
   - **Clean Project** - Nettoie les fichiers de build
   - **Build Bundle(s) / APK(s)** → **Build APK(s)** - Génère l'APK

#### Raccourcis clavier
- **Ctrl+F9** : Make Project (compilation rapide)
- **Ctrl+Shift+F9** : Rebuild Project (compilation complète)
- **Shift+F10** : Run (compile et lance l'application)

#### Barre d'outils
- Bouton **▶️ (Run)** - Compile et lance l'application
- Bouton **🔨 (Build)** - Menu déroulant avec les options de build

### Étapes pour compiler

1. **Ouvrir le projet dans Android Studio**
   - File → Open
   - Sélectionner le dossier `LivreAGPIA`
   - Attendre la synchronisation Gradle

2. **Vérifier la configuration**
   - File → Project Structure
   - Vérifier que le SDK est configuré correctement
   - Vérifier que les dépendances sont bien chargées

3. **Compiler l'application**
   - **Build → Build Bundle(s) / APK(s) → Build APK(s)**
   - Ou **Build → Make Project** (Ctrl+F9)
   - Attendre la fin de la compilation
   - Un message apparaîtra : **"APK(s) generated successfully"**

4. **Localiser l'APK**
   - Cliquer sur **"locate"** dans le message de succès
   - Ou aller dans : `app/build/outputs/apk/debug/app-debug.apk`

### Si vous ne voyez pas le menu Build

1. Vérifiez que vous êtes dans la vue **"Project"** (pas "Android")
2. Assurez-vous qu'un projet est ouvert
3. Essayez **View → Tool Windows → Build** pour ouvrir le panneau Build

## Méthode 2 : Compilation en ligne de commande

### Prérequis
- JDK 17 installé et configuré dans JAVA_HOME
- Android SDK installé et configuré dans ANDROID_HOME
- Gradle wrapper présent dans le projet

### Configuration de l'environnement

#### Windows (PowerShell)
```powershell
# Définir JAVA_HOME (remplacer par votre chemin)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Définir ANDROID_HOME (remplacer par votre chemin)
$env:ANDROID_HOME = "C:\Users\VotreNom\AppData\Local\Android\Sdk"
$env:PATH = "$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\tools;$env:PATH"
```

#### Linux/Mac
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
```

### Compilation

```bash
# Compiler l'APK de debug
./gradlew assembleDebug

# Compiler l'APK de release (nécessite une signature)
./gradlew assembleRelease

# Nettoyer le projet
./gradlew clean

# Vérifier les erreurs
./gradlew lint
```

### Emplacement de l'APK généré
```
app/build/outputs/apk/debug/app-debug.apk
```

## Résolution des problèmes courants

### Erreur : JAVA_HOME not set
**Solution :** Configurer JAVA_HOME avec le chemin vers votre installation JDK 17

### Erreur : SDK not found
**Solution :** 
1. Ouvrir Android Studio
2. File → Settings → Appearance & Behavior → System Settings → Android SDK
3. Installer le SDK Platform 34
4. Noter le chemin et le définir dans ANDROID_HOME

### Erreur : Gradle sync failed
**Solution :**
1. File → Invalidate Caches / Restart
2. File → Sync Project with Gradle Files

### Erreur : Build failed - Missing dependencies
**Solution :**
1. Vérifier la connexion Internet
2. File → Sync Project with Gradle Files
3. Build → Clean Project
4. Build → Rebuild Project

### Erreur JDK (jlink.exe, core-for-system-modules.jar)
**Solution :** Voir `SOLUTION_JDK_COMPLETE.md`

## Ordre recommandé pour résoudre les problèmes

1. **Build → Clean Project** (nettoie les fichiers de build)
2. **File → Invalidate Caches / Restart** (invalide les caches)
3. Attendre le redémarrage
4. **File → Sync Project with Gradle Files** (synchronise Gradle)
5. **Build → Rebuild Project** (recompile tout)

## Vérification avant compilation

### Checklist
- [ ] Tous les fichiers XML sont valides
- [ ] Tous les imports Kotlin sont corrects
- [ ] AndroidManifest.xml contient toutes les activités
- [ ] Les ressources (strings, colors, drawables) sont définies
- [ ] Les layouts sont valides

## Installation de l'APK

### Sur un appareil Android
1. Activer les "Sources inconnues" dans les paramètres
2. Transférer l'APK sur l'appareil
3. Ouvrir l'APK et installer

### Via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Notes importantes

- La première compilation peut prendre plusieurs minutes (téléchargement des dépendances)
- Assurez-vous d'avoir au moins 4 GB d'espace libre
- La compilation de release nécessite une clé de signature (voir documentation Android)
- **Pour une compilation rapide, utilisez Android Studio qui gère automatiquement toutes les dépendances !**


