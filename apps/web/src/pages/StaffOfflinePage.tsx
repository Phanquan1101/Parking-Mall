import { useCallback, useEffect, useMemo, useState } from "react";
import { syncOfflineEvents } from "../api/offlineSyncApi";
import { Alert } from "../components/ui/Alert";
import { EmptyState } from "../components/ui/EmptyState";
import { PageHeader } from "../components/ui/PageHeader";
import { StatCard } from "../components/ui/StatCard";
import { StatusBadge } from "../components/ui/StatusBadge";
import { getDeviceId } from "../offline/deviceId";
import { addOfflineCheckIn, loadOfflineQueue, setQueueStatus, updateOfflineEvent } from "../offline/offlineQueue";
import type { OfflineCheckInEvent, OfflineQueueStatus } from "../offline/offlineTypes";
import { dashIfEmpty, formatDateTime } from "../utils/formatters";

const TOKEN_KEY = "parkflow.offline.staff-token";
const statuses: OfflineQueueStatus[] = ["PENDING", "SYNCED", "CONFLICT", "REJECTED"];

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
  const queueCounts = useMemo(() => Object.fromEntries(statuses.map(status => [status, queue.filter(event => event.syncStatus === status).length])) as Record<OfflineQueueStatus, number>, [queue]);

  const refreshQueue = useCallback(() => setQueue(loadOfflineQueue()), []);
  const syncNow = useCallback(async () => {
    const events = loadOfflineQueue().filter(event => event.syncStatus === "PENDING");
    if (!events.length || !token.trim() || !navigator.onLine) return;
    setSyncing(true); setQueue(setQueueStatus(events.map(event => event.eventId), "SYNCING"));
    try {
      const results = await syncOfflineEvents(token.trim(), deviceId, events);
      results.forEach(result => updateOfflineEvent(result.eventId, { syncStatus: result.status, message: result.message, serverSessionId: result.serverSessionId, sessionCode: result.sessionCode }));
      setLastSync(new Date().toISOString()); setNotice("Đã đồng bộ hàng đợi offline với máy chủ."); refreshQueue();
    } catch {
      setQueue(setQueueStatus(events.map(event => event.eventId), "PENDING", "Đồng bộ thất bại. Hãy thử lại khi có mạng."));
      setNotice("Không thể đồng bộ. Các sự kiện vẫn được giữ trong hàng đợi thiết bị.");
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
    if (!vehiclePlate.trim() || !entryGate.trim()) { setNotice("Biển số xe và cổng vào là bắt buộc."); return; }
    setQueue(addOfflineCheckIn({ vehiclePlate: vehiclePlate.trim(), vehicleType, entryGate: entryGate.trim(), plateSource: "MANUAL" }));
    setVehiclePlate(""); setNotice("Đã lưu check-in offline trên thiết bị. Đây chưa phải là phiên gửi xe chính thức.");
  }
  const connectionLabel = syncing ? "Đang đồng bộ" : online ? "Trực tuyến" : "Mất kết nối";

  return <main className="offline-layout">
    <PageHeader eyebrow="Công cụ nhân viên" title="Chế độ offline">
      Ghi nhận check-in khi mất mạng và đồng bộ khi có kết nối trở lại.
    </PageHeader>
    <section className="offline-overview">
      <div className={`connection connection--${syncing ? "syncing" : online ? "online" : "offline"}`}><div><strong>{connectionLabel}</strong><span>Thiết bị <code>{deviceId}</code></span></div><span>{lastSync ? `Đồng bộ gần nhất: ${formatDateTime(lastSync)}` : "Chưa có lần đồng bộ nào."}</span></div>
      <div className="stat-grid stat-grid--four"><StatCard label="Đang chờ" value={queueCounts.PENDING} hint="Sẵn sàng gửi" tone="warning" /><StatCard label="Đã đồng bộ" value={queueCounts.SYNCED} hint="Máy chủ đã xác nhận" tone="success" /><StatCard label="Xung đột" value={queueCounts.CONFLICT} hint="Cần nhân viên xử lý" tone="danger" /><StatCard label="Bị từ chối" value={queueCounts.REJECTED} hint="Kiểm tra nội dung sự kiện" tone="neutral" /></div>
    </section>

    <section className="offline-card offline-form-card">
      <div className="section-title"><div><h2>Tạo check-in offline</h2><p>Chỉ dùng khi mất kết nối. Sự kiện sẽ được gửi lại khi có mạng.</p></div><StatusBadge status="OFFLINE_CHECK_IN" /></div>
      <label>JWT nhân viên hoặc quản trị viên<textarea value={token} onChange={event => saveToken(event.target.value)} placeholder="Dán PARKING_STAFF hoặc ADMIN JWT" rows={2} /></label>
      <div className="offline-form"><label>Biển số xe<input value={vehiclePlate} onChange={event => setVehiclePlate(event.target.value)} placeholder="Ví dụ: 59A1-12345" /></label><label>Loại xe<select value={vehicleType} onChange={event => setVehicleType(event.target.value as "MOTORBIKE" | "CAR")}><option value="MOTORBIKE">Xe máy</option><option value="CAR">Ô tô</option></select></label><label>Cổng vào<input value={entryGate} onChange={event => setEntryGate(event.target.value)} /></label></div>
      <button className="primary-button" type="button" onClick={addEvent}>Thêm sự kiện offline</button>
    </section>

    <section className="data-section offline-queue-section">
      <div className="data-section-heading"><div><h2>Hàng đợi cục bộ</h2><p>Máy chủ vẫn là nguồn dữ liệu chính sau khi đồng bộ.</p></div><button className="primary-button" type="button" disabled={syncing || !online || !token.trim()} onClick={() => void syncNow()}>{syncing ? "Đang đồng bộ…" : "Đồng bộ ngay"}</button></div>
      {notice && <Alert tone={notice.includes("thất bại") ? "danger" : "info"}>{notice}</Alert>}
      <div className="dashboard-table-wrap"><table className="dashboard-table offline-table"><thead><tr><th>Trạng thái</th><th>Biển số</th><th>Sự kiện</th><th>Thời gian cục bộ</th><th>Phiên máy chủ</th><th>Ghi chú</th></tr></thead><tbody>
        {queue.length === 0 ? <tr><td colSpan={6}><EmptyState title="Hàng đợi đang trống" description="Sự kiện check-in offline sẽ xuất hiện ở đây trước khi đồng bộ." /></td></tr> : queue.map(event => <tr key={event.eventId}><td><StatusBadge status={event.syncStatus} /></td><td><strong>{event.payload.vehiclePlate}</strong><br /><small>{event.payload.vehicleType} · {event.payload.entryGate}</small></td><td><code>{event.eventId}</code><br /><small>{event.eventType}</small></td><td>{formatDateTime(event.localTimestamp)}</td><td>{event.serverSessionId ? <><code>{event.serverSessionId}</code><br /><small>{dashIfEmpty(event.sessionCode)}</small></> : "—"}</td><td className="table-message">{dashIfEmpty(event.message)}</td></tr>)}
      </tbody></table></div>
    </section>
  </main>;
}
