# Résolution : Fichier verrouillé par un processus

## Problème
Le fichier `transforms-3.lock` est verrouillé car un processus Gradle ou Java est encore en cours d'exécution.

## Solution étape par étape

### Étape 1 : Arrêter tous les processus Gradle/Java

**Option A : Script PowerShell (RECOMMANDÉ)**
1. Exécuter : `.\kill_gradle_processes.ps1`
2. Suivre les instructions à l'écran

**Option B : Gestionnaire des tâches (Manuel)**
1. Appuyer sur **Ctrl+Shift+Esc** pour ouvrir le Gestionnaire des tâches
2. Aller dans l'onglet **"Détails"**
3. Chercher les processus :
   - `java.exe` (qui pourrait être Gradle)
   - `gradle.exe` ou `gradle-daemon`
4. Sélectionner chaque processus et cliquer sur **"Terminer la tâche"**
5. Répéter pour tous les processus Java/Gradle trouvés

**Option C : Ligne de commande**
```powershell
# Arrêter tous les processus Java
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force

# Arrêter tous les processus Gradle
Get-Process gradle* -ErrorAction SilentlyContinue | Stop-Process -Force
```

### Étape 2 : Vérifier qu'Android Studio est fermé

1. Vérifier dans le Gestionnaire des tâches qu'**Android Studio** n'est pas en cours
2. Si présent, le fermer complètement

### Étape 3 : Attendre quelques secondes

Attendre 3-5 secondes pour que les fichiers soient libérés.

### Étape 4 : Nettoyer le cache

**Option A : Script**
```powershell
.\clean_gradle_cache.ps1
```

**Option B : Manuel**
1. Aller dans : `C:\Users\AntoineSELEMAN\.gradle\caches`
2. Supprimer le dossier `transforms-3`
3. Si le fichier `.lock` existe toujours, essayer de le supprimer directement

**Option C : Supprimer uniquement le fichier lock**
```powershell
Remove-Item "C:\Users\AntoineSELEMAN\.gradle\caches\transforms-3\transforms-3.lock" -Force
```

### Étape 5 : Redémarrer Android Studio

1. Rouvrir Android Studio
2. **File → Sync Project with Gradle Files**
3. **Build → Rebuild Project**

## Si le problème persiste

### Solution alternative : Renommer le dossier

Au lieu de supprimer, renommez le dossier :
```powershell
Rename-Item "C:\Users\AntoineSELEMAN\.gradle\caches\transforms-3" "transforms-3.old"
```

Gradle créera un nouveau dossier automatiquement.

### Solution radicale : Redémarrer l'ordinateur

Si rien ne fonctionne :
1. Redémarrer l'ordinateur
2. Cela libérera tous les fichiers verrouillés
3. Ensuite, supprimer le dossier `transforms-3`

## Vérification

Pour vérifier qu'aucun processus n'utilise les fichiers :
```powershell
Get-Process | Where-Object {$_.Path -like "*gradle*" -or $_.Name -eq "java"}
```

Si aucun processus n'apparaît, vous pouvez supprimer le cache en toute sécurité.


