# Migrations Flyway

Le schéma de la base est géré par **Flyway**. `hibernate.ddl-auto` est réglé sur `validate` :
Hibernate ne modifie plus rien, il vérifie au démarrage que le schéma correspond aux entités
et **refuse de démarrer** en cas d'écart.

## La règle

> Toute modification d'entité JPA doit être accompagnée d'un script de migration.

Sans script, la colonne ou la table n'existe pas en base, `validate` échoue et l'application
ne démarre plus — en local comme en production.

## Écrire une migration

- Nommage : `V<n>__description_courte.sql`, `n` strictement croissant. **Prochain numéro
  libre : `V25`.**
- Un script déjà appliqué **ne se modifie jamais**, et ne se renomme pas non plus : Flyway
  compare les sommes de contrôle et échoue si un script connu a changé ou a disparu. Une
  correction se fait par un nouveau script.
- Écrire du PostgreSQL. Préférer les formes idempotentes (`IF NOT EXISTS`) quand elles
  existent : un même script peut rencontrer une base où Hibernate avait déjà créé l'objet.
- Les scripts s'exécutent dans l'ordre, une seule fois, et sont enregistrés dans la table
  `flyway_schema_history`.

## Le contenu actuel

| Script | Rôle |
|---|---|
| `V1__baseline_schema.sql` | Empreinte du schéma **de production**, dumpé de `accel-connect-int` (42 tables, 6 vues). Sur les bases déjà en service, `baseline-on-migrate` l'estampille **sans l'exécuter** ; elle ne s'exécute que sur une base vide. |
| `V20__add_articles_date_audit.sql` | `articles.created_at` / `updated_at` (héritage de `DateAudit`). |
| `V21__add_article_comments_and_likes.sql` | Tables `article_comments` et `article_likes`. |
| `V22__add_article_photos.sql` | Table `article_photos` : la galerie d'un article. |
| `V23__add_articles_author.sql` | `articles.author_id` : qui a publié l'article. Nullable, les articles antérieurs n'en ont pas. |
| `V24__fix_employee_skills_skill_id_type.sql` | `employee_skills.skill_id` en `integer` : la prod l'avait en `bigint`, `validate` refusait de démarrer. |

V20 et V21 sont déjà appliqués sur la base de dev et figurent dans `flyway_schema_history` :
c'est pourquoi ils gardent ces numéros et ne peuvent pas être renumérotés ni archivés. Leur
contenu est idempotent, donc les rejouer sur une base neuve après la ligne de base est sans
effet.

Le trou de numérotation entre V1 et V20 correspond aux scripts antérieurs à Flyway, archivés
dans [`../legacy/`](../legacy/) car non rejouables.

## La réécriture de V1

La règle « un script appliqué ne se modifie jamais » a été enfreinte **une fois**, pour V1. Sa
première version n'avait pas été dumpée de la production mais d'une base de développement locale,
que des mois de `hibernate.ddl-auto=update` avaient fait diverger : trois tables mortes en trop
(`action_decidee`, `commentaire`, `document`), deux manquantes (`leave_requests`, `leave_types`),
et une dizaine de contraintes UNIQUE et NOT NULL inexistantes en production. Toute base neuve —
nouveau poste, CI — naissait donc différente de la prod, et la ligne de base décrivait un schéma
qui n'existait nulle part.

La réécriture n'a cassé aucune base existante, et la raison mérite d'être connue : là où V1 était
déjà enregistré, il l'était comme **marqueur de ligne de base** — `type = BASELINE`, `checksum`
nul. `baseline-on-migrate` estampille sans exécuter, et Flyway ne compare aucune somme de contrôle
sur ces lignes-là. Seules les bases nées vides *après* cette réécriture portent V1 en `type = SQL`
avec une somme de contrôle, et aucune n'existait avant.

Si une base signale malgré tout *Migration checksum mismatch for migration version 1*, elle se
répare une fois avec :

```sh
mvn -f backend/pom.xml org.flywaydb:flyway-maven-plugin:11.7.2:repair \
  -Dflyway.url=jdbc:postgresql://localhost:5432/accel-connect \
  -Dflyway.user=postgres -Dflyway.password=<mot-de-passe> \
  -Dflyway.locations=filesystem:backend/src/main/resources/db/migration
```

Les contraintes UNIQUE qui ont quitté la ligne de base (`users.email`, `users.username`,
`employee_details.cni`, `departments.code`, `job_categories.code`/`name`,
`job_titles.job_title`/`job_title_code`) n'existent pas en production : les rétablir demande un
script dédié, après vérification qu'aucun doublon ne s'y oppose.

## Les vues

`db/views.sql` reste en dehors de Flyway : `ViewsInitializer` le rejoue à chaque démarrage de
l'application.
