# MonExtension

Module personnel CloudStream de HHAYOUBE, séparé des extensions existantes.

## Ajouter ou changer une source

1. Ouvre `src/main/kotlin/com/hhayoube/monextension/MonExtensionConfig.kt`.
2. Remplace `https://example.org` par le domaine HTTPS de la source que tu es autorisé à intégrer.
3. Adapte `MonExtensionProvider.kt` à la structure réelle du site : la méthode `search()` doit renvoyer les résultats et `load()` doit construire la fiche.
4. Enregistre et pousse les changements sur GitHub. GitHub Actions compilera les modules configurés.

**Important :** changer seulement `baseUrl` ne suffit pas si le nouveau site utilise une structure HTML/API différente. Cette extension est un squelette de départ, pas encore un fournisseur fonctionnel. Elle ne contourne pas les protections d'accès.
