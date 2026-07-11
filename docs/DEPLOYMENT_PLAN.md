# Deployment Plan

- Local development: Docker Compose.
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
