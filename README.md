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

**État important :** AnimeSama et SenpaiStream sont des squelettes techniques. Leur recherche, leurs fiches et la lecture ne sont pas implémentées ; le nom et l'URL configurée ne suffisent pas à rendre un site lisible dans CloudStream. Toute intégration doit s'appuyer sur des contenus et flux autorisés.

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
