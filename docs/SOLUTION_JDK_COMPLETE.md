# Solution complète pour les problèmes JDK

## Problèmes courants

### 1. Erreur `Failed to transform core-for-system-modules.jar`
### 2. Erreur `jlink.exe` 
### 3. Erreur `Could not resolve all files for configuration ':app:androidJdkImage'`

Toutes ces erreurs indiquent un problème avec le JDK utilisé par Gradle.

## Solutions (essayer dans l'ordre)

### Solution 1 : Redémarrer l'ordinateur ⭐ (SIMPLE ET EFFICACE)

Parfois, un simple redémarrage libère tous les verrous :
1. **Fermer Android Studio**
2. **Redémarrer l'ordinateur**
3. Rouvrir Android Studio
4. **File → Sync Project with Gradle Files**
5. **Build → Rebuild Project**

### Solution 2 : Vérifier le JDK dans Android Studio

1. **File → Project Structure → SDK Location**
2. Vérifier que **JDK location** pointe vers :
   ```
   C:\Program Files\Android\Android Studio\jbr
   ```
3. Si le chemin est différent ou contient des erreurs, le corriger
4. **Apply** puis **OK**
5. **File → Sync Project with Gradle Files**

### Solution 3 : Nettoyer le cache Gradle

#### Option A : Via Android Studio (RECOMMANDÉ)
1. **File → Invalidate Caches / Restart**
2. Sélectionner **"Invalidate and Restart"**
3. Après le redémarrage :
   - **Build → Clean Project**
   - **File → Sync Project with Gradle Files**
   - **Build → Rebuild Project**

#### Option B : Manuellement
1. Fermer Android Studio
2. Utiliser le script : `scripts/clean_all_gradle.ps1`
3. Rouvrir Android Studio
4. **File → Sync Project with Gradle Files**

### Solution 4 : Forcer l'utilisation du JDK d'Android Studio

Le fichier `gradle.properties` contient déjà :
```properties
org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
```

Si le problème persiste :
1. Vérifier que cette ligne est présente dans `gradle.properties`
2. **File → Sync Project with Gradle Files**
3. **Build → Rebuild Project**

### Solution 5 : Vérifier le SDK Android

1. **File → Settings → Appearance & Behavior → System Settings → Android SDK**
2. Vérifier que **Android SDK Platform 34** est installé
3. Si manquant, l'installer
4. **Apply** puis **OK**
5. Réessayer la compilation

### Solution 6 : Utiliser un JDK externe (si rien ne fonctionne)

1. Télécharger JDK 17 depuis [Adoptium](https://adoptium.net/)
2. L'installer dans un dossier **sans espaces** : `C:\Java\jdk-17`
3. Dans Android Studio : **File → Project Structure → SDK Location**
4. Changer **JDK location** vers : `C:\Java\jdk-17`
5. Mettre à jour `gradle.properties` :
   ```properties
   org.gradle.java.home=C:\\Java\\jdk-17
   ```
6. **File → Sync Project with Gradle Files**
7. **Build → Rebuild Project**

## Modifications déjà appliquées

✅ Désactivation de la compilation Java (projet 100% Kotlin)
```gradle
sourceSets {
    main.java.srcDirs = []
}
```

✅ Configuration Gradle optimisée
```properties
org.gradle.caching=false
org.gradle.parallel=false
```

✅ Cache Gradle nettoyé

## Vérification

Pour vérifier que tout fonctionne :
1. **Build → Rebuild Project**
2. L'APK devrait être généré dans : `app/build/outputs/apk/debug/app-debug.apk`

## Note importante

Le projet étant 100% Kotlin, la compilation Java n'est normalement pas nécessaire. Si l'erreur persiste, c'est probablement un problème avec :
- Le JDK d'Android Studio
- Le cache Gradle qui se régénère mal
- Un conflit avec OneDrive (voir `SOLUTION_ONEDRIVE.md`)

**Essayez d'abord un redémarrage de l'ordinateur** - c'est souvent la solution la plus simple et efficace !

## Fichiers de référence

- `gradle.properties` - Configuration JDK
- `app/build.gradle` - Configuration du projet
- `scripts/clean_all_gradle.ps1` - Script de nettoyage


