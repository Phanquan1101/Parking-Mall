import { QRCodeSVG } from "qrcode.react";

type TicketLookupQrProps = {
  ticketUrl: string;
  sessionCode: string;
};

export function TicketLookupQr({ ticketUrl, sessionCode }: TicketLookupQrProps) {
  return <section className="ticket-qr" aria-label="Mã QR vé khách hàng">
    <div className="ticket-qr-copy">
      <span>QR vé khách hàng</span>
      <strong>{sessionCode}</strong>
      <p>Quét mã này để mở vé và thanh toán. Đây không phải Exit Pass.</p>
    </div>
    <div className="ticket-qr-code">
      <QRCodeSVG value={ticketUrl} size={184} level="M" marginSize={4} title={`QR vé ${sessionCode}`} />
    </div>
  </section>;
}
