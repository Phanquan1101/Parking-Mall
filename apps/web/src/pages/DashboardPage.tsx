import { useState } from "react";
import { dashboardMetrics, loadParking, loadReconciliation, loadReservations, type ParkingSession, type ReconciliationItem, type Reservation } from "../api/dashboardApi";
import { Alert } from "../components/ui/Alert";
import { EmptyState } from "../components/ui/EmptyState";
import { PageHeader } from "../components/ui/PageHeader";
import { StatCard } from "../components/ui/StatCard";
import { StatusBadge } from "../components/ui/StatusBadge";
import { dashIfEmpty, formatDateTime, formatPercent } from "../utils/formatters";

const KEY = "parkflow.dashboard.token";

export function DashboardPage() {
  const [token, setToken] = useState(() => localStorage.getItem(KEY) ?? "");
  const [parking, setParking] = useState<ParkingSession[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [reconciliation, setReconciliation] = useState<ReconciliationItem[]>([]);
  const [reconciliationNote, setReconciliationNote] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [refreshed, setRefreshed] = useState<string | null>(null);
  const metrics = dashboardMetrics(parking, reservations, reconciliation);

  async function refresh() {
    if (!token.trim()) { setError("Dán JWT ADMIN hoặc PARKING_STAFF để tải bảng điều khiển."); return; }
    setLoading(true); setError(""); setReconciliationNote("");
    const [parkingResult, reservationResult, reconciliationResult] = await Promise.allSettled([
      loadParking(token.trim()), loadReservations(token.trim()), loadReconciliation(token.trim()),
    ]);
    if (parkingResult.status === "fulfilled") setParking(parkingResult.value);
    else setError(parkingResult.reason instanceof Error ? parkingResult.reason.message : "Không tải được phiên gửi xe.");
    if (reservationResult.status === "fulfilled") setReservations(reservationResult.value);
    else setError(previous => previous || (reservationResult.reason instanceof Error ? reservationResult.reason.message : "Không tải được dữ liệu đặt chỗ."));
    if (reconciliationResult.status === "fulfilled") setReconciliation(reconciliationResult.value);
    else {
      const status = (reconciliationResult.reason as { status?: number })?.status;
      setReconciliationNote(status === 403 ? "Chi tiết reconciliation chỉ dành cho ADMIN." : reconciliationResult.reason instanceof Error ? reconciliationResult.reason.message : "Không tải được reconciliation.");
    }
    setRefreshed(new Date().toISOString()); setLoading(false);
  }

  function saveToken(value: string) { setToken(value); localStorage.setItem(KEY, value); }

  return <main className="dashboard-layout">
    <PageHeader eyebrow="Giám sát chỉ đọc" title="Bảng điều khiển vận hành" action={<button className="primary-button" disabled={loading} onClick={() => void refresh()}>{loading ? "Đang tải…" : "Làm mới dữ liệu"}</button>}>
      Theo dõi phiên gửi xe, đặt chỗ và reconciliation từ các API hiện có.
    </PageHeader>

    <section className="auth-panel">
      <div><strong>Quyền truy cập demo</strong><p>ADMIN xem được reconciliation; PARKING_STAFF xem phiên gửi xe và đặt chỗ.</p></div>
      <label>JWT nhân viên hoặc quản trị viên<textarea rows={refreshed ? 2 : 3} value={token} onChange={event => saveToken(event.target.value)} placeholder="Dán PARKING_STAFF hoặc ADMIN JWT" /></label>
      <span className="refresh-meta">{refreshed ? `Cập nhật lúc ${formatDateTime(refreshed)}` : "Chưa tải dữ liệu."}</span>
    </section>
    {error && <Alert tone="danger">{error}</Alert>}

    <section className="stat-grid" aria-label="Chỉ số vận hành">
      <StatCard label="Phiên đang hoạt động" value={metrics.activeSessions} hint={`${metrics.totalSessions} phiên đã tải`} tone="info" />
      <StatCard label="Chưa thanh toán" value={metrics.unpaidActiveSessions} hint="Phiên active" tone="warning" />
      <StatCard label="Đã thanh toán, chưa ra" value={metrics.paidNotExitedSessions} hint="Cần Exit Pass hợp lệ" tone="success" />
      <StatCard label="Đặt chỗ còn hiệu lực" value={metrics.reservedCount} hint={`${metrics.consumedCount} đã sử dụng`} tone="neutral" />
      <StatCard label="Reconciliation cần xử lý" value={reconciliationNote ? "—" : metrics.openReconciliationItems + metrics.pendingManualReviewItems} hint={reconciliationNote || "Open + manual review"} tone="danger" />
    </section>

    <DataSection title="Phiên gửi xe gần đây" meta={`Tổng ${metrics.totalSessions} · OCR ${metrics.ocrAssistedSessions} · Từ đặt chỗ ${metrics.reservationSessions}`}>
      <Table headers={["Mã phiên", "Biển số", "Trạng thái", "Thanh toán", "Vào lúc", "Đặt chỗ", "Nguồn biển", "OCR"]}>
        {parking.length === 0 ? <EmptyRow columns={8} title="Chưa có phiên gửi xe" description="Nhấn Làm mới sau khi tạo check-in từ cổng vào." /> : parking.slice(0, 20).map(session => <tr key={session.sessionId}>
          <td><strong>{session.sessionCode}</strong></td><td>{session.vehiclePlate}</td><td><StatusBadge status={session.status} /></td><td><StatusBadge status={session.paymentStatus} /></td><td>{formatDateTime(session.entryTime)}</td><td>{dashIfEmpty(session.reservationCode)}</td><td><span className="source-label">{dashIfEmpty(session.plateSource)}</span></td><td>{formatPercent(session.ocrConfidence)}</td>
        </tr>)}
      </Table>
    </DataSection>

    <DataSection title="Đặt chỗ" meta={`Reserved ${metrics.reservedCount} · Consumed ${metrics.consumedCount} · Expired ${metrics.expiredCount} · Cancelled ${metrics.cancelledCount}`}>
      <Table headers={["Mã đặt chỗ", "Biển số", "Loại xe", "Từ", "Đến", "Trạng thái"]}>
        {reservations.length === 0 ? <EmptyRow columns={6} title="Chưa có đặt chỗ" description="Danh sách sẽ xuất hiện sau khi khách tạo đặt chỗ." /> : reservations.slice(0, 20).map(reservation => <tr key={reservation.reservationId}>
          <td><strong>{reservation.reservationCode}</strong></td><td>{reservation.vehiclePlate}</td><td>{reservation.vehicleType}</td><td>{formatDateTime(reservation.reservedFrom)}</td><td>{formatDateTime(reservation.reservedUntil)}</td><td><StatusBadge status={reservation.status} /></td>
        </tr>)}
      </Table>
    </DataSection>

    <DataSection title="Payment reconciliation" meta={reconciliationNote || `Open ${metrics.openReconciliationItems} · Resolved ${metrics.resolvedReconciliationItems} · Manual review ${metrics.pendingManualReviewItems}`}>
      {reconciliationNote ? <Alert tone="warning">{reconciliationNote}</Alert> : <Table headers={["Mã item", "Payment order", "Đối tượng", "Vấn đề", "Trạng thái", "Lần thử", "Cập nhật"]}>
        {reconciliation.length === 0 ? <EmptyRow columns={7} title="Không có reconciliation item" description="Các item cần đối soát sẽ hiển thị tại đây cho ADMIN." /> : reconciliation.slice(0, 20).map(item => <tr key={item.id}>
          <td><code>{item.id}</code></td><td><code>{item.paymentOrderId}</code></td><td><code>{item.targetId}</code></td><td><span className="issue-label">{item.issueType}</span></td><td><StatusBadge status={item.status} /></td><td>{dashIfEmpty(item.attemptCount)}</td><td>{formatDateTime(item.updatedAt)}</td>
        </tr>)}
      </Table>}
    </DataSection>
  </main>;
}

function DataSection({ title, meta, children }: { title: string; meta: string; children: React.ReactNode }) {
  return <section className="data-section"><div className="data-section-heading"><div><h2>{title}</h2><p>{meta}</p></div></div>{children}</section>;
}

function Table({ headers, children }: { headers: string[]; children: React.ReactNode }) {
  return <div className="dashboard-table-wrap"><table className="dashboard-table"><thead><tr>{headers.map(header => <th key={header}>{header}</th>)}</tr></thead><tbody>{children}</tbody></table></div>;
}

function EmptyRow({ columns, title, description }: { columns: number; title: string; description: string }) {
  return <tr><td colSpan={columns}><EmptyState title={title} description={description} /></td></tr>;
}
