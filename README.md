# Application Android - Livre AGPIA

Application Android de lecture du livre de l'AGPIA (prières des heures), en français et en arabe,
avec une interface Material 3.

## 📱 Fonctionnalités

- ✅ **Deux langues de contenu** : français (texte revu sur le livre imprimé) et arabe (st-takla.org),
  bascule depuis la barre de lecture ou les paramètres ; l'interface suit la langue choisie (RTL en arabe)
- ✅ **Mode bilingue** : français et arabe côte à côte
- ✅ **Lecture en texte continu** des 10 heures, police serif, en-tête illustré par heure ;
  toutes les parties sont repliables (tap sur le titre), oraisons séparées, bouton **Tout agrandir / Tout réduire**
- ✅ **Texte copte** rendu avec la police Noto Sans Coptic
- ✅ **Titres alignés ou centrés** (option)
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
│   │   │   ├── data/                  # BookContent (modèle + BookRepository), ContentManifest, ContentUpdater
│   │   │   ├── ui/                    # Adapters, recherche, marque-pages, paramètres, AboutActivity
│   │   │   └── utils/                 # Préférences, insets edge-to-edge, images d'heure
│   │   ├── assets/book/
│   │   │   ├── manifest.json          # Versions, langues, sha256 des fichiers de contenu
│   │   │   ├── fr/                    # Contenu français : chapter1.json … chapter10.json
│   │   │   └── ar/                    # Contenu arabe : chapter1.json … chapter10.json
│   │   ├── assets/licenses/           # Licence OFL de Noto Sans Coptic
│   │   └── res/
│   │       ├── drawable-nodpi/        # hour_1.webp … hour_10.webp, cover.webp (extraits du livre)
│   │       ├── font/                  # noto_sans_coptic.ttf
│   │       └── values-ar/             # Interface en arabe
│   └── build.gradle
├── scripts/                # build_content_manifest.py (validation du contenu + manifeste)
└── gradle/                 # Wrapper Gradle
```

## 📊 Spécifications techniques

- **Gradle** : 9.4.1 — **AGP** : 9.2.1 (support Kotlin intégré, pas de plugin Kotlin séparé)
- **Min SDK** : 24 (Android 7.0) — **Compile / Target SDK** : 36 (Android 16)
- **Langage** : Kotlin 100 % — **UI** : Material 3 (`Theme.Material3.DayNight.NoActionBar`), ViewBinding
- **Stockage** : SharedPreferences (taille de texte, thème, langue, dernière heure, marque-pages, état « tout agrandir »)
- **Contenu** : un fichier par heure et par langue dans `assets/book/<lang>/chapterN.json`
  (`{"number","title","sections":[{"id","title","content"[,"kind"[,"groupEnd"]]}]}`, mêmes `id` dans
  toutes les langues). Toutes les langues sont lues depuis les assets, avec repli silencieux sur le
  français si un fichier manque. `assets/book/manifest.json` liste les langues, la version du contenu
  et le sha256 de chaque fichier.
- **Langues** : langue principale (interface + sens de lecture) et seconde langue facultative
  (seconde colonne), toutes deux choisies parmi les langues du manifeste.
- **Release** : `minifyEnabled` + `shrinkResources` (APK non signé : `assembleRelease` ; `res/raw/keep.xml`
  protège les images `hour_*` résolues dynamiquement)

## 🔄 Mise à jour du contenu

La mise à jour est entièrement silencieuse : au lancement, au plus une fois par semaine et sur un thread
de fond, l'application regarde si une version plus récente du contenu est publiée sur GitHub
(`raw.githubusercontent.com/asel-hue/LivreAGPIA/main/app/src/main/assets/book/`). Seuls les fichiers
dont l'empreinte a changé sont téléchargés ; tout est vérifié (sha256, schéma JSON, cohérence entre
langues) avant un basculement atomique. Rien n'est affiché et l'écran en cours n'est pas modifié : le
nouveau contenu est servi au prochain chargement d'une heure ou au prochain lancement. En cas d'erreur
(hors ligne, fichier invalide), le contenu précédent est conservé. *Paramètres → Contenu* indique la
version affichée ; *Restaurer le contenu embarqué* revient au contenu livré avec l'APK.

Pour publier une mise à jour :

1. Modifier les JSON dans `app/src/main/assets/book/<lang>/`.
2. Lancer `python scripts/build_content_manifest.py` (valide le schéma, recalcule les empreintes,
   incrémente `contentVersion`).
3. Commit + push sur `main` : les applications installées récupèrent la mise à jour dans la semaine,
   au lancement suivant.

Pour ajouter une langue : créer `assets/book/<code>/chapter1.json … chapter10.json` (mêmes `id` que
le FR), ajouter une entrée dans `languages` du manifeste (`code`, `name`, `nativeName`, `rtl`,
`uiLocale`), puis relancer le script. La langue apparaît dans les paramètres sans modification du code.
Pour une interface traduite, ajouter `res/values-<code>/strings.xml` et le code dans
`PreferencesManager.SUPPORTED_UI_LOCALES` ; sinon l'interface reste en français.

## 📖 Sources des textes

- **Texte français** : *AGPIA : les prières des heures*, **édition 2009**, Église copte orthodoxe
  Archange Michel et Saint Georges (Villejuif, France), sous la bénédiction de S.S. le Pape Chenouda III ;
  compilé et traduit par le sous-diacre Naguy Wasfy, revu et corrigé par le père Moussa Anba Bishoy et
  Mme Nadia Simaika-Kelada Antoun, correction et mise en page 2009 par Magdalena Prod'homme. L'ouvrage se
  présente comme une « traduction non officielle » imprimée pour les besoins de la prière en langue française.
  Le texte a été relu depuis l'édition PDF fournie par l'auteur du projet.
- **Formules et répons coptes** : texte liturgique copte traditionnel.
- **Texte arabe** : [st-takla.org](https://st-takla.org/Agpeya_.html) (« الأجبية »), pages des heures
  (`https://st-takla.org/Agpeya/…`) et prières diverses (`https://st-takla.org/Prayers-Slawat/…`).
- **Icônes des heures** : illustrations du livre français.
- **Police copte** : [Noto Sans Coptic](https://fonts.google.com/noto/specimen/Noto+Sans+Coptic),
  SIL Open Font License 1.1 (licence dans `app/src/main/assets/licenses/`).

Les textes liturgiques, traductions et illustrations restent attribués à leurs auteurs et éditeurs respectifs.

## 📜 Licence

- **Code source** : [PolyForm Noncommercial 1.0.0](LICENSE). Utilisation, modification et redistribution
  libres à des fins non commerciales ; tout usage commercial nécessite l'accord préalable de l'auteur
  (contact via [GitHub](https://github.com/asel-hue)).
- **Textes liturgiques, traductions et illustrations** : non couverts par cette licence. Ils proviennent
  des sources citées ci-dessus et restent attribués à leurs auteurs.
- **Police Noto Sans Coptic** : SIL Open Font License 1.1.

## ⚠️ Notes importantes

- Le texte arabe provient de st-takla.org ; les heures 8 (fermeture du voile) et 9 (minuit) y sont complètes
  et le français a été complété en conséquence.
- Les données (marque-pages, préférences) sont stockées localement sur l'appareil (sauvegardées via Auto Backup).

---

**Version** : 1.3.0 (versionCode 4)
