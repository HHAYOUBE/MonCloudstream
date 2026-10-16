# MonCloudstream — dépôt personnel d'extensions CloudStream

Ce dépôt contient les extensions existantes et des modules personnels.

## Modules

- AfterDark
- Frembed
- Flemmix / Wiflix
- Xalaflix
- MonExtension
- AnimeSama
- SenpaiStream

**État important :** AnimeSama et SenpaiStream utilisent actuellement AniList pour les catalogues d’animation, la recherche, les fiches et les pages de tendances. SenpaiStream classe aussi les films d’animation au format Movie. Cela ne constitue pas encore un catalogue général de films/séries non animés, ni une intégration complète des saisons du site Senpai-Stream. Les fiches peuvent afficher des liens de plateformes officielles référencées par AniList lorsqu’ils existent et sont pris en charge. Aucun lecteur YouTube n’est intégré.

## État technique des extensions anime

- Recherche, fiches et pages de tendances : données de catalogue publiques d’AniList.
- SenpaiStream distingue les films d’animation des autres formats d’animés ; les saisons de séries générales et les catalogues non animés restent à intégrer via une API autorisée adaptée.
- Les identifiants internes sont des identifiants AniList, pas des URL d’épisodes.
- Aucun épisode fictif n’est généré pour simuler une lecture fonctionnelle.
- Les liens de plateformes officielles référencés par AniList sont envoyés à l’extracteur CloudStream intégré ; la lecture dépend de la disponibilité publique et de la prise en charge par l’extracteur.
- Les autres liens directs acceptés par `loadLinks` sont limités aux médias HTTP(S) reconnaissables (`.m3u8`, `.mpd`, `.mp4`, `.m4v`, `.webm`, `.mkv`).
- Les futurs fournisseurs vidéo doivent être isolés dans leurs propres adaptateurs, utiliser des flux autorisés et être testés via la CI avant publication.

## Installer le dépôt dans CloudStream / Goodstream

**Adresse à ajouter comme dépôt (manifest) :**

```text
https://raw.githubusercontent.com/HHAYOUBE/MonCloudstream/builds/repo.json
```

**Important :** colle cette adresse dans la fonction « Ajouter un dépôt ». Ne colle pas l’URL de `plugins.json` à cet endroit : ce fichier est le catalogue interne utilisé par `repo.json`, ce n’est pas le manifeste à ajouter. Les fichiers `.cs3` sont les extensions compilées, pas l’adresse du dépôt.

Après avoir ajouté le dépôt, actualise sa liste et installe AnimeSama ou SenpaiStream depuis le dépôt. Si le dépôt n’apparaît toujours pas, vérifie que l’application accepte les dépôts CloudStream compatibles.

## Maintenance simple

- **Catalogue central de liens :** `Sources/sources.json`
- **Instructions pour modifier les liens :** [Sources/README.md](Sources/README.md)
- **Vérificateurs automatiques :** `scripts/validate_sources.py` et `scripts/validate_extensions.py`
- **Validation et compilation :** `.github/workflows/validate-build.yml`
- **Publication des extensions compilées :** `.github/workflows/build.yml`

Quand tu demandes d'ajouter ou de mettre à jour un lien, il faut distinguer un lien du catalogue d'une adresse de fournisseur : les URL de base des extensions Kotlin sont dans le fichier `*Config.kt` du module concerné. Les deux changements doivent être testés ensemble lorsqu'une source change.

La CI vérifie le format du catalogue puis tente de compiler les sept modules. La publication vers la branche `builds` est séparée et doit réussir avant que CloudStream puisse installer les fichiers `.cs3`. Un résultat vert de validation ne garantit pas, à lui seul, que les sites sources sont disponibles.
