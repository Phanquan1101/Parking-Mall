# ParkFlow Mall Web

Current status: Slice 3 customer ticket page is implemented with React, Vite, TypeScript, and React Router.

`/tickets/:lookupToken` calls the public ticket API and displays only its safe customer fields. It does not include login, payment, or exit authorization.

The home route (`/`) explains the ticket-link format. Staff, merchant, and admin surfaces remain future work.

Run locally:

```powershell
npm install
npm run dev
```

The default API gateway is `http://localhost:8080`. Copy `.env.example` to `.env` to override `VITE_API_BASE_URL` for local development.
