import { useState } from "react";
import { Link } from "react-router-dom";
import { createReservation, type Reservation } from "../api/reservationApi";
import { Alert } from "../components/ui/Alert";
import { CopyButton } from "../components/ui/CopyButton";
import { PageHeader } from "../components/ui/PageHeader";
import { StatusBadge } from "../components/ui/StatusBadge";
import { formatDateTime } from "../utils/formatters";

export function ReservationNewPage() {
  const [form, setForm] = useState({ vehiclePlate: "", vehicleType: "MOTORBIKE", reservedFrom: "", reservedUntil: "", customerName: "", customerPhone: "" });
  const [result, setResult] = useState<Reservation | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const set = (key: keyof typeof form, value: string) => setForm({ ...form, [key]: value });
  async function submit(event: React.FormEvent) {
    event.preventDefault(); setError(""); setBusy(true);
    try { setResult(await createReservation({ ...form, reservedFrom: new Date(form.reservedFrom).toISOString(), reservedUntil: new Date(form.reservedUntil).toISOString() })); }
    catch (submitError) { setError(submitError instanceof Error ? submitError.message : "Không thể tạo đặt chỗ."); }
    finally { setBusy(false); }
  }
  return <main className="reservation-layout">
    <PageHeader eyebrow="Dành cho khách hàng" title="Đặt chỗ gửi xe">Tạo mã đặt chỗ trước khi đến trung tâm thương mại.</PageHeader>
    <div className="reservation-grid">
      <form className="reservation-form-card" onSubmit={event => void submit(event)}>
        <div className="section-title"><div><h2>Thông tin xe và thời gian</h2><p>Nhập đúng biển số để nhân viên xác nhận tại cổng vào.</p></div></div>
        <div className="reservation-form-fields"><label>Biển số xe<input required value={form.vehiclePlate} onChange={event => set("vehiclePlate", event.target.value)} placeholder="Ví dụ: 59A1-12345" /></label><label>Loại xe<select value={form.vehicleType} onChange={event => set("vehicleType", event.target.value)}><option value="MOTORBIKE">Xe máy</option><option value="CAR">Ô tô</option></select></label><label>Thời gian bắt đầu<input required type="datetime-local" value={form.reservedFrom} onChange={event => set("reservedFrom", event.target.value)} /></label><label>Thời gian kết thúc<input required type="datetime-local" value={form.reservedUntil} onChange={event => set("reservedUntil", event.target.value)} /></label><label>Họ tên <span>(tùy chọn)</span><input value={form.customerName} onChange={event => set("customerName", event.target.value)} /></label><label>Số điện thoại <span>(tùy chọn)</span><input inputMode="tel" value={form.customerPhone} onChange={event => set("customerPhone", event.target.value)} /></label></div>
        <button className="primary-button" disabled={busy}>{busy ? "Đang tạo mã…" : "Tạo đặt chỗ"}</button>{error && <Alert tone="danger">{error}</Alert>}
      </form>
      <aside className="reservation-guidance"><h2>Lưu ý cho bản demo</h2><ul><li>Không yêu cầu thanh toán hoặc đặt cọc khi tạo đặt chỗ.</li><li>Nhân viên nhập mã đặt chỗ tại cổng vào để tạo phiên gửi xe thông thường.</li><li>Mã đã hết hạn hoặc đã hủy sẽ không thể dùng để check-in.</li></ul></aside>
    </div>
    {result && <section className="reservation-success" aria-live="polite"><div><span className="ticket-code">Đặt chỗ đã tạo</span><h2>{result.reservationCode}</h2><p>Đưa mã này cho nhân viên khi check-in.</p></div><StatusBadge status={result.status} /><dl><div><dt>Biển số</dt><dd>{result.vehiclePlate}</dd></div><div><dt>Khung giờ</dt><dd>{formatDateTime(result.reservedFrom)} – {formatDateTime(result.reservedUntil)}</dd></div></dl><div className="reservation-success-actions"><CopyButton value={result.reservationCode} label="Sao chép mã" /><Link className="primary-button" to={`/reservations/${result.reservationCode}`}>Xem đặt chỗ</Link></div></section>}
  </main>;
}
