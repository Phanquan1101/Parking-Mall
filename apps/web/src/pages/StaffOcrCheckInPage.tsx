import { useState } from "react";
import { ocrAssistedCheckIn, recognizePlate, type OcrResult } from "../api/visionApi";
import { Alert } from "../components/ui/Alert";
import { PageHeader } from "../components/ui/PageHeader";
import { StatusBadge } from "../components/ui/StatusBadge";
import { formatPercent } from "../utils/formatters";

const KEY = "parkflow.ocr.staff-token";

export function StaffOcrCheckInPage() {
  const [token, setToken] = useState(() => localStorage.getItem(KEY) ?? "");
  const [file, setFile] = useState<File | null>(null);
  const [ocr, setOcr] = useState<OcrResult | null>(null);
  const [plate, setPlate] = useState("");
  const [vehicleType, setVehicleType] = useState("MOTORBIKE");
  const [entryGate, setEntryGate] = useState("GATE_IN_01");
  const [reservationCode, setReservationCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [session, setSession] = useState<{ sessionId: string; sessionCode: string; qrLookupToken: string; ticketUrl: string } | null>(null);

  async function scan() {
    if (!file || !token.trim()) { setError("Dán JWT nhân viên và chọn ảnh trước khi quét OCR."); return; }
    setBusy(true); setError("");
    try { const result = await recognizePlate(token.trim(), file); setOcr(result); setPlate(result.candidatePlate ?? ""); }
    catch (scanError) { setError(scanError instanceof Error ? scanError.message : "OCR không thành công. Bạn vẫn có thể nhập biển số thủ công."); }
    finally { setBusy(false); }
  }

  async function checkIn() {
    if (!token.trim() || !plate.trim()) { setError("JWT nhân viên và biển số đã xác nhận là bắt buộc."); return; }
    setBusy(true); setError("");
    try {
      setSession(await ocrAssistedCheckIn(token.trim(), { vehiclePlate: plate.trim(), vehicleType, entryGate, plateSource: ocr ? "OCR_ASSISTED" : "MANUAL", ocrRequestId: ocr?.ocrRequestId, ocrCandidatePlate: ocr?.candidatePlate, ocrConfidence: ocr?.confidence, reservationCode: reservationCode.trim() || undefined }));
    } catch (checkInError) { setError(checkInError instanceof Error ? checkInError.message : "Check-in không thành công."); }
    finally { setBusy(false); }
  }

  return <main className="offline-layout ocr-page">
    <PageHeader eyebrow="Công cụ nhân viên" title="OCR biển số từ ảnh">Tải ảnh để lấy gợi ý biển số; nhân viên vẫn phải xác nhận trước khi tạo phiên gửi xe.</PageHeader>
    <section className="ocr-workspace">
      <section className="offline-card ocr-upload-card">
        <div className="section-title"><div><h2>Tải ảnh biển số</h2><p>Hỗ trợ JPEG, PNG và WebP. Ảnh chỉ được dùng cho yêu cầu OCR hiện tại.</p></div><StatusBadge status="OCR ASSIST" /></div>
        <label>JWT nhân viên<textarea rows={2} value={token} onChange={event => { setToken(event.target.value); localStorage.setItem(KEY, event.target.value); }} placeholder="Dán PARKING_STAFF hoặc ADMIN JWT" /></label>
        <label className="file-dropzone"><input accept="image/jpeg,image/png,image/webp" type="file" onChange={event => setFile(event.target.files?.[0] ?? null)} /><span className="file-dropzone-icon" aria-hidden="true">↑</span><strong>{file ? file.name : "Chọn ảnh biển số"}</strong><small>{file ? `${Math.ceil(file.size / 1024)} KB · sẵn sàng quét` : "JPEG, PNG hoặc WebP"}</small></label>
        <button className="primary-button" disabled={busy || !file} onClick={() => void scan()}>{busy ? "Đang nhận diện…" : "Chạy OCR hỗ trợ"}</button>
      </section>

      <section className="offline-card ocr-result-card" aria-live="polite">
        <div className="section-title"><div><h2>Kết quả nhận diện</h2><p>OCR chỉ cung cấp gợi ý, không tự tạo check-in.</p></div>{ocr && <StatusBadge status={ocr.provider} />}</div>
        {!ocr ? <div className="empty-state"><strong>Chưa có kết quả OCR</strong><p>Chọn ảnh và chạy OCR để xem biển số gợi ý, nhà cung cấp và độ tin cậy.</p></div> : <>
          <div className="ocr-result-plate"><span>Biển số gợi ý</span><strong>{ocr.candidatePlate ?? "Không chắc chắn"}</strong><small>Chuẩn hóa: {ocr.normalizedCandidatePlate ?? "—"}</small></div>
          <div className="info-grid"><div><span>Nhà cung cấp</span><strong>{ocr.provider}</strong></div><div><span>Độ tin cậy</span><strong>{formatPercent(ocr.confidence)}</strong></div><div><span>Mã yêu cầu</span><code>{ocr.ocrRequestId}</code></div></div>
          <Alert tone={ocr.candidatePlate ? "warning" : "danger"}>{ocr.warnings.length ? ocr.warnings.join(" ") : "Nhân viên phải xác nhận biển số trước khi check-in."}</Alert>
        </>}
      </section>
    </section>

    <section className="offline-card confirm-checkin-card">
      <div className="section-title"><div><h2>Xác nhận và check-in</h2><p>Biển số xác nhận là nguồn dữ liệu chính. {ocr ? "Check-in sẽ dùng nguồn OCR_ASSISTED." : "Không có kết quả OCR sẽ dùng nguồn MANUAL."}</p></div>{ocr && <StatusBadge status="STAFF CONFIRMATION" />}</div>
      <div className="offline-form"><label className="confirmed-plate-field">Biển số đã xác nhận<input value={plate} onChange={event => setPlate(event.target.value)} placeholder="Nhập hoặc chỉnh sửa biển số" /></label><label>Loại xe<select value={vehicleType} onChange={event => setVehicleType(event.target.value)}><option value="MOTORBIKE">Xe máy</option><option value="CAR">Ô tô</option></select></label><label>Cổng vào<input value={entryGate} onChange={event => setEntryGate(event.target.value)} /></label><label>Mã đặt chỗ <span>(tùy chọn)</span><input value={reservationCode} onChange={event => setReservationCode(event.target.value)} /></label></div>
      <button className="primary-button" disabled={busy} onClick={() => void checkIn()}>{busy ? "Đang check-in…" : "Xác nhận check-in"}</button>
      {error && <Alert tone="danger">{error}</Alert>}
      {session && <div className="checkin-success"><div><StatusBadge status="CHECK-IN SUCCESS" /><h3>{session.sessionCode}</h3><p>Đã tạo phiên gửi xe cho vé QR khách hàng.</p></div><dl><div><dt>Session ID</dt><dd><code>{session.sessionId}</code></dd></div><div><dt>Ticket URL</dt><dd><code>{session.ticketUrl}</code></dd></div></dl></div>}
    </section>
  </main>;
}
