import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getPublicTicket, TicketApiError } from "../api/ticketApi";
import type { PublicTicket } from "../types/ticket";
import { createPaymentOrder, simulatePayment, type PaymentOrder } from "../api/paymentApi";
import { generateExitPass, type ExitPass } from "../api/exitPassApi";

type TicketState = "loading" | "success" | "invalid" | "error";

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

const dateFormatter = new Intl.DateTimeFormat("vi-VN", {
  dateStyle: "medium",
  timeStyle: "short",
});

function formatDate(value: string) {
  const parsedDate = new Date(value);
  return Number.isNaN(parsedDate.getTime()) ? "Unavailable" : dateFormatter.format(parsedDate);
}

function formatStatus(status: string) {
  return status.replaceAll("_", " ");
}

function statusClass(status: string) {
  const normalized = status.toLowerCase();
  return normalized === "active" || normalized === "paid" ? `status status--${normalized}` : "status";
}

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

    if (!lookupToken?.trim()) {
      setTicket(null);
      setState("invalid");
      return () => {
        isCurrent = false;
      };
    }

    setTicket(null);
    setState("loading");
    getPublicTicket(lookupToken)
      .then((loadedTicket) => {
        if (isCurrent) {
          setTicket(loadedTicket);
          setState("success");
        }
      })
      .catch((error: unknown) => {
        if (!isCurrent) {
          return;
        }
        setState(error instanceof TicketApiError && error.status === 404 ? "invalid" : "error");
      });

    return () => {
      isCurrent = false;
    };
  }, [lookupToken, retryKey]);

  return (
    <main className="ticket-layout" aria-live="polite">
        {state === "loading" && <LoadingTicket />}
        {state === "invalid" && <InvalidTicket />}
        {state === "error" && <ErrorTicket onRetry={() => setRetryKey((key) => key + 1)} />}
        {state === "success" && ticket && <TicketSummary ticket={ticket} lookupToken={lookupToken!} order={order} paymentError={paymentError} paymentBusy={paymentBusy} exitPass={exitPass} exitPassError={exitPassError} exitPassBusy={exitPassBusy} onCreate={async()=>{setPaymentBusy(true);setPaymentError(null);try{setOrder(await createPaymentOrder(ticket,lookupToken!));}catch{setPaymentError("Unable to create payment order. Please try again.");}finally{setPaymentBusy(false);}}} onSimulate={async()=>{if(!order)return;setPaymentBusy(true);setPaymentError(null);try{await simulatePayment(order);setRetryKey(key=>key+1);}catch{setPaymentError("Unable to simulate payment. Please verify the order and try again.");}finally{setPaymentBusy(false);}}} onGenerateExitPass={async()=>{setExitPassBusy(true);setExitPassError(null);try{setExitPass(await generateExitPass(ticket.sessionId,lookupToken!));}catch{setExitPassError("Unable to generate an Exit Pass. Please ask parking staff for help.");}finally{setExitPassBusy(false);}}} />}
    </main>
  );
}

function LoadingTicket() {
  return (
    <section className="state-panel" aria-busy="true">
      <h1>Đang tải vé gửi xe…</h1>
      <p>Đang lấy thông tin phiên gửi xe một cách an toàn.</p>
      <div className="skeleton-sheet" aria-hidden="true">
        <div className="skeleton-line skeleton-line--short" />
        <div className="skeleton-line" />
        <div className="skeleton-line skeleton-line--medium" />
        <div className="skeleton-line" />
      </div>
    </section>
  );
}

function InvalidTicket() {
  return (
    <section className="state-panel state-panel--error">
      <h1>Không tìm thấy vé</h1>
      <p>Liên kết vé không hợp lệ hoặc không còn hiệu lực.</p>
    </section>
  );
}

function ErrorTicket({ onRetry }: { onRetry: () => void }) {
  return (
    <section className="state-panel state-panel--error">
      <h1>Không thể tải vé</h1>
      <p>Vui lòng thử lại hoặc liên hệ nhân viên bãi xe.</p>
      <button className="primary-button" type="button" onClick={onRetry}>Thử lại</button>
    </section>
  );
}

function TicketSummary({ ticket, order, paymentError, paymentBusy, exitPass, exitPassError, exitPassBusy, onCreate, onSimulate, onGenerateExitPass }: { ticket: PublicTicket; lookupToken: string; order: PaymentOrder | null; paymentError: string | null; paymentBusy: boolean; exitPass: ExitPass | null; exitPassError: string | null; exitPassBusy: boolean; onCreate: () => Promise<void>; onSimulate: () => Promise<void>; onGenerateExitPass: () => Promise<void> }) {
  return (
    <>
      <section className="ticket-intro">
        <span className="ticket-code">Vé khách hàng</span>
        <h1>{ticket.sessionCode}</h1>
        <p>Thông tin phiên gửi xe và thanh toán hiện tại của bạn.</p>
      </section>

      <article className="ticket-sheet" aria-label="Parking ticket summary">
        <section className="ticket-section" aria-labelledby="vehicle-information">
          <h2 className="section-heading" id="vehicle-information">Vehicle information</h2>
          <dl className="details-grid">
            <div>
              <dt>Vehicle plate</dt>
              <dd>{ticket.vehiclePlate}</dd>
            </div>
            <div>
              <dt>Vehicle type</dt>
              <dd>{formatStatus(ticket.vehicleType)}</dd>
            </div>
          </dl>
        </section>

        <section className="ticket-section" aria-labelledby="parking-information">
          <h2 className="section-heading" id="parking-information">Parking information</h2>
          <dl className="details-grid">
            <div>
              <dt>Entry time</dt>
              <dd>{formatDate(ticket.entryTime)}</dd>
            </div>
            <div>
              <dt>Duration</dt>
              <dd>{ticket.durationMinutes} minutes</dd>
            </div>
            <div>
              <dt>Session status</dt>
              <dd><span className={statusClass(ticket.status)}>{formatStatus(ticket.status)}</span></dd>
            </div>
            <div>
              <dt>Payment status</dt>
              <dd><span className={statusClass(ticket.paymentStatus)}>{formatStatus(ticket.paymentStatus)}</span></dd>
            </div>
          </dl>
        </section>

        <section className="ticket-section" aria-labelledby="fee-summary">
          <h2 className="section-heading" id="fee-summary">Fee summary</h2>
          <dl className="fee-list">
            <div>
              <dt>Estimated fee</dt>
              <dd>{currencyFormatter.format(ticket.estimatedFee)}</dd>
            </div>
            <div>
              <dt>Discount amount</dt>
              <dd>{currencyFormatter.format(ticket.discountAmount)}</dd>
            </div>
            <div>
              <dt>Eligible merchant invoices</dt>
              <dd>{currencyFormatter.format(ticket.totalEligibleInvoiceAmount)}</dd>
            </div>
            <div className="fee-total">
              <dt>Final fee</dt>
              <dd>{currencyFormatter.format(ticket.finalFee)}</dd>
            </div>
          </dl>
          <p className="payment-complete">{ticket.merchantDiscountMessage}</p>
        </section>

        <section className="ticket-section" aria-labelledby="security-notice">
          <div className="security-notice">
            <h2 id="security-notice">Security notice</h2>
            <p>QR Lookup is for ticket lookup only. It cannot authorize vehicle exit.</p>
          </div>
        </section>
        {ticket.paymentStatus !== "PAID" && <section className="ticket-section" aria-labelledby="payment-simulation"><h2 className="section-heading" id="payment-simulation">Payment simulation</h2>{!order ? <button className="primary-button" disabled={paymentBusy} onClick={onCreate}>{paymentBusy ? "Creating order..." : "Create Payment Order"}</button> : <div className="payment-demo"><p><strong>Payment code:</strong> {order.paymentCode}</p><p><strong>Amount:</strong> {currencyFormatter.format(order.amount)}</p><p>Simulation Mode only. No real money is used.</p><button className="primary-button" disabled={paymentBusy} onClick={onSimulate}>{paymentBusy ? "Processing..." : "Simulate Payment Success"}</button></div>}{paymentError && <p className="payment-error">{paymentError}</p>}</section>}
        {ticket.canGenerateExitPass && <section className="ticket-section" aria-labelledby="exit-pass"><h2 className="section-heading" id="exit-pass">Exit Pass</h2>{!exitPass ? <><p className="payment-complete">{ticket.exitPassMessage}</p><button className="primary-button" type="button" disabled={exitPassBusy} onClick={onGenerateExitPass}>{exitPassBusy ? "Generating Exit Pass..." : "Generate Exit Pass"}</button></> : <div className="exit-pass"><p><strong>Exit Pass token:</strong> <code>{exitPass.exitPassToken}</code></p><p><strong>Expires at:</strong> {formatDate(exitPass.expiresAt)}</p><p><strong>TTL:</strong> {exitPass.ttlSeconds} seconds</p><p>This Exit Pass is short-lived and can be used only once.</p><p>This is not the same as your QR Lookup ticket.</p></div>}{exitPassError && <p className="payment-error">{exitPassError}</p>}</section>}
        {ticket.status === "EXITED" && <section className="ticket-section"><p className="payment-complete">Vehicle has checked out. An Exit Pass can no longer be generated.</p></section>}
      </article>
    </>
  );
}
