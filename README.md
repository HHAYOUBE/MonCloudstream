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

**État important :** AnimeSama et SenpaiStream utilisent AniList pour la recherche, les fiches et les pages de tendances. Les fiches chargent les liens d’épisodes YouTube publiés dans le champ `streamingEpisodes` d’AniList lorsqu’ils sont disponibles ; CloudStream essaie ensuite son extracteur YouTube intégré. La disponibilité varie selon l’anime et la région. AniList ne fournit pas un catalogue complet des épisodes, et cette intégration ne contourne ni DRM, ni connexion, ni abonnement.

## État technique des extensions anime

- Recherche, fiches et pages de tendances : données de catalogue publiques d’AniList.
- Les identifiants internes sont des identifiants AniList, pas des URL d’épisodes.
- Aucun épisode fictif n’est généré pour simuler une lecture fonctionnelle.
- Les liens YouTube référencés par AniList sont envoyés à l’extracteur CloudStream intégré ; la lecture dépend de la disponibilité publique de la vidéo et de la prise en charge par l’extracteur.
- Les autres liens directs acceptés par `loadLinks` sont limités aux médias HTTP(S) reconnaissables (`.m3u8`, `.mpd`, `.mp4`, `.m4v`, `.webm`, `.mkv`).
- Les futurs fournisseurs vidéo doivent être isolés dans leurs propres adaptateurs, utiliser des flux autorisés et être testés via la CI avant publication.

## Installer le dépôt

Après une publication réussie du workflow, ajoute cette adresse dans CloudStream :

```text
https://raw.githubusercontent.com/HHAYOUBE/MonCloudstream/builds/repo.json
```

## Maintenance simple

- **Catalogue central de liens :** `Sources/sources.json`
- **Instructions pour modifier les liens :** [Sources/README.md](Sources/README.md)
- **Vérificateur automatique :** `scripts/validate_sources.py`
- **Validation et compilation :** `.github/workflows/validate-build.yml`
- **Publication des extensions compilées :** `.github/workflows/build.yml`

Quand tu demandes d'ajouter ou de mettre à jour un lien, il faut distinguer un lien du catalogue d'une adresse de fournisseur : les URL de base des extensions Kotlin sont dans le fichier `*Config.kt` du module concerné. Les deux changements doivent être testés ensemble lorsqu'une source change.

La CI vérifie le format du catalogue puis tente de compiler les sept modules. La publication vers la branche `builds` est séparée et doit réussir avant que CloudStream puisse installer les fichiers `.cs3`. Un résultat vert de validation ne garantit pas, à lui seul, que les sites sources sont disponibles.
