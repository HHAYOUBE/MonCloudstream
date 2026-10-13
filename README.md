# MonCloudstream — dépôt personnel d'extensions CloudStream

Ce dépôt contient des extensions CloudStream et le module personnel `MonExtension`.

Pour la documentation de l'application, consulte le [dépôt officiel de CloudStream](https://github.com/recloudstream/cloudstream).

## Modules

- AfterDark
- Frembed
- Flemmix / Wiflix
- Xalaflix
- **MonExtension** : squelette personnel à adapter à une source que tu es autorisé à intégrer.

## Installer le dépôt

Après publication réussie du workflow, ajoute cette adresse dans CloudStream :

```text
https://raw.githubusercontent.com/HHAYOUBE/MonCloudstream/builds/repo.json
```

## Ajouter une source à MonExtension

1. Ouvre `MonExtension/src/main/kotlin/com/hhayoube/monextension/MonExtensionConfig.kt`.
2. Modifie `baseUrl` avec le domaine HTTPS de la source autorisée.
3. Adapte `MonExtensionProvider.kt` à la structure du site : modifier uniquement l'adresse ne suffit pas si sa recherche et ses pages ont une structure différente.
4. Enregistre les changements et pousse-les sur la branche principale après avoir testé la branche de fonctionnalité.

Le module est actuellement un **squelette de départ** : la recherche et les fiches ne sont pas encore implémentées. Il faut adapter le parseur au site choisi avant de pouvoir l'utiliser.

## Publication automatique

Le workflow `.github/workflows/build.yml` compile les cinq modules et publie les fichiers `.cs3`, `plugins.json` et `repo.json` sur la branche `builds`.

Les changements de cette branche de travail ne modifient pas `main` tant qu'ils ne sont pas fusionnés.
