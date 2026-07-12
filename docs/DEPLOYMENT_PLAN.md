# Deployment Plan

## Local development modes

- Recommended Hybrid Local Development: PostgreSQL runs with `docker compose --profile infra up -d`; backend services run from IntelliJ on `8080`–`8086` (Vision on `8090`); frontend runs from VS Code on `5173`.
- Optional Full Docker Stack: `docker compose --profile full-stack up --build`; backend containers retain internal ports `8080`–`8090` but publish `18080`–`18090` so they cannot conflict with IntelliJ.
- PostgreSQL publishes `5432` by default in both modes and uses the `infra` and `full-stack` profiles.
- Hybrid service URLs use `localhost`; Docker service URLs use Compose names such as `http://parking-service:8082`.
- Frontend base URL is `http://localhost:8080` in hybrid mode and `http://localhost:18080` for the full Docker backend.
- Plain `docker compose up` intentionally starts no profiled services; developers should choose a profile explicitly.

- Local development infrastructure: Docker Compose.
- Database: Supabase PostgreSQL preferred, with local PostgreSQL fallback.
- Storage: Supabase Storage.
- Cloud demo: Railway preferred; hybrid local/cloud is accepted.
- Payment webhook public endpoint: Railway or a development tunnel.
- Payment mode order: `SIMULATION`, then `SEPAY_TEST`, then `SEPAY_LIVE` only after MVP.
- OCR service may run locally during the demo.
- Railway ports, proxy values, URLs, and secrets must be environment variables.
- Required environment variables must match `.env.example`.
- SePay Live remains disabled for MVP.

## Deployment responsibilities

- API Gateway and core services may run on Railway for the cloud demo.
- Supabase provides PostgreSQL and Storage.
- Vision/OCR may remain local when cloud resource limits make that preferable.
- Docker Compose remains the local integration and fallback path.
