import type { OfflineCheckInEvent, OfflineQueueStatus } from "./offlineTypes";

const QUEUE_KEY = "parkflow.offline.check-in-queue";

export function loadOfflineQueue(): OfflineCheckInEvent[] {
  try {
    const value: unknown = JSON.parse(localStorage.getItem(QUEUE_KEY) ?? "[]");
    return Array.isArray(value) ? value as OfflineCheckInEvent[] : [];
  } catch {
    return [];
  }
}

export function saveOfflineQueue(events: OfflineCheckInEvent[]): OfflineCheckInEvent[] {
  localStorage.setItem(QUEUE_KEY, JSON.stringify(events));
  return events;
}

export function addOfflineCheckIn(payload: OfflineCheckInEvent["payload"]): OfflineCheckInEvent[] {
  const event: OfflineCheckInEvent = {
    eventId: crypto.randomUUID(), eventType: "OFFLINE_CHECK_IN", idempotencyKey: crypto.randomUUID(),
    localTimestamp: new Date().toISOString(), payload, syncStatus: "PENDING",
  };
  return saveOfflineQueue([event, ...loadOfflineQueue()]);
}

export function updateOfflineEvent(eventId: string, update: Partial<Pick<OfflineCheckInEvent, "syncStatus" | "message" | "serverSessionId" | "sessionCode">>): OfflineCheckInEvent[] {
  return saveOfflineQueue(loadOfflineQueue().map((event) => event.eventId === eventId ? { ...event, ...update } : event));
}

export function setQueueStatus(eventIds: string[], syncStatus: OfflineQueueStatus, message?: string): OfflineCheckInEvent[] {
  const targetIds = new Set(eventIds);
  return saveOfflineQueue(loadOfflineQueue().map((event) => targetIds.has(event.eventId) ? { ...event, syncStatus, message } : event));
}
