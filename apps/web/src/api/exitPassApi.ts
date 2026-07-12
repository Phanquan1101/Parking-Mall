const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

export type ExitPass = {
  exitPassToken: string;
  sessionId: string;
  sessionCode: string;
  expiresAt: string;
  ttlSeconds: number;
  status: string;
  message: string;
};

export async function generateExitPass(sessionId: string, lookupToken: string): Promise<ExitPass> {
  const response = await fetch(`${apiBaseUrl}/api/parking/sessions/${encodeURIComponent(sessionId)}/exit-passes`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ lookupToken }),
  });
  if (!response.ok) {
    throw new Error("Unable to generate Exit Pass");
  }
  return response.json() as Promise<ExitPass>;
}
