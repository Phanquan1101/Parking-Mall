import type { OcrResult } from "../api/visionApi";

export type GateEntryState = "CAMERA_IDLE" | "CAMERA_STARTING" | "SCANNING" | "RECOGNIZING" | "PLATE_READY" | "CHECKING_IN" | "QR_READY" | "ERROR";

export const MIN_CONFIDENCE_TO_LOCK = 0.7;
export const SCAN_COOLDOWN_MS = 1800;
export const INITIAL_BACKOFF_MS = 2000;
export const MAX_BACKOFF_MS = 8000;

export function shouldLockCandidate(result: OcrResult): boolean {
  return Boolean(result.candidatePlate && result.confidence >= MIN_CONFIDENCE_TO_LOCK);
}

export function buildOcrCheckInPayload(input: {
  confirmedPlate: string;
  vehicleType: "MOTORBIKE" | "CAR";
  entryGate: string;
  reservationCode: string;
  ocr: OcrResult;
}) {
  return {
    vehiclePlate: input.confirmedPlate.trim(),
    vehicleType: input.vehicleType,
    entryGate: input.entryGate.trim(),
    plateSource: "OCR_ASSISTED",
    ocrRequestId: input.ocr.ocrRequestId,
    ocrCandidatePlate: input.ocr.candidatePlate,
    ocrConfidence: input.ocr.confidence,
    reservationCode: input.reservationCode.trim() || undefined,
  };
}

export function buildManualCheckInPayload(input: {
  confirmedPlate: string;
  vehicleType: "MOTORBIKE" | "CAR";
  entryGate: string;
  reservationCode: string;
}) {
  return {
    vehiclePlate: input.confirmedPlate.trim(),
    vehicleType: input.vehicleType,
    entryGate: input.entryGate.trim(),
    plateSource: "MANUAL",
    reservationCode: input.reservationCode.trim() || undefined,
  };
}

export function retryBackoffMs(consecutiveFailures: number): number {
  return Math.min(INITIAL_BACKOFF_MS * 2 ** Math.max(0, consecutiveFailures - 1), MAX_BACKOFF_MS);
}

export function customerTicketUrl(lookupToken: string, origin = window.location.origin): string {
  return `${origin}/tickets/${encodeURIComponent(lookupToken)}`;
}
