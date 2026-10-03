# TODO — Persistance via base de données

Objectif : faire survivre l'état du jeu (roster, exécutions de missions, journal) entre les redémarrages de l'application. Aujourd'hui tout est en mémoire : `ConsulState` et le champ `activeExecution` de `GameController` sont perdus à chaque arrêt.

## 1. Choix technique : PostgreSQL embarqué (Zonky), H2 pour les tests

> **Statut : implémenté** (voir `EmbeddedPostgresConfig`, paquet `com.extremis.db`, `GameService`).

Système retenu : **PostgreSQL embarqué via les binaires Zonky** (`io.zonky.test:embedded-postgres`), piloté par **Spring Data JPA** (Hibernate).

Pourquoi :
- Aucun Docker ni installation : les binaires PostgreSQL sont téléchargés par Maven et la base démarre in-process (`EmbeddedPostgresConfig`, profil par défaut). La base persiste sur disque dans `./data/postgres` (`setCleanDataDirectory(false)`).
- Pour les **tests** (profil `test-rapide`) : **H2 en mémoire** — aucun service externe, tests rapides. Les entités restent compatibles H2 (attention aux mots réservés : la colonne `value` de `character_skills` s'appelle `skill_value`).

Dépendances à ajouter dans `pom.xml` :
- `spring-boot-starter-data-jpa`
- `org.postgresql:postgresql` (runtime)
- `com.h2database:h2` (test / profil local)

## 2. Schéma de base de données

### 2.1 Entités (dans `com.extremis.db`, miroir JPA du cœur `com.extremis.core`)

Le paquet `core` reste du pur code métier, les entités JPA sont des adaptateurs séparés.

- `CharacterEntity` (table `characters`)
  - `id` (VARCHAR, PK), `name`, `coterie`, `health` (défaut 10), `alive` (booléen)
  - relations : `traits` (1-N), `skills` (1-N), `inventory` (1-N)
- `TraitEntity` (table `character_traits`) : `name`, `characterId`. Un trait porte un `Set<String> grants` aujourd'hui — sérialiser `grants` en colonne TEXT (séparateur `;` ou JSON) suffit.
- `SkillEntity` (table `character_skills`) : `skill` (nom de l'enum `Skill`), `value` (int). Contrainte unique (`characterId`, `skill`).
- `InventoryItemEntity` (table `character_items`) : `label`.
- `GameCharacter` (table `available_characters`) : **catalogue des personnages disponibles au recrutement** (voir §3).
- `ConsulStateEntity` (table `consul_state`, ligne unique) : `jokers`, `simulations_left`, `last_recruitment`, `last_mission_taken`, `game_time_offset`.
- `MissionExecutionEntity` (table `mission_executions`) : **état d'exécution des missions** (voir §4).
- `ExecutionLogLineEntity` (table `execution_log_lines`) : lignes de journal d'une exécution, ordre conservé via une colonne `line_index`.

### 2.2 Mapping enum / code

- `Skill` : stocker le nom (`COMBAT`, `DISCRETION`, ...). `@Enumerated(EnumType.STRING)`.
- Les définitions de missions (`Mission`, `MissionEvent`, `SkillTest`) restent **en pur code** (`catalog/Missions.java`). La table `mission_executions` ne stocke que l'identifiant de mission (`mission_id`) et relit la définition au chargement. Contrainte : si une mission du code disparaît, l'exécution orpheline est marquée « échouée/abandonnée » plutôt que de faire planter le chargement.

## 3. Catalogue des personnages disponibles en base

Le wiki (Notion, « Jeu des tables aléatoires → Persos - catalogue → Par rôles / Par coterie ») définit les coteries et rôles. La table `available_characters` reprend cette structure :

- `id` (PK), `name`, `coterie` (bastet, carthaginois, cathare, celte, conquistador, culte-du-plaisir, demokratos, elfe, esthete, feerique, jacobin, khaos, libertin, lotus-blanc, lumieres, ogre, performeur, romain, saabi, schweizer, skaven, templier, transhumaniste, tyranide, zaporogue, tzigane, ork, acheron), `role` (enqueteur, specialistes-volonte, voyageur, citadin, ennemis-acheron, saltimbanque, vilain, vauban)
- compétences : comme `SkillEntity`
- traits : comme `TraitEntity`
- `health`, `alive`, `inventory`

Le recrutement (`POST /recruit`) sélectionne alors un personnage dans `available_characters` (au lieu de créer un `Character` avec `COMBAT 40` en dur dans `GameController`) et copie ses données vers `characters`.

### 3.1 Données de test temporaires (seed)

Créer un seeder (`db/CharacterSeeder`, profil `local` / `test-rapide`) insérant ~10 personnages inspirés des sous-pages du wiki :

| id | nom | coterie | rôle | compétences | traits |
|---|---|---|---|---|---|
| seed-01 | Alphonse Hercule de Gascoigne | performeur | enqueteur | ERUDITION 10, AGILITE 5, COMBAT 0 | Esthète, Orgueilleux, Chaste |
| seed-02 | Marat | jacobin | enqueteur | DIPLOMATIE 8, ERUDITION 6 | Radical |
| seed-03 | Voltaire | lumieres | specialistes-volonte | ERUDITION 9, DIPLOMATIE 7 | Esprit |
| seed-04 | Rorschach | khaos | enqueteur | COMBAT 7, PERCEPTION 9, DISCRETION 6 | Implacable |
| seed-05 | Arsène Lupin | esthete | enqueteur | DISCRETION 10, AGILITE 8, TECHNIQUE 6 | Cambrioleur |
| seed-06 | Albios le barde | celte | specialistes-volonte | DIPLOMATIE 8, ERUDITION 5 | Barde |
| seed-07 | Mata-Hari | lotus-blanc | enqueteur | DISCRETION 9, DIPLOMATIE 8 | Espionne |
| seed-08 | Odysseus | demokratos | voyageur | DIPLOMATIE 7, COMBAT 6, TECHNIQUE 9 | Stratège |
| seed-09 | Vidocq | citadin | enqueteur | PERCEPTION 8, DISCRETION 8, COMBAT 5 | Inspecteur |
| seed-10 | Welf Schwarzschütze | elfe | voyageur | COMBAT 8, PERCEPTION 7, AGILITE 6 | Traqueur |

Ces données sont **temporaires** : à remplacer progressivement par le vrai catalogue une fois les fiches du wiki saisies. Le seeder doit être idempotent (`INSERT ... ON CONFLICT DO NOTHING` / `existsById` avant insertion).

## 4. État d'exécution des missions en base

Aujourd'hui `MissionExecution` (événements planifiés, curseur `nextEvent`, journal, équipe) vit dans un champ du contrôleur. À persister :

- `MissionExecutionEntity`
  - `id` (PK auto), `mission_id` (VARCHAR, référence l'id de la mission en code, ex. `gotheim`)
  - `status` (enum `IN_PROGRESS`, `FINISHED`, `ABANDONED`)
  - `start_time` (Instant), `current_time` — remplacer `timeOffset` du contrôleur par un `game_time` persisté
  - `next_event_index` (int) : curseur d'avancement, `scheduled_times` sérialisés (une colonne par événement ou JSON)
  - relations vers `characters` (équipe de la mission : copie indépendante des personnages, la mort d'un arrangeur sur une mission ne doit pas être perdue) et `execution_log_lines`
  - `finished_at`, `team_wiped` (booléen) pour le rapport final (`MissionReport`)
- Au chargement de l'application (`ApplicationReadyEvent` ou requête paresseuse) : retrouver l'exécution `IN_PROGRESS`, la reconstruire à partir de la définition `Mission` en code (via `mission_id`) et de `next_event_index`.
- Le `@Scheduled` `tick()` doit recharger l'état depuis la base (ou ne tick qu'une exécution chargée), et **persister chaque avancement** (`advance`) dans une transaction.
- Les définitions de mission ne sont **pas** en base : la création de mission (`catalog/Missions.java`, `MissionConfig`) reste en pur code, seule l'exécution y fait référence par `mission_id`.

## 5. Refactorings nécessaires

- [ ] Extraire la logique d'état de `GameController` vers un service (`GameService`) qui utilise les repositories JPA au lieu des champs en mémoire (`consul`, `activeExecution`, `eventLog`, `timeOffset`).
- [ ] Rendre `ConsulState` persistant (ligne unique en base) plutôt qu'un champ `new ConsulState()` dans le contrôleur.
- [ ] Ajouter les repositories Spring Data : `CharacterRepository`, `AvailableCharacterRepository`, `ConsulStateRepository`, `MissionExecutionRepository`.
- [ ] Configuration : `spring.jpa.hibernate.ddl-auto=update` en local, `validate` + migrations **Flyway** (`db/migration/V1__schema.sql`) sinon.
- [ ] `application.properties` : profils `local` (PostgreSQL) et `test` (H2 mem), `mission.tick-rate-ms` conservé.
- [ ] Mettre à jour `GameControllerIntegrationTest` pour utiliser H2 + seed, et vérifier qu'une exécution survit à un « redémarrage » (nouveau contexte / nettoyage du bean contrôleur).

## 6. Plan de tests

- [ ] Test d'intégration : recruter → couper/relancer le contexte Spring → le roster est intact.
- [ ] Test d'intégration : lancer une mission → redémarrage du contexte → l'exécution `IN_PROGRESS` est rechargée avec le bon `next_event_index` et son journal.
- [ ] Test : la mort d'un personnage en mission est bien persistée (santé 0, `alive=false`).
- [ ] Test : les personnages du seed sont bien présents et recrutables.
- [ ] Test : `mission_id` référencé inexistant en code → exécution marquée `ABANDONED` sans erreur au chargement.
- [ ] Conserver `mvn test` vert avec le profil `test-rapide` existant.

## 7. Ordre de mise en œuvre suggéré

1. Dépendances + configuration des deux profils (`local` PostgreSQL / `test` H2) + Flyway V1.
2. Entités + repositories (characters, available_characters, consul_state, mission_executions, log lines).
3. Seeder des 10 personnages temporaires du §3.1.
4. `GameService` : recrutement depuis `available_characters`, persistance du roster et du consul.
5. Persistance des exécutions de missions (création, tick, rechargement au démarrage).
6. Tests d'intégration de survie au redémarrage (§6).
7. Mise à jour du `README.md` (lancement local : `docker run postgres` ou H2 fichier, profils Maven).
