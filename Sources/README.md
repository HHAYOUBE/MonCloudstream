# Catalogue de sources — MonCloudstream

Le fichier `sources.json` est le catalogue central des liens éditables. Il sert de registre lisible et validé par CI ; **ce n'est pas** un flux CloudStream et il ne récupère pas les vidéos tout seul.

## Ajouter ou modifier un lien

1. Modifie `Sources/sources.json`.
2. Garde un identifiant stable, unique, en minuscules avec des tirets (ex. `arte`).
3. Renseigne `name`, `url` (HTTPS), `type` et `description`.
4. Lance localement `python3 scripts/validate_sources.py` ou laisse GitHub Actions vérifier le changement.
5. Vérifie que la compilation/publication termine sans erreur.

Ne crée pas un nouvel identifiant pour une simple modification d'adresse : conserve l'identifiant existant afin que les futures mises à jour restent faciles à suivre. Les contrôles ne font volontairement pas de requêtes réseau : un site peut bloquer les robots ou être temporairement indisponible, ce qui rendrait la CI instable.

## Règles de maintenance

- Privilégie les pages officielles et les flux que tu as l'autorisation d'intégrer.
- Ne stocke pas de mots de passe, jetons ou données privées dans ce fichier.
- Le catalogue ne configure pas automatiquement les extensions Kotlin : si l'adresse de base d'un fournisseur change, modifie aussi son fichier `*Config.kt` et teste l'extension correspondante.
- Les extensions doivent utiliser des API officielles, des flux autorisés ou des contenus du domaine public/licenciés.

