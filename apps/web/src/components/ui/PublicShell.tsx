import { Link } from "react-router-dom";
import type { ReactNode } from "react";

export function PublicShell({ children }: { children: ReactNode }) {
  return <div className="public-shell">
    <header className="public-header">
      <Link className="public-brand" to="/"><span aria-hidden="true">P</span> ParkFlow Mall</Link>
      <nav aria-label="Điều hướng khách hàng"><Link to="/reservations/new">Đặt chỗ</Link><Link to="/dashboard">Khu vực nhân viên</Link></nav>
    </header>
    {children}
    <footer className="public-footer">ParkFlow Mall · Vé QR chỉ dùng để tra cứu, không cấp quyền ra cổng.</footer>
  </div>;
}
