# MonExtension

Module personnel CloudStream de HHAYOUBE, séparé des extensions existantes.

## Catalogue de liens

Le dossier [`Sources/`](../Sources/) contient `sources.json`, un catalogue modifiable de liens vers des sites officiels et des contenus proposés légalement. Pour le moment, il s'agit d'un catalogue de configuration : il n'est pas encore affiché dans l'interface CloudStream et ne lance pas de lecture depuis ces sites.

## Ajouter ou changer une source

1. Modifie `Sources/sources.json` pour ajouter ou mettre à jour le nom, l'URL HTTPS et la description.
2. Privilégie les sites officiels, les contenus du domaine public et les sources que tu es autorisé à intégrer.
3. Pour transformer une source en véritable fournisseur CloudStream, il faut développer puis tester séparément son intégration dans `MonExtensionProvider.kt`.

**Important :** changer une URL dans le catalogue ne suffit pas à créer un fournisseur de recherche/lecture. Cette extension reste un squelette tant que le fournisseur n'a pas été implémenté et compilé avec succès.
