# Estrategia de Testing — TP Etapa 1 (Programación IV)

Opción 1: proyecto desarrollado en clase (fork de `BACK-JOBS-API`) + tests E2E del front con Selenium.

## Pirámide de tests

| Nivel | Qué valida | Herramientas | Dependencias externas |
|---|---|---|---|
| **Unitarios** | Lógica de una clase aislada | JUnit 5 + Mockito, MockMvc standalone | Ninguna (todo mockeado) |
| **Integración** | Controller → Service → Repository dentro de Spring | `@SpringBootTest` + MockMvc, MockWebServer | API ReqRes simulada con MockWebServer; `jobs.json` real |
| **E2E API** | La API levantada de punta a punta, por HTTP real | Postman / Newman | API levantada + ReqRes real |
| **E2E Front** | El flujo de login del usuario en el navegador | Selenium + TestNG + ExtentReports | Front levantado ([repo Selenium](https://github.com/osorio-cristian-wt/Selenium)) |

## Cobertura de lo pedido en el enunciado

| Requisito | Archivo | Casos |
|---|---|---|
| Unitarios de `UserServiceImpl` (éxito y excepciones) | `service/impl/UserServiceImplTest.java` | 12 |
| Corregir `JobControllerTest` por la actualización del fork | `controller/JobControllerTest.java` | 6 |
| ↳ casos movidos de `/api/job/users` → `/api/user` | `controller/UserControllerTest.java` | 6 |
| ↳ casos movidos de `/api/job/assign` → `/api/assign` | `controller/AssignControllerTest.java` | 5 |
| Integración de `AssignController` | `controller/AssignControllerIntegrationTest.java` | 5 |
| Integración de `JobController` | `controller/JobControllerIntegrationTest.java` | 4 |
| E2E de `/api/assign` | `src/test/resources/postman/assign.postman_collection.json` | 7 requests / 18 aserciones |
| Tests del flujo de login del front | repo Selenium: `LoginTest.java` | 10 |

Además se corrigió `UserControllerIntegrationTest` (venía del upstream): fallaba por JSON inválido y porque
`getUserById()` ahora busca en la página 1 de ReqRes en vez de pedir el usuario por id.

### Por qué se "corrigió" JobControllerTest así

El test de clase probaba `/api/job/users/{page}` y `/api/job/assign`, pero el commit *Separación Controllers*
dejó a `JobController` solo con `/api/job/all` y `/api/job/{id}`. Se reescribió para esos dos endpoints
(éxito, lista vacía, error del service → 500, id inexistente → 500, id no numérico → 400) y los casos viejos
se movieron a los tests de los controllers donde ahora viven esos endpoints.

### Casos de UserServiceImpl

- `search(page)`: asigna `jobId` correlativos 1..n · usa la página recibida · página vacía · el error del repositorio se propaga · `data` null → NPE · respuesta null → NPE
- `searchById(id)`: devuelve el usuario con `jobId = 1` · usuario inexistente → NPE · error del repositorio se propaga
- `update(user)`: delega una vez en el repositorio · error envuelto en `RuntimeException("Error al crear el usuario: ...")` conservando la causa · usuario null

Los casos que esperan `NullPointerException` documentan el comportamiento actual (no hay validación de nulos);
si se agrega manejo de errores en el servicio, hay que actualizar esos tests.

## Cómo correr todo

```bash
# Unitarios + integración (+ cobertura en target/site/jacoco/index.html)
docker compose run --rm tests          # o: mvn test

# E2E de /api/assign: levanta la API y corre Newman (reporte en target/e2e/assign-e2e.html)
docker compose run --rm e2e
# o con la API ya levantada en tu PC:
npx -p newman -p newman-reporter-htmlextra newman run src/test/resources/postman/assign.postman_collection.json \
    -e src/test/resources/postman/local.postman_environment.json -r cli,htmlextra

# E2E del front: en el repo Selenium
docker compose run --rm e2e            # levanta front + navegador; ver en vivo en http://localhost:7900
```

La colección también se puede importar en Postman (*Import → archivo*) y correr con el *Collection Runner*.

## Notas

- Los tests E2E de la API usan la API externa real de usuarios (ReqRes): necesitan internet.
- El front corre en modo mock (`REACT_APP_USE_MOCK=true`): credenciales válidas `admin@correo.com` / `123`.
- CI: cada push corre unitarios + integración + E2E en GitHub Actions y publica los reportes como artifacts.
