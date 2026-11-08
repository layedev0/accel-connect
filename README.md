# ACCEL-CONNECT

Application de gestion de commandes de repas avec système de subvention et suivi des budgets hebdomadaires.

## Structure du projet

```
ACCEL-CONNECT/
├── backend/               # Spring Boot API (Java 17)
│   ├── src/              # Code source Java
│   ├── database/         # Scripts SQL (vues)
│   ├── Dockerfile        # Image Docker backend
│   └── pom.xml           # Dépendances Maven
│
├── frontend/             # Application Angular
│   ├── src/              # Code source TypeScript/Angular
│   ├── Dockerfile        # Image Docker frontend
│   └── package.json      # Dépendances npm
│
└── README.md             # Ce fichier
```

## Technologies

### Backend
- **Framework**: Spring Boot 3.x
- **Langage**: Java 17
- **Base de données**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Sécurité**: Spring Security + Keycloak OAuth2
- **Build**: Maven
- **Rapports**: Apache POI (Excel)

### Frontend
- **Framework**: Angular 19
- **UI**: PrimeNG, Tailwind CSS
- **HTTP**: HttpClient
- **Auth**: OAuth2 (Keycloak)

## Démarrage

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Ou avec Docker:
```bash
cd backend
docker build -t accel-connect-back:latest .
docker run -p 8080:8080 accel-connect-back:latest
```

### Frontend

```bash
cd frontend
npm install
npm start
```

Ou avec Docker:
```bash
cd frontend
docker build -t accel-connect-front:latest .
docker run -p 8080:8080 accel-connect-front:latest
```

## Images Docker

Les images sont disponibles sur Docker Hub:
- Backend: `shams2305/accel-connect-back:v3.3`
- Frontend: `shams2305/accel-connect-front:v2.8`

## Fonctionnalités principales

- Gestion des commandes de repas
- Système de subvention (1000 FCFA)
- Calcul automatique des frais de livraison partagés
- Suivi des budgets hebdomadaires
- Rapports Excel détaillés (par restaurant, par personne, par période)
- Gestion des dépôts et remboursements
- Suivi des dettes et avances

## API Endpoints

- `GET /api/balance/my-balance` - Solde de l'utilisateur
- `GET /api/balance/stats` - Statistiques pour l'admin
- `GET /api/weekly-budget/current` - Budget de la semaine
- `POST /api/orders` - Créer une commande
- `GET /api/reports/consumption` - Rapport de consommation
