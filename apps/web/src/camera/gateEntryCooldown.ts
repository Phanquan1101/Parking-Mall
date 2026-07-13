export const SAME_PLATE_COOLDOWN_SECONDS = 15;

export function normalizeCooldownPlate(plate: string | null | undefined): string | null {
  if (!plate) return null;
  const normalized = plate.toUpperCase().replace(/[^A-Z0-9]/g, "");
  return normalized || null;
}

export function isPlateInCooldown(recentPlates: Map<string, number>, plate: string | null | undefined, now: number, cooldownSeconds = SAME_PLATE_COOLDOWN_SECONDS): boolean {
  const normalized = normalizeCooldownPlate(plate);
  if (!normalized) return false;
  const lockedAt = recentPlates.get(normalized);
  return lockedAt !== undefined && now - lockedAt < cooldownSeconds * 1000;
}

export function rememberLockedPlate(recentPlates: Map<string, number>, plate: string | null | undefined, now: number): void {
  const normalized = normalizeCooldownPlate(plate);
  if (normalized) recentPlates.set(normalized, now);
}

export function pruneExpiredPlateCooldowns(recentPlates: Map<string, number>, now: number, cooldownSeconds = SAME_PLATE_COOLDOWN_SECONDS): void {
  for (const [plate, lockedAt] of recentPlates) {
    if (now - lockedAt >= cooldownSeconds * 1000) recentPlates.delete(plate);
  }
}
