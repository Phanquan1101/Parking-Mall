import { NavLink } from "react-router-dom";
import type { ReactNode } from "react";

const navigation = [
  { to: "/dashboard", label: "Bảng điều khiển", icon: "▦" },
  { to: "/staff/gate-entry", label: "Cổng vào", icon: "↗" },
  { to: "/staff/ocr-checkin", label: "Quét biển số", icon: "◎" },
  { to: "/staff/offline", label: "Đồng bộ offline", icon: "⇄" },
  { to: "/merchant/validate", label: "Hóa đơn cửa hàng", icon: "◫" },
  { to: "/reservations/new", label: "Đặt chỗ", icon: "□" },
];

export function AppShell({ children }: { children: ReactNode }) {
  return <div className="operations-shell">
    <aside className="operations-sidebar" aria-label="Điều hướng vận hành">
      <NavLink className="shell-brand" to="/dashboard">
        <span className="shell-mark" aria-hidden="true">P</span>
        <span><strong>ParkFlow</strong><small>Mall operations</small></span>
      </NavLink>
      <nav className="operations-nav">
        <p>Vận hành</p>
        {navigation.map((item) => <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-item${isActive ? " nav-item--active" : ""}`}>
          <span aria-hidden="true">{item.icon}</span>{item.label}
        </NavLink>)}
      </nav>
      <div className="sidebar-footer">
        <span className="system-dot" aria-hidden="true" />
        Chế độ demo cục bộ
      </div>
    </aside>
    <div className="operations-content">
      <header className="operations-header">
        <span>ParkFlow Mall · Trung tâm vận hành</span>
        <NavLink to="/" className="public-link">Vé khách hàng ↗</NavLink>
      </header>
      <div className="operations-main">{children}</div>
    </div>
  </div>;
}
