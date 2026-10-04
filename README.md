# missionExtremis
jeu de gestion d'équipe / lancement de missions dans l'univers Extremis

## Persistance des donnees

L'application utilise PostgreSQL embarque ([binaires Zonky](https://github.com/zonkyio/embedded-postgres))
: au premier `mvn spring-boot:run` (ou lancement depuis IntelliJ), les binaires PostgreSQL
sont telecharges via Maven et la base est creee automatiquement dans `./data/postgres`.
Aucun Docker ni installation PostgreSQL n'est necessaire. L'etat du jeu (roster, catalogue
de personnages, executions de missions) survit aux redemarrages.

- Catalogue : 10 personnages temporaires au premier demarrage (cf. `docs/todo-persistance-bdd.md`).
- Tests : profil `test-rapide` avec H2 en memoire (`mvn test`).
- Pour repartir de zero : supprimer le dossier `data/`, ou lancer avec `extremis.debug=true`
  et utiliser le bouton "Reinitialiser (usine)" visible dans la console en mode debug.
