import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { cancelReservation, getReservation, type Reservation } from "../api/reservationApi";
import { Alert } from "../components/ui/Alert";
import { CopyButton } from "../components/ui/CopyButton";
import { StatusBadge } from "../components/ui/StatusBadge";
import { formatDateTime } from "../utils/formatters";

const statusDescription: Record<Reservation["status"], string> = { RESERVED: "Còn hiệu lực", CANCELLED: "Đã hủy", EXPIRED: "Đã hết hạn", CONSUMED: "Đã dùng để check-in" };

export function ReservationPage() {
  const { reservationCode = "" } = useParams();
  const [data, setData] = useState<Reservation | null>(null);
  const [error, setError] = useState("");
  const [reason, setReason] = useState("Khách hàng hủy đặt chỗ.");
  const [busy, setBusy] = useState(false);
  useEffect(() => { void getReservation(reservationCode).then(setData).catch(loadError => setError(loadError instanceof Error ? loadError.message : "Không tìm thấy đặt chỗ.")); }, [reservationCode]);
  async function cancel() { setBusy(true); setError(""); try { setData(await cancelReservation(reservationCode, reason.trim() || "Khách hàng hủy đặt chỗ.")); } catch (cancelError) { setError(cancelError instanceof Error ? cancelError.message : "Không thể hủy đặt chỗ."); } finally { setBusy(false); } }
  if (error && !data) return <main className="ticket-layout"><section className="state-panel state-panel--error"><h1>Không thể mở đặt chỗ</h1><p>{error}</p></section></main>;
  if (!data) return <main className="ticket-layout"><section className="state-panel"><h1>Đang tải đặt chỗ…</h1></section></main>;
  return <main className="reservation-detail-layout"><section className="reservation-detail-head"><div><span className="ticket-code">Mã đặt chỗ</span><h1>{data.reservationCode}</h1><p>{statusDescription[data.status]}</p></div><StatusBadge status={data.status} /></section><article className="reservation-detail-card"><section className="reservation-code-banner"><div><span>Mã dùng tại cổng vào</span><strong>{data.reservationCode}</strong></div><CopyButton value={data.reservationCode} label="Sao chép mã" /></section><dl className="details-grid reservation-details"><div><dt>Biển số xe</dt><dd>{data.vehiclePlate}</dd></div><div><dt>Loại xe</dt><dd>{data.vehicleType === "MOTORBIKE" ? "Xe máy" : "Ô tô"}</dd></div><div><dt>Từ thời gian</dt><dd>{formatDateTime(data.reservedFrom)}</dd></div><div><dt>Đến thời gian</dt><dd>{formatDateTime(data.reservedUntil)}</dd></div><div><dt>Trạng thái</dt><dd><StatusBadge status={data.status} /></dd></div></dl><Alert tone="info">Nhân viên sẽ nhập mã này tại cổng vào để tạo phiên gửi xe. Khách hàng không tự tạo check-in từ trang này.</Alert>{data.status === "RESERVED" && <section className="reservation-cancel-section"><div><h2>Hủy đặt chỗ</h2><p>Hành động này sẽ làm mã không còn dùng được tại cổng vào.</p></div><label>Lý do hủy<textarea rows={2} value={reason} onChange={event => setReason(event.target.value)} /></label><button className="secondary-button" disabled={busy} onClick={() => void cancel()}>{busy ? "Đang hủy…" : "Hủy đặt chỗ"}</button></section>}{error && <Alert tone="danger">{error}</Alert>}</article></main>;
}
