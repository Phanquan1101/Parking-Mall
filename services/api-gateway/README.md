# API Gateway

Responsibility: request routing, local-development CORS, auth forwarding, and future gateway-level rate limiting as defined in the SAD.

Current status: Slice 1 auth proxy. It forwards `POST /api/auth/login`, `GET /api/auth/me`, and `GET /identity/health` to Identity Service. Bearer tokens are passed through; Identity Service validates JWTs.

Run locally:

```powershell
mvn spring-boot:run
```

Default port: `8080` (`API_GATEWAY_PORT`).
