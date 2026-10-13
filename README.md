# MonCloudstream — dépôt personnel d'extensions CloudStream

Ce dépôt contient les extensions existantes et trois modules personnels/en préparation.

## Modules
- AfterDark
- Frembed
- Flemmix / Wiflix
- Xalaflix
- MonExtension
- AnimeSama
- SenpaiStream

**Important :** AnimeSama et SenpaiStream sont pour le moment des squelettes installables uniquement si la compilation réussit. Leur recherche, leurs fiches et la lecture ne sont pas implémentées ; le nom et l'URL configurée ne suffisent pas à rendre un site lisible dans CloudStream. Les intégrations de lecture doivent se limiter aux contenus et flux autorisés.

## Installer le dépôt

Après une publication réussie du workflow, ajoute cette adresse dans CloudStream :

```text
https://raw.githubusercontent.com/HHAYOUBE/MonCloudstream/builds/repo.json
```

## Développement

La compilation de validation est définie dans `.github/workflows/validate-build.yml`. La publication vers la branche `builds` se fait depuis `main` ou `master` après fusion des changements. Vérifie que GitHub Actions termine avec succès avant d'essayer d'installer le dépôt sur la télé.
