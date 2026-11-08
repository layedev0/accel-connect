# Scripts SQL antérieurs à Flyway — archive

Ces scripts datent d'avant l'adoption de Flyway. Ils n'ont **jamais été exécutés par un
outil de migration** : Flyway n'était pas au classpath, et le schéma était produit par
`hibernate.ddl-auto=update`. Ils ont été appliqués à la main, quand ils l'ont été.

Ils ne sont pas rejouables :

- la numérotation **commence à `V7`** — aucun script ne crée le schéma de départ ;
- `V10`, `V11` et `V15` sont écrits en **syntaxe MySQL** (`INT AUTO_INCREMENT PRIMARY KEY`,
  `DATETIME(6)`, `UPDATE … JOIN … SET`) et échouent sur PostgreSQL.

Leur effet est intégralement contenu dans
[`../migration/V1__baseline_schema.sql`](../migration/V1__baseline_schema.sql), l'empreinte
du schéma réel prise au moment de la bascule.

Ils sont conservés pour l'historique : ne pas les remettre dans `db/migration/`.
