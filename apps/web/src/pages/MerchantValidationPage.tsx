import { useState } from "react";
import { validateInvoice, type MerchantResult } from "../api/merchantApi";
import { Alert } from "../components/ui/Alert";
import { PageHeader } from "../components/ui/PageHeader";
import { StatusBadge } from "../components/ui/StatusBadge";
import { formatCurrencyVnd } from "../utils/formatters";

export function MerchantValidationPage() {
  const [token, setToken] = useState(localStorage.getItem("parkflow.merchant.token") ?? "");
  const [lookupToken, setLookupToken] = useState("");
  const [invoiceCode, setInvoiceCode] = useState("");
  const [invoiceAmount, setInvoiceAmount] = useState("");
  const [result, setResult] = useState<MerchantResult | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit() { setError(""); setBusy(true); try { setResult(await validateInvoice(token, lookupToken, invoiceCode, Number(invoiceAmount))); } catch (submitError) { const message = submitError instanceof Error ? submitError.message : "Xác thực hóa đơn thất bại."; setError(message.toLowerCase().includes("duplicate") ? "Mã hóa đơn đã được sử dụng." : message); } finally { setBusy(false); } }
  return <main className="merchant-layout">
    <PageHeader eyebrow="Merchant counter" title="Xác thực hóa đơn cửa hàng">Nhập QR ticket và hóa đơn để cộng dồn ưu đãi gửi xe.</PageHeader>
    <div className="merchant-grid"><section className="merchant-form-card"><div className="merchant-access"><strong>Quyền truy cập demo</strong><p>Chỉ MERCHANT_STAFF hoặc ADMIN. Xác thực chính thức yêu cầu kết nối online.</p><label>JWT merchant hoặc quản trị viên<textarea rows={2} value={token} onChange={event => { setToken(event.target.value); localStorage.setItem("parkflow.merchant.token", event.target.value); }} placeholder="Dán MERCHANT_STAFF hoặc ADMIN JWT" /></label></div><div className="section-title"><div><h2>Hóa đơn cần xác thực</h2><p>Mỗi mã hóa đơn chỉ được dùng một lần.</p></div></div><div className="merchant-fields"><label>QR Lookup Token<input value={lookupToken} onChange={event => setLookupToken(event.target.value)} placeholder="Dán mã từ vé khách hàng" /></label><label>Mã hóa đơn<input value={invoiceCode} onChange={event => setInvoiceCode(event.target.value)} placeholder="Ví dụ: INV-2026-001" /></label><label>Số tiền hóa đơn (VND)<input type="number" min="1" value={invoiceAmount} onChange={event => setInvoiceAmount(event.target.value)} placeholder="300000" /></label></div><button className="primary-button" disabled={busy} onClick={() => void submit()}>{busy ? "Đang xác thực…" : "Xác thực hóa đơn"}</button>{error && <Alert tone="danger">{error}</Alert>}</section><aside className="merchant-policy"><h2>Chính sách demo</h2><dl><div><dt>Chính sách</dt><dd>AGGREGATE_INVOICE</dd></div><div><dt>Ngưỡng cộng dồn</dt><dd>{formatCurrencyVnd(300000)}</dd></div><div><dt>Giảm giá demo</dt><dd>{formatCurrencyVnd(5000)}</dd></div></dl><p>Giảm giá không vượt quá phí gửi xe. Không có xác thực merchant chính thức khi offline.</p></aside></div>
    {result && <section className="merchant-result" aria-live="polite"><div className="merchant-result-head"><div><span className="ticket-code">Kết quả xác thực</span><h2>{result.sessionCode}</h2><p>{result.message}</p></div><StatusBadge status={result.status} /></div><dl className="merchant-result-grid"><div><dt>Mã validation</dt><dd><code>{result.validationId}</code></dd></div><div><dt>Hóa đơn vừa nhập</dt><dd>{invoiceCode}</dd></div><div><dt>Giá trị hóa đơn</dt><dd>{formatCurrencyVnd(result.invoiceAmount)}</dd></div><div><dt>Tổng hóa đơn hợp lệ</dt><dd>{formatCurrencyVnd(result.totalEligibleInvoiceAmount)}</dd></div><div><dt>Giảm giá</dt><dd>{formatCurrencyVnd(result.discountAmount)}</dd></div><div><dt>Phí cuối cùng</dt><dd>{formatCurrencyVnd(result.finalFee)}</dd></div></dl></section>}
  </main>;
}
