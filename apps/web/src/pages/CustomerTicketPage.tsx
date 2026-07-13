import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { generateExitPass, type ExitPass } from "../api/exitPassApi";
import { createPaymentOrder, simulatePayment, type PaymentOrder } from "../api/paymentApi";
import { getPublicTicket, TicketApiError } from "../api/ticketApi";
import { Alert } from "../components/ui/Alert";
import { CopyButton } from "../components/ui/CopyButton";
import { StatusBadge } from "../components/ui/StatusBadge";
import type { PublicTicket } from "../types/ticket";
import { formatCurrencyVnd, formatDateTime, formatStatusLabel } from "../utils/formatters";

type TicketState = "loading" | "success" | "invalid" | "error";

export function CustomerTicketPage() {
  const { lookupToken } = useParams();
  const [ticket, setTicket] = useState<PublicTicket | null>(null);
  const [state, setState] = useState<TicketState>(lookupToken ? "loading" : "invalid");
  const [retryKey, setRetryKey] = useState(0);
  const [order, setOrder] = useState<PaymentOrder | null>(null);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [paymentBusy, setPaymentBusy] = useState(false);
  const [exitPass, setExitPass] = useState<ExitPass | null>(null);
  const [exitPassError, setExitPassError] = useState<string | null>(null);
  const [exitPassBusy, setExitPassBusy] = useState(false);

  useEffect(() => {
    let isCurrent = true;
    if (!lookupToken?.trim()) { setTicket(null); setState("invalid"); return () => { isCurrent = false; }; }
    setTicket(null); setState("loading");
    getPublicTicket(lookupToken).then(loadedTicket => { if (isCurrent) { setTicket(loadedTicket); setState("success"); } }).catch((error: unknown) => { if (isCurrent) setState(error instanceof TicketApiError && error.status === 404 ? "invalid" : "error"); });
    return () => { isCurrent = false; };
  }, [lookupToken, retryKey]);

  return <main className="ticket-layout ticket-layout--customer" aria-live="polite">
    {state === "loading" && <LoadingTicket />}
    {state === "invalid" && <InvalidTicket />}
    {state === "error" && <ErrorTicket onRetry={() => setRetryKey(key => key + 1)} />}
    {state === "success" && ticket && <TicketSummary ticket={ticket} lookupToken={lookupToken!} order={order} paymentError={paymentError} paymentBusy={paymentBusy} exitPass={exitPass} exitPassError={exitPassError} exitPassBusy={exitPassBusy}
      onCreate={async () => { setPaymentBusy(true); setPaymentError(null); try { setOrder(await createPaymentOrder(ticket, lookupToken!)); } catch { setPaymentError("Không thể tạo lệnh thanh toán. Vui lòng thử lại."); } finally { setPaymentBusy(false); } }}
      onSimulate={async () => { if (!order) return; setPaymentBusy(true); setPaymentError(null); try { await simulatePayment(order); setRetryKey(key => key + 1); } catch { setPaymentError("Thanh toán mô phỏng chưa thành công. Vui lòng kiểm tra lại lệnh thanh toán."); } finally { setPaymentBusy(false); } }}
      onGenerateExitPass={async () => { setExitPassBusy(true); setExitPassError(null); try { setExitPass(await generateExitPass(ticket.sessionId, lookupToken!)); } catch { setExitPassError("Không thể tạo Exit Pass. Vui lòng liên hệ nhân viên bãi xe."); } finally { setExitPassBusy(false); } }} />}
  </main>;
}

function LoadingTicket() { return <section className="state-panel" aria-busy="true"><span className="ticket-code">Vé khách hàng</span><h1>Đang tải vé gửi xe…</h1><p>Đang lấy thông tin phiên gửi xe an toàn.</p><div className="skeleton-sheet" aria-hidden="true"><div className="skeleton-line skeleton-line--short" /><div className="skeleton-line" /><div className="skeleton-line skeleton-line--medium" /><div className="skeleton-line" /></div></section>; }
function InvalidTicket() { return <section className="state-panel state-panel--error"><span className="ticket-code">Vé khách hàng</span><h1>Không tìm thấy vé</h1><p>Liên kết vé không hợp lệ hoặc không còn hiệu lực. Hãy quét lại QR tại bãi xe.</p></section>; }
function ErrorTicket({ onRetry }: { onRetry: () => void }) { return <section className="state-panel state-panel--error"><span className="ticket-code">Kết nối tạm thời gián đoạn</span><h1>Không thể tải vé</h1><p>Vui lòng thử lại hoặc liên hệ nhân viên bãi xe.</p><button className="primary-button" type="button" onClick={onRetry}>Thử lại</button></section>; }

function TicketSummary({ ticket, lookupToken, order, paymentError, paymentBusy, exitPass, exitPassError, exitPassBusy, onCreate, onSimulate, onGenerateExitPass }: { ticket: PublicTicket; lookupToken: string; order: PaymentOrder | null; paymentError: string | null; paymentBusy: boolean; exitPass: ExitPass | null; exitPassError: string | null; exitPassBusy: boolean; onCreate: () => Promise<void>; onSimulate: () => Promise<void>; onGenerateExitPass: () => Promise<void> }) {
  return <>
    <section className="customer-ticket-head"><div><span className="ticket-code">Vé khách hàng · {ticket.sessionCode}</span><h1>{ticket.vehiclePlate}</h1><p>{ticket.vehicleType === "MOTORBIKE" ? "Xe máy" : ticket.vehicleType} · Vào lúc {formatDateTime(ticket.entryTime)}</p></div><div className="ticket-status-stack"><StatusBadge status={ticket.status} /><StatusBadge status={ticket.paymentStatus} /></div></section>
    <article className="ticket-sheet customer-ticket-sheet" aria-label="Thông tin vé gửi xe">
      <section className="ticket-section ticket-hero-details"><div><span>Phí cần thanh toán</span><strong>{formatCurrencyVnd(ticket.finalFee)}</strong><small>{ticket.discountAmount > 0 ? `Đã giảm ${formatCurrencyVnd(ticket.discountAmount)}` : "Chưa có ưu đãi"}</small></div><dl><div><dt>Thời gian gửi</dt><dd>{ticket.durationMinutes} phút</dd></div><div><dt>Trạng thái thanh toán</dt><dd><StatusBadge status={ticket.paymentStatus} /></dd></div></dl></section>
      <section className="ticket-section"><h2 className="section-heading">Thông tin phiên gửi xe</h2><dl className="details-grid"><div><dt>Mã phiên</dt><dd>{ticket.sessionCode}</dd></div><div><dt>Loại xe</dt><dd>{formatStatusLabel(ticket.vehicleType)}</dd></div><div><dt>Giờ vào</dt><dd>{formatDateTime(ticket.entryTime)}</dd></div><div><dt>Trạng thái bãi xe</dt><dd><StatusBadge status={ticket.status} /></dd></div></dl></section>
      <section className="ticket-section"><h2 className="section-heading">Thanh toán và ưu đãi</h2><dl className="fee-list"><div><dt>Phí ước tính</dt><dd>{formatCurrencyVnd(ticket.estimatedFee)}</dd></div><div><dt>Tổng hóa đơn cửa hàng hợp lệ</dt><dd>{formatCurrencyVnd(ticket.totalEligibleInvoiceAmount)}</dd></div><div><dt>Giảm giá</dt><dd>{formatCurrencyVnd(ticket.discountAmount)}</dd></div><div className="fee-total"><dt>Phí cuối cùng</dt><dd>{formatCurrencyVnd(ticket.finalFee)}</dd></div></dl>{ticket.merchantDiscountMessage && <Alert tone="success">{ticket.merchantDiscountMessage}</Alert>}</section>
      <section className="ticket-section"><Alert tone="warning"><strong>QR này chỉ dùng để xem vé và thanh toán.</strong> QR này không phải Exit Pass và không thể tự cấp quyền ra cổng.</Alert></section>
      {ticket.paymentStatus !== "PAID" && <section className="ticket-section payment-panel"><div className="section-heading-block"><h2>Thanh toán mô phỏng</h2><p>Đây là môi trường demo; không có giao dịch tiền thật.</p></div>{!order ? <button className="primary-button" disabled={paymentBusy} onClick={onCreate}>{paymentBusy ? "Đang tạo lệnh…" : "Tạo lệnh thanh toán"}</button> : <div className="payment-order-card"><div><span>Mã thanh toán</span><strong>{order.paymentCode}</strong></div><div><span>Số tiền</span><strong>{formatCurrencyVnd(order.amount)}</strong></div><StatusBadge status={order.status} /><button className="primary-button" disabled={paymentBusy} onClick={onSimulate}>{paymentBusy ? "Đang xử lý…" : "Mô phỏng thanh toán thành công"}</button></div>}{paymentError && <Alert tone="danger">{paymentError}</Alert>}</section>}
      {ticket.canGenerateExitPass && <section className="ticket-section exit-pass-panel"><div className="section-heading-block"><h2>Exit Pass</h2><p>Chỉ tạo sau khi thanh toán hợp lệ hoặc phí bằng 0.</p></div>{!exitPass ? <><Alert tone="warning">{ticket.exitPassMessage} Exit Pass có thời hạn ngắn và chỉ dùng một lần.</Alert><button className="primary-button" type="button" disabled={exitPassBusy} onClick={onGenerateExitPass}>{exitPassBusy ? "Đang tạo Exit Pass…" : "Tạo Exit Pass"}</button></> : <div className="exit-pass"><StatusBadge status={exitPass.status} /><p><strong>Exit Pass token</strong><code>{exitPass.exitPassToken}</code><CopyButton value={exitPass.exitPassToken} label="Sao chép token" /></p><p><strong>Hết hạn lúc:</strong> {formatDateTime(exitPass.expiresAt)} · TTL {exitPass.ttlSeconds} giây</p><Alert tone="warning">Exit Pass này khác QR Lookup, có thời hạn ngắn và chỉ được sử dụng một lần.</Alert></div>}{exitPassError && <Alert tone="danger">{exitPassError}</Alert>}</section>}
      {ticket.status === "EXITED" && <section className="ticket-section"><Alert tone="success">Xe đã hoàn tất check-out. Không thể tạo Exit Pass mới.</Alert></section>}
      <section className="ticket-section ticket-token-footer"><span>Lookup token</span><code>{lookupToken}</code><CopyButton value={lookupToken} label="Sao chép mã vé" /></section>
    </article>
  </>;
}
