# hands_ON - Inventrack

Drone parts/products inventory management API (Spring Boot 3, Java 21+, JPA, JWT).

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Dev DB: in-memory H2, seeded with 5 sample inventory centers (`app.seed-data`).
- JWT: an ephemeral RSA key pair is generated at startup. Set `app.jwt.private-key-location` /
  `app.jwt.public-key-location` (PKCS#8 / X.509 PEM) for persistent keys.

## Test

```powershell
.\mvnw.cmd test
```

## API

| Endpoint | Auth |
| --- | --- |
| `POST /api/v1/auth/register`, `/authenticate`, `/refresh-token` | none (refresh needs `Bearer <refreshToken>`) |
| `GET /api/v1/users/{id}` | self or ADMIN |
| `GET /api/v1/centers` (centerName, address, operationalStatus, page, size, sort, order) | USER+ |
| `POST /api/v1/centers`, `PUT /api/v1/centers/{centerId}`, `DELETE /api/v1/centers/{centerId}` | USER+ |

Errors use `{ "error": { "code", "message", "timestamp" } }`.
