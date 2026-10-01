# BACK-JOBS-API

API de trabajos (Spring Boot 4, Java 21) — Programación IV, UAP.

## Correr con Docker (no hace falta Java ni Maven instalados)

| Qué | Comando |
|---|---|
| Correr unitarios + integración | `docker compose run --rm tests` |
| Correr E2E de `/api/assign` (Newman) | `docker compose run --rm e2e` |
| Levantar la API (http://localhost:8080) | `docker compose up -d --build api` |
| GraphiQL | http://localhost:8080/graphiql |
| Shell con Java 21 + Maven | `docker compose run --rm dev` |
| Bajar todo | `docker compose down` |

Reportes: `target/surefire-reports/` (JUnit), `target/site/jacoco/index.html` (cobertura), `target/e2e/assign-e2e.html` (E2E).

**Estrategia y detalle de los tests: [TESTING.md](TESTING.md).**

## Sin PC: GitHub Codespaces

1. En GitHub: **Code → Codespaces → Create codespace on main** (funciona desde el navegador del celular o tablet).
2. El contenedor ya trae Java 21, Maven y Docker (`.devcontainer/`).
3. En la terminal: `mvn test` o `docker compose run --rm tests`.

## CI

Cada push a `main` corre unitarios + integración y luego el E2E en GitHub Actions (`.github/workflows/tests.yml`) y publica los reportes como artifacts.
