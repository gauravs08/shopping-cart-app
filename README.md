# Shopping Cart App

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-green)
![Build](https://img.shields.io/badge/build-Gradle%208.12-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey) 

A full-featured e-commerce shopping cart application built with Spring Boot, inspired by platforms like Zalando and Temu. Features OAuth2 authorization, product catalog, shopping cart, order management, reviews, and wishlists.

## Tech Stack

| Layer           | Technology                                      |
|-----------------|-------------------------------------------------|
| Language        | Java 21                                         |
| Framework       | Spring Boot 3.4.5                               |
| Security        | Spring Security + OAuth2 Authorization Server   |
| Database        | PostgreSQL 16 (prod), H2 (dev/test)             |
| Migrations      | Flyway                                          |
| ORM             | Spring Data JPA / Hibernate                     |
| Caching         | Caffeine                                        |
| Mapping         | MapStruct 1.6.3                                 |
| API Docs        | SpringDoc OpenAPI 2.8.6 (Swagger UI)            |
| Build           | Gradle 8.12 (Groovy DSL)                        |
| Code Quality    | JaCoCo (80% minimum coverage)                   |
| Containerization| Docker + Docker Compose                         |
| Testing         | JUnit 5, Testcontainers, Spring Security Test   |

## Architecture

```
                    +-------------------+
                    |   Swagger UI      |
                    |   /swagger-ui     |
                    +--------+----------+
                             |
                    +--------v----------+
                    |   REST API Layer  |
                    |   (Controllers)   |
                    +--------+----------+
                             |
              +--------------+--------------+
              |              |              |
     +--------v---+  +------v-----+  +-----v------+
     |  Security  |  |  Service   |  |  Caching   |
     |  (OAuth2)  |  |  Layer     |  |  (Caffeine)|
     +--------+---+  +------+-----+  +-----+------+
              |              |              |
              +--------------+--------------+
                             |
                    +--------v----------+
                    |  Repository Layer |
                    |  (Spring Data JPA)|
                    +--------+----------+
                             |
                    +--------v----------+
                    |  PostgreSQL / H2  |
                    |  (Flyway managed) |
                    +-------------------+
```

## Prerequisites

- Java 21 (JDK)
- Docker & Docker Compose (for containerized setup)
- PostgreSQL 16 (optional, only if running without Docker)

## Quick Start

### Option 1: Development Mode (H2 Database)

```bash
./gradlew bootRun
```

The application starts on `http://localhost:8080` with an embedded H2 database.

- H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./db/shopping_cart;MODE=PostgreSQL;AUTO_SERVER=TRUE`
- Username: `sa`, Password: (empty)

### Option 2: Docker Compose (PostgreSQL)

```bash
docker compose up -d
```

Starts both PostgreSQL and the application:
- App: http://localhost:8080
- PostgreSQL: localhost:5432

### Build & Test

```bash
# Build the project
./gradlew build

# Run tests only
./gradlew test

# Run tests with coverage report
./gradlew test jacocoTestReport

# Verify coverage meets threshold (80%)
./gradlew check
```

Coverage report is generated at `build/reports/jacoco/test/html/index.html`.

## API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## Authentication Endpoints

| Endpoint             | Description                      |
|----------------------|----------------------------------|
| `POST /auth/login`   | Login and get JWT access token   |
| `POST /auth/register`| Register a new user account      |

## Default Credentials

| User   | Email           | Password  | Roles                  |
|--------|-----------------|-----------|------------------------|
| Admin  | admin@shop.com  | admin123  | ROLE_ADMIN, ROLE_USER  |
| User   | user@shop.com   | user123   | ROLE_USER              |
| Seller | seller@shop.com | seller123 | ROLE_SELLER, ROLE_USER |

## Project Structure

```
shopping-cart-app/
├── build.gradle
├── settings.gradle
├── docker-compose.yml
├── Dockerfile
├── gradlew / gradlew.bat
├── gradle/wrapper/
│   └── gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── java/com/shopping/app/
    │   │   ├── config/          # Security, cache, OpenAPI config
    │   │   ├── controller/      # REST controllers
    │   │   ├── dto/             # Request/response DTOs
    │   │   ├── entity/          # JPA entities
    │   │   ├── exception/       # Custom exceptions & handlers
    │   │   ├── mapper/          # MapStruct mappers
    │   │   ├── repository/      # Spring Data repositories
    │   │   └── service/         # Business logic services
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── db/migration/    # Flyway SQL migrations
    └── test/
        ├── java/com/shopping/app/
        │   └── ...              # Unit & integration tests
        └── resources/
            └── application-test.yml
```

## Key Features

- **Product Catalog** - Browse products by category, search, filter by price/brand/rating
- **Shopping Cart** - Add/remove items, update quantities, persistent per user
- **Wishlist** - Save products for later
- **Order Management** - Place orders, track status, order history
- **User Accounts** - Registration, login, profile management
- **Reviews & Ratings** - Product reviews with verified purchase badges
- **Address Book** - Multiple shipping addresses per user
- **Role-Based Access** - User, Admin, and Seller roles
- **OAuth2 Security** - Token-based authentication with JWT
- **Caching** - Caffeine cache for frequently accessed data
- **API Documentation** - Interactive Swagger UI
- **Health Monitoring** - Spring Actuator endpoints

## Environment Variables (Production)

| Variable         | Description                 | Default                 |
|------------------|-----------------------------|-------------------------|
| `DB_PASSWORD`    | PostgreSQL password         | `shopping_secret`       |
| `JWT_SECRET`     | JWT signing secret (Base64) | -                       |
| `JWT_EXPIRATION` | Token expiration in ms      | `3600000`               |
| `CORS_ORIGINS`   | Allowed CORS origins        | `http://localhost:3000` |
| `APP_ISSUER_URL` | OAuth2 issuer URL           | `http://localhost:8080` |
