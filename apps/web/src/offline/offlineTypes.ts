export type OfflineQueueStatus = "PENDING" | "SYNCING" | "SYNCED" | "DUPLICATE" | "REJECTED" | "CONFLICT" | "MANUAL_REVIEW_REQUIRED";

export type OfflineCheckInEvent = {
  eventId: string;
  eventType: "OFFLINE_CHECK_IN";
  idempotencyKey: string;
  localTimestamp: string;
  payload: { vehiclePlate: string; vehicleType: "MOTORBIKE" | "CAR"; entryGate: string; plateSource: "MANUAL" };
  syncStatus: OfflineQueueStatus;
  message?: string;
  serverSessionId?: string;
  sessionCode?: string;
};
