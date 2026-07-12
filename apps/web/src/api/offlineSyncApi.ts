import type { OfflineCheckInEvent, OfflineQueueStatus } from "../offline/offlineTypes";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

export type OfflineSyncResult = { eventId: string; status: OfflineQueueStatus; serverSessionId?: string; sessionCode?: string; message: string };

export async function syncOfflineEvents(token: string, deviceId: string, events: OfflineCheckInEvent[]): Promise<OfflineSyncResult[]> {
  const response = await fetch(`${apiBaseUrl}/api/parking/offline-sync`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}`, "Idempotency-Key": crypto.randomUUID() },
    body: JSON.stringify({ deviceId, events: events.map(({ syncStatus, message, serverSessionId, sessionCode, ...event }) => event) }),
  });
  if (!response.ok) throw new Error("Unable to sync offline events.");
  const payload = await response.json() as { results?: OfflineSyncResult[] };
  return payload.results ?? [];
}
