import { useCallback, useEffect, useState } from "react";
import { syncOfflineEvents } from "../api/offlineSyncApi";
import { getDeviceId } from "../offline/deviceId";
import { addOfflineCheckIn, loadOfflineQueue, setQueueStatus, updateOfflineEvent } from "../offline/offlineQueue";
import type { OfflineCheckInEvent } from "../offline/offlineTypes";

const TOKEN_KEY = "parkflow.offline.staff-token";

export function StaffOfflinePage() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? "");
  const [deviceId] = useState(getDeviceId);
  const [online, setOnline] = useState(navigator.onLine);
  const [syncing, setSyncing] = useState(false);
  const [lastSync, setLastSync] = useState<string | null>(null);
  const [queue, setQueue] = useState<OfflineCheckInEvent[]>(loadOfflineQueue);
  const [vehiclePlate, setVehiclePlate] = useState("");
  const [vehicleType, setVehicleType] = useState<"MOTORBIKE" | "CAR">("MOTORBIKE");
  const [entryGate, setEntryGate] = useState("GATE_IN_01");
  const [notice, setNotice] = useState<string | null>(null);

  const refreshQueue = useCallback(() => setQueue(loadOfflineQueue()), []);
  const syncNow = useCallback(async () => {
    const events = loadOfflineQueue().filter((event) => event.syncStatus === "PENDING");
    if (!events.length || !token.trim() || !navigator.onLine) return;
    setSyncing(true); setQueue(setQueueStatus(events.map((event) => event.eventId), "SYNCING"));
    try {
      const results = await syncOfflineEvents(token.trim(), deviceId, events);
      results.forEach((result) => updateOfflineEvent(result.eventId, { syncStatus: result.status, message: result.message, serverSessionId: result.serverSessionId, sessionCode: result.sessionCode }));
      setLastSync(new Date().toISOString()); setNotice("Offline queue synchronized with the server."); refreshQueue();
    } catch {
      setQueue(setQueueStatus(events.map((event) => event.eventId), "PENDING", "Sync failed. Retry when online."));
      setNotice("Sync failed. The events remain in this device queue.");
    } finally { setSyncing(false); }
  }, [deviceId, refreshQueue, token]);

  useEffect(() => {
    const handleOnline = () => { setOnline(true); void syncNow(); };
    const handleOffline = () => setOnline(false);
    window.addEventListener("online", handleOnline); window.addEventListener("offline", handleOffline);
    return () => { window.removeEventListener("online", handleOnline); window.removeEventListener("offline", handleOffline); };
  }, [syncNow]);

  function saveToken(value: string) { setToken(value); localStorage.setItem(TOKEN_KEY, value); }
  function addEvent() {
    if (!vehiclePlate.trim() || !entryGate.trim()) { setNotice("Vehicle plate and entry gate are required."); return; }
    setQueue(addOfflineCheckIn({ vehiclePlate: vehiclePlate.trim(), vehicleType, entryGate: entryGate.trim(), plateSource: "MANUAL" }));
    setVehiclePlate(""); setNotice("Offline check-in event saved locally. It is not yet an official parking session.");
  }
  const connectionLabel = syncing ? "Syncing" : online ? "Online" : "Offline";

  return <main className="offline-layout">
    <section className="ticket-intro"><span className="ticket-code">Staff demo tool</span><h1>Offline Staff Console</h1><p>Create local check-in events during a network outage, then synchronize them to the authoritative server.</p></section>
    <section className="offline-card"><div className={`connection connection--${connectionLabel.toLowerCase()}`}><strong>{connectionLabel}</strong><span>{lastSync ? `Last sync: ${new Date(lastSync).toLocaleString()}` : "No sync completed yet."}</span></div><p className="offline-note">Device ID: <code>{deviceId}</code></p><label>Staff JWT<textarea value={token} onChange={(event) => saveToken(event.target.value)} placeholder="Paste Parking Staff or Admin JWT" rows={3} /></label></section>
    <section className="offline-card"><h2>Create offline check-in</h2><div className="offline-form"><label>Vehicle plate<input value={vehiclePlate} onChange={(event) => setVehiclePlate(event.target.value)} /></label><label>Vehicle type<select value={vehicleType} onChange={(event) => setVehicleType(event.target.value as "MOTORBIKE" | "CAR")}><option value="MOTORBIKE">Motorbike</option><option value="CAR">Car</option></select></label><label>Entry gate<input value={entryGate} onChange={(event) => setEntryGate(event.target.value)} /></label></div><button className="primary-button" type="button" onClick={addEvent}>Add Offline Check-in Event</button></section>
    <section className="offline-card"><div className="offline-card-heading"><h2>Local queue</h2><button className="primary-button" type="button" disabled={syncing || !online || !token.trim()} onClick={() => void syncNow()}>{syncing ? "Syncing..." : "Sync Now"}</button></div>{notice && <p className="offline-note">{notice}</p>}<p className="offline-note">Rejected and conflicted events remain visible here. The server result is authoritative.</p><div className="offline-events">{queue.length === 0 ? <p className="offline-note">No local offline events.</p> : queue.map((event) => <article key={event.eventId} className="offline-event"><div><strong>{event.eventType}</strong><span className={`status status--${event.syncStatus.toLowerCase()}`}>{event.syncStatus}</span></div><p>{event.payload.vehiclePlate} · {event.payload.vehicleType} · {event.payload.entryGate}</p><p><code>{event.eventId}</code></p><p>{new Date(event.localTimestamp).toLocaleString()}</p>{event.message && <p>{event.message}</p>}{event.serverSessionId && <p>Server session: <code>{event.serverSessionId}</code>{event.sessionCode ? ` (${event.sessionCode})` : ""}</p>}</article>)}</div></section>
  </main>;
}
