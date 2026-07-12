import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getPublicTicket, TicketApiError } from "../api/ticketApi";
import type { PublicTicket } from "../types/ticket";
import { createPaymentOrder, simulatePayment, type PaymentOrder } from "../api/paymentApi";

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
    <div className="app-shell">
      <header className="app-header">
        <div className="header-content">
          <Link className="brand" to="/">
            <span aria-hidden="true" className="brand-mark">P</span>
            ParkFlow Mall
          </Link>
          <span className="header-label">Parking ticket</span>
        </div>
      </header>
      <main className="ticket-layout" aria-live="polite">
        {state === "loading" && <LoadingTicket />}
        {state === "invalid" && <InvalidTicket />}
        {state === "error" && <ErrorTicket onRetry={() => setRetryKey((key) => key + 1)} />}
        {state === "success" && ticket && <TicketSummary ticket={ticket} lookupToken={lookupToken!} order={order} paymentError={paymentError} paymentBusy={paymentBusy} onCreate={async()=>{setPaymentBusy(true);setPaymentError(null);try{setOrder(await createPaymentOrder(ticket,lookupToken!));}catch{setPaymentError("Unable to create payment order. Please try again.");}finally{setPaymentBusy(false);}}} onSimulate={async()=>{if(!order)return;setPaymentBusy(true);setPaymentError(null);try{await simulatePayment(order);setRetryKey(key=>key+1);}catch{setPaymentError("Unable to simulate payment. Please verify the order and try again.");}finally{setPaymentBusy(false);}}} />}
      </main>
    </div>
  );
}

function LoadingTicket() {
  return (
    <section className="state-panel" aria-busy="true">
      <h1>Loading ticket...</h1>
      <p>Retrieving your parking session securely.</p>
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
      <h1>Ticket not found</h1>
      <p>Ticket not found or no longer valid.</p>
    </section>
  );
}

function ErrorTicket({ onRetry }: { onRetry: () => void }) {
  return (
    <section className="state-panel state-panel--error">
      <h1>Unable to load ticket</h1>
      <p>Unable to load ticket. Please try again or contact parking staff.</p>
      <button className="primary-button" type="button" onClick={onRetry}>Try again</button>
    </section>
  );
}

function TicketSummary({ ticket, order, paymentError, paymentBusy, onCreate, onSimulate }: { ticket: PublicTicket; lookupToken: string; order: PaymentOrder | null; paymentError: string | null; paymentBusy: boolean; onCreate: () => Promise<void>; onSimulate: () => Promise<void> }) {
  return (
    <>
      <section className="ticket-intro">
        <span className="ticket-code">Parking session</span>
        <h1>{ticket.sessionCode}</h1>
        <p>Here is the current summary of your parking session.</p>
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
            <div className="fee-total">
              <dt>Final fee</dt>
              <dd>{currencyFormatter.format(ticket.finalFee)}</dd>
            </div>
          </dl>
        </section>

        <section className="ticket-section" aria-labelledby="security-notice">
          <div className="security-notice">
            <h2 id="security-notice">Security notice</h2>
            <p>QR Lookup is for ticket lookup only. It cannot authorize vehicle exit.</p>
          </div>
        </section>
        {ticket.paymentStatus !== "PAID" && <section className="ticket-section" aria-labelledby="payment-simulation"><h2 className="section-heading" id="payment-simulation">Payment simulation</h2>{!order ? <button className="primary-button" disabled={paymentBusy} onClick={onCreate}>{paymentBusy ? "Creating order..." : "Create Payment Order"}</button> : <div className="payment-demo"><p><strong>Payment code:</strong> {order.paymentCode}</p><p><strong>Amount:</strong> {currencyFormatter.format(order.amount)}</p><p>Simulation Mode only. No real money is used.</p><button className="primary-button" disabled={paymentBusy} onClick={onSimulate}>{paymentBusy ? "Processing..." : "Simulate Payment Success"}</button></div>}{paymentError && <p className="payment-error">{paymentError}</p>}</section>}
        {ticket.paymentStatus === "PAID" && <section className="ticket-section"><p className="payment-complete">Payment completed. Exit Pass will be available in a later slice.</p></section>}
      </article>
    </>
  );
}
