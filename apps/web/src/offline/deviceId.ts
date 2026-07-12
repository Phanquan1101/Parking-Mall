const DEVICE_ID_KEY = "parkflow.offline.device-id";

export function getDeviceId(): string {
  const existing = localStorage.getItem(DEVICE_ID_KEY);
  if (existing) return existing;
  const deviceId = `staff-device-${crypto.randomUUID()}`;
  localStorage.setItem(DEVICE_ID_KEY, deviceId);
  return deviceId;
}
