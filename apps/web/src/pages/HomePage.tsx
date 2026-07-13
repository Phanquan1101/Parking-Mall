import { Link } from "react-router-dom";

export function HomePage() {
  return (
    <main className="home-layout">
      <section className="home-hero">
        <span className="page-eyebrow">Bãi xe thông minh cho trung tâm thương mại</span>
        <h1>Đến bãi xe nhanh hơn. Theo dõi vé rõ ràng hơn.</h1>
        <p>ParkFlow Mall giúp khách tra cứu vé, đặt chỗ và thanh toán mô phỏng; đồng thời hỗ trợ nhân viên vận hành cổng vào một cách an toàn.</p>
        <div className="home-actions"><Link className="primary-button" to="/reservations/new">Tạo đặt chỗ</Link><Link className="secondary-button" to="/staff/gate-entry">Cổng vào nhân viên</Link></div>
      </section>
      <section className="quick-links" aria-label="Lối tắt ParkFlow">
        <Link to="/reservations/new"><strong>Đặt chỗ</strong><span>Tạo mã đặt chỗ trước khi đến</span></Link>
        <Link to="/dashboard"><strong>Bảng điều khiển</strong><span>Theo dõi phiên gửi xe và vận hành</span></Link>
        <Link to="/merchant/validate"><strong>Hóa đơn cửa hàng</strong><span>Xác thực ưu đãi tại quầy merchant</span></Link>
      </section>
      <section className="route-hint"><strong>Mở vé khách hàng</strong><p>Quét QR tại bãi xe hoặc mở liên kết dạng <code>/tickets/&#123;lookupToken&#125;</code>. Mã QR chỉ dùng để tra cứu, không phải quyền ra cổng.</p></section>
    </main>
  );
}
