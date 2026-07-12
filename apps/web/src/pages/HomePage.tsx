import { Link } from "react-router-dom";

export function HomePage() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="header-content">
          <Link className="brand" to="/">
            <span aria-hidden="true" className="brand-mark">P</span>
            ParkFlow Mall
          </Link>
          <span className="header-label">Customer ticket</span>
        </div>
      </header>
      <main className="home-layout">
        <h1>Your parking ticket, at a glance.</h1>
        <p>Open the link from your parking QR ticket to see your session and current fee summary.</p>
        <div className="route-hint">
          Ticket links use the format <code>/tickets/&#123;lookupToken&#125;</code>.
        </div>
        <Link className="primary-button" to="/reservations/new">Reserve parking</Link>
      </main>
    </div>
  );
}
