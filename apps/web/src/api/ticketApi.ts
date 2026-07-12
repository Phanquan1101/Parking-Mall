import type { PublicTicket } from "../types/ticket";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

export class TicketApiError extends Error {
  constructor(public readonly status?: number) {
    super("Unable to load the ticket.");
  }
}

function isPublicTicket(value: unknown): value is PublicTicket {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const ticket = value as Record<string, unknown>;
  return typeof ticket.sessionId === "string"
    && typeof ticket.sessionCode === "string"
    && typeof ticket.vehiclePlate === "string"
    && typeof ticket.vehicleType === "string"
    && typeof ticket.status === "string"
    && typeof ticket.paymentStatus === "string"
    && typeof ticket.entryTime === "string"
    && typeof ticket.durationMinutes === "number"
    && typeof ticket.estimatedFee === "number"
    && typeof ticket.discountAmount === "number"
    && typeof ticket.finalFee === "number"
    && typeof ticket.canGenerateExitPass === "boolean"
    && typeof ticket.exitPassAvailable === "boolean"
    && typeof ticket.exitPassMessage === "string"
    && typeof ticket.message === "string";
}

export async function getPublicTicket(lookupToken: string): Promise<PublicTicket> {
  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl}/api/public/tickets/${encodeURIComponent(lookupToken)}`);
  } catch {
    throw new TicketApiError();
  }

  if (!response.ok) {
    throw new TicketApiError(response.status);
  }

  const payload: unknown = await response.json();
  if (!isPublicTicket(payload)) {
    throw new TicketApiError();
  }

  return payload;
}
