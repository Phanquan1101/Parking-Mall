# Identity Service

Responsibility: users, roles, JWT, and staff/merchant/admin access as defined in the SAD.

Current status: Slice 1 in-memory demo authentication. It exposes `/api/auth/login`, protected `/api/auth/me`, and Actuator health at `/actuator/health`.

Demo users are intentionally in-memory and BCrypt-hashed for this slice only. They must be replaced with persistent identity storage before production.

Run locally:

```powershell
mvn spring-boot:run
```

Default port: `8081` (`IDENTITY_SERVICE_PORT`).
