# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

CardLink is a microservices backend for a platform for trading card game (TCG) players and stores: profiles, stores, product inventory, events (tournaments, prereleases), player groups, card trades, and store geolocation. Java 17 + Spring Boot 3.5.14 + Spring Cloud 2025.0.2 + Maven, with Eureka service discovery and a single-entry API Gateway. Inter-service communication is exclusively via **Feign clients** resolved through Eureka (`lb://ms-xxx`, never hardcoded URLs), exchanging DTOs over HTTP — there is no shared database and no message broker.

This is a DUOC (Chilean university) class project / MVP, not a production system — see "Known limitations" below before suggesting hardening work that's out of scope.

## Services and ports

| Service (dir) | Eureka name | Port | Owns |
|---|---|---|---|
| `eureka-server` | — | 8761 | Service registry/dashboard |
| `gateway` | gateway | 8080 | Single entry point, Spring Cloud Gateway routes |
| `Login` | ms-login | 8081 | Registration, login, JWT issuance, token validation |
| `Usuarios` | ms-usuarios | 8082 | Profile, favorite games, participation counters |
| `Grupos` | ms-grupos | 8083 | Player groups |
| `Tiendas` | ms-tiendas | 8084 | Stores |
| `Inventario` | ms-inventario | 8085 | Store product catalog |
| `Localizacion` | ms-localizacion | 8086 | Store addresses/geolocation |
| `Eventos` | ms-eventos | 8087 | Tournaments/events |
| `Intercambios` | ms-intercambios | 8088 | Card trades between players |

Gateway routes (`gateway/src/main/resources/application.yaml`) map external paths to internal services, and the mapping is **not 1:1 with names**: `/api/usuarios/**` → ms-login (auth), `/api/perfil/**` → ms-usuarios (profile), `/api/grupos|tiendas|inventario|localizacion|eventos|intercambios/**` → their respective services.

## Common commands

Each service is an independent Maven module with its own wrapper (no root build). Run from inside the service directory:

```powershell
cd Login
.\mvnw.cmd spring-boot:run        # run the service
.\mvnw.cmd test                   # run all tests
.\mvnw.cmd test -Dtest=LoginServiceTest        # run a single test class
.\mvnw.cmd test -Dtest=LoginServiceTest#metodoX # run a single test method
.\mvnw.cmd clean package          # build jar
```

Same commands apply to `Usuarios`, `Grupos`, `Tiendas`, `Inventario`, `Localizacion`, `Eventos`, `Intercambios`, `gateway`, `eureka-server` — substitute the directory.

### Startup order (required for Eureka/Feign resolution to work)

1. MySQL running on `localhost:3306` (XAMPP-style), user `root`, no password.
2. Manually create 4 databases that don't auto-create: `ms_login`, `ms_usuarios`, `ms_grupos`, `ms_eventos`. The other 4 (`ms_tiendas`, `ms_inventarios` — note plural, `ms_localizacion`, `ms_intercambios`) use `createDatabaseIfNotExist=true`.
3. Start `eureka-server` first, wait for `http://localhost:8761` to be up.
4. Start the 8 business microservices in any order (each needs its own terminal).
5. Start `gateway` last.

Tables are created automatically (`spring.jpa.hibernate.ddl-auto: update`); there are no SQL migration scripts (no Flyway/Liquibase).

### Verifying a running stack

- Eureka dashboard `http://localhost:8761` should list all 9 apps (8 services + gateway).
- Each service exposes Swagger at `http://localhost:<port>/swagger-ui/index.html`.

## Architecture pattern (applies to every business microservice)

Every service module (`Login`, `Usuarios`, `Grupos`, `Tiendas`, `Inventario`, `Localizacion`, `Eventos`, `Intercambios`) follows the identical internal layout under `src/main/java/cl/duoc/ms_<nombre>/`:

- `controller/` — REST endpoints
- `service/` — business logic
- `repository/` — Spring Data JPA repositories
- `model/` — JPA entities (kept separate from DTOs)
- `dto/` — request/response DTOs, never expose entities directly
- `config/FiltroJwt.java` — custom JWT filter (present in every service except `Login`, which issues tokens instead of validating incoming ones)
- `config/ConfiguracionSeguridad.java` — Spring Security config wiring the JWT filter
- `config/ManejadorErroresValidacion.java` — global exception handling via `@RestControllerAdvice`, present in every service
- `security/JwtUtil.java` — JWT parsing/generation helper
- `client/` or `clientes/` — `@FeignClient` interfaces for calling other services (only in services that need cross-service data; naming of the package is inconsistent — `client` in most, `clientes` in `Tiendas`)

Bean Validation is used for request DTO validation.

### Cross-service calls (Feign)

- Feign clients are interfaces annotated `@FeignClient(name = "ms-xxx")` — the name matches the target's `spring.application.name`/Eureka registration, and Feign resolves the host via Eureka load-balancing (`lb://`). No URLs are hardcoded in `application.yml`.
- The caller forwards the `Authorization` header explicitly to the callee via `@RequestHeader("Authorization") String authHeader`, since the downstream service also validates the JWT independently.
- Known cross-service dependencies: `Inventario` → `Tiendas` (verify store exists/is active and is owned by the caller before catalog mutations), `Eventos` → `Tiendas`, `Usuarios`; `Intercambios` → `Usuarios`; `Localizacion` → `Tiendas`; `Tiendas` → `Inventario`, `Localizacion`, `Login`.
- Profile counters (`eventosParticipados`, `intercambiosCompletados`) are updated via "fire and forget" Feign calls from `Eventos`/`Intercambios` to `Usuarios` — if `Usuarios` is down at that moment the call fails silently and the counter is not retried. Keep this in mind when working on counter-related bugs — it's a known limitation, not necessarily a bug to fix without being asked.

### JWT

All services share the **same hardcoded JWT secret** (in each `application.yml`, base64 `c2VjcmV0...`) so that a token issued by `Login` can be validated independently by every other service without a central auth call. This is fine for this MVP context — do not "fix" it by introducing per-service secrets unless explicitly asked, as it would break cross-service auth.

## Known limitations (MVP, intentional — don't "fix" silently)

- JWT secret hardcoded and identical across all services in `application.yml`; no HTTPS.
- No Docker/docker-compose/CI — stack is started manually, service by service.
- `Inventario`'s database is named `ms_inventarios` (plural), inconsistent with the rest (singular) — cosmetic only, no cross-DB references exist.
- Counter updates between `Eventos`/`Intercambios` and `Usuarios` are fire-and-forget with no retry (see above).

## Troubleshooting

- **A service doesn't show up in Eureka**: check MySQL is running and that the service's database exists (especially `Login`, `Usuarios`, `Grupos`, `Eventos`, which don't auto-create their DB). Check the service's startup log for DB connection errors.
- **Gateway returns 404**: the target microservice isn't registered in Eureka yet (check `:8761` dashboard) before retrying through the Gateway.
