# Application Android - Livre AGPIA

Application Android de lecture du livre de l'AGPIA (prières des heures), en français et en arabe,
avec une mise en page inspirée de Coptic Reader et une interface Material 3. L'application Agpia
d'origine (`com.stmstg.agpia`) est archivée dans `archive/` pour référence.

## 📱 Fonctionnalités

- ✅ **Deux langues de contenu** : français (texte revu sur le livre imprimé) et arabe (st-takla.org),
  bascule depuis la barre de lecture ou les paramètres ; l'interface suit la langue choisie (RTL en arabe)
- ✅ **Lecture en texte continu** des 10 heures, police serif, en-tête illustré par heure,
  psaumes repliables (Afficher / Masquer) et bouton **Tout agrandir / Tout réduire**
- ✅ **Tiroir latéral** listant les heures + boutons Précédent / Accueil / Suivant
- ✅ **Reprise de la dernière lecture** depuis l'accueil (liste illustrée des heures)
- ✅ **Recherche** dans tout le texte de la langue courante (insensible à la casse et aux accents)
- ✅ **Marque-pages** (communs aux deux langues) et **partage** par partie (menu ⋮ de chaque partie)
- ✅ **Taille du texte** réglable (12–28 sp, curseur ou menu « Agrandir / Réduire le texte »)
- ✅ **Apparence** : Système / Clair / Sombre
- ✅ Edge-to-edge, retour prédictif, état conservé à la rotation, écran maintenu allumé pendant la lecture

## 🚀 Démarrage rapide

### Prérequis
- Android Studio (version récente, fournissant le JBR 21)
- JDK 17 ou plus (le projet utilise le JBR d'Android Studio via `gradle.properties`)
- Android SDK avec API 36 (compileSdk)

### Compilation

En ligne de commande :

```powershell
.\gradlew.bat assembleDebug        # APK : app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat installDebug         # installe sur l'appareil connecté (adb)
.\gradlew.bat lintDebug            # analyse statique
```

Ou dans Android Studio : **File → Sync Project with Gradle Files** puis **Run**.

### Configuration JDK

`gradle.properties` force `org.gradle.java.home` vers `C:\Program Files\Android\Android Studio\jbr`.
Adapter ce chemin si Android Studio est installé ailleurs.

## 📁 Structure du projet

```
LivreAGPIA/
├── app/
│   ├── src/main/
│   │   ├── java/com/agpia/livre/
│   │   │   ├── MainActivity.kt        # Accueil : liste des heures + reprise
│   │   │   ├── ReadingActivity.kt     # Lecture (tiroir, psaumes, actions)
│   │   │   ├── LivreAgpiaApp.kt       # Application : thème, locale, init du contenu
│   │   │   ├── data/                  # Modèle + contenu FR (chapters/ChapterN.kt), chargement AR
│   │   │   ├── ui/                    # Adapters, recherche, marque-pages, paramètres
│   │   │   └── utils/                 # Préférences, insets edge-to-edge, images d'heure
│   │   ├── assets/book/ar/            # Contenu arabe : chapter1.json … chapter10.json
│   │   └── res/
│   │       ├── drawable-nodpi/        # hour_1.webp … hour_10.webp, cover.webp (extraits du livre)
│   │       └── values-ar/             # Interface en arabe
│   └── build.gradle
├── docs/                   # Documentation historique
├── scripts/                # Scripts utilitaires (extraction du contenu, nettoyage Gradle)
├── archive/                # Ancienne application Agpia (référence, non versionnée)
└── gradle/                 # Wrapper Gradle
```

## 📊 Spécifications techniques

- **Gradle** : 9.4.1 — **AGP** : 9.2.1 (support Kotlin intégré, pas de plugin Kotlin séparé)
- **Min SDK** : 24 (Android 7.0) — **Compile / Target SDK** : 36 (Android 16)
- **Langage** : Kotlin 100 % — **UI** : Material 3 (`Theme.Material3.DayNight.NoActionBar`), ViewBinding
- **Stockage** : SharedPreferences (taille de texte, thème, langue, dernière heure, marque-pages, état « tout agrandir »)
- **Contenu** : FR compilé dans `data/chapters/` ; AR lu depuis `assets/book/ar/chapterN.json`
  (`{"number","title","sections":[{"id","title","content"}]}`, mêmes `id` que le FR ; repli silencieux
  sur le français si un fichier manque). Les titres commençant par « Psaume » / « مزمور » sont repliables.
- **Release** : `minifyEnabled` + `shrinkResources` (APK non signé : `assembleRelease` ; `res/raw/keep.xml`
  protège les images `hour_*` résolues dynamiquement)

## 📚 Documentation

Le dossier `docs/` contient la documentation historique du projet (migration du contenu, résolution
de problèmes Gradle/OneDrive). Certaines informations de version y sont antérieures à la mise à jour
AGP 9 / SDK 36 décrite ci-dessus.

## ⚠️ Notes importantes

- Le texte français a été relu et corrigé d'après le PDF du livre (`archive/Agpia/Agpia livre/Livre_Agpia.pdf`) ;
  les passages en copte, illisibles dans le PDF, sont donnés en translittération.
- Le texte arabe provient de st-takla.org ; les heures 8 (fermeture du voile) et 9 (minuit) y sont complètes
  et le français a été complété en conséquence.
- Les données (marque-pages, préférences) sont stockées localement sur l'appareil (sauvegardées via Auto Backup).
- Si OneDrive bloque un build (« not a regular file » dans `app/build`), supprimer `app/build` et relancer.
- Le dossier `archive/` est exclu de git (`.gitignore`).

---

**Version** : 1.2.0 (versionCode 3)
