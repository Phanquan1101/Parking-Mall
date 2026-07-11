# Parking Service

Responsibility: parking sessions, QR Lookup, future check-in/check-out, Dynamic Exit Pass, offline synchronization, and parking fraud signals.

Current status: Slice 2 in-memory check-in and QR Lookup. Supported endpoints are protected check-in/session search and public ticket lookup. Actuator health remains available at `/actuator/health`.

Session storage is intentionally in-memory behind `ParkingSessionRepository`. It has no database, payment, check-out, Exit Pass, or offline synchronization implementation.

Run locally:

```powershell
mvn spring-boot:run
```

Default port: `8082` (`PARKING_SERVICE_PORT`).
