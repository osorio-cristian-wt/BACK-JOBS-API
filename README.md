# BACK-JOBS-API

API de trabajos (Spring Boot 4, Java 21) — Programación IV, UAP.

## Correr con Docker (no hace falta Java ni Maven instalados)

| Qué | Comando |
|---|---|
| Correr todos los tests | `docker compose run --rm tests` |
| Levantar la API (http://localhost:8080) | `docker compose up -d --build api` |
| GraphiQL | http://localhost:8080/graphiql |
| Shell con Java 21 + Maven | `docker compose run --rm dev` |
| Bajar todo | `docker compose down` |

Los reportes de tests quedan en `target/surefire-reports/`.

## Sin PC: GitHub Codespaces

1. En GitHub: **Code → Codespaces → Create codespace on main** (funciona desde el navegador del celular o tablet).
2. El contenedor ya trae Java 21, Maven y Docker (`.devcontainer/`).
3. En la terminal: `mvn test` o `docker compose run --rm tests`.

## CI

Cada push a `main` corre los tests en GitHub Actions (`.github/workflows/tests.yml`) y publica los reportes como artifact.
