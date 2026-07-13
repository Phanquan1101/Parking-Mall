import { Link } from "react-router-dom";

const features = [
  ["QR Ticket", "Tra cứu vé, trạng thái thanh toán và ưu đãi trên một liên kết an toàn."],
  ["Cổng vào trực tiếp", "Nhận diện biển số hỗ trợ nhân viên xác nhận check-in tại cổng."],
  ["Ưu đãi cửa hàng", "Cộng dồn hóa đơn hợp lệ theo chính sách demo của trung tâm."],
  ["Thanh toán mô phỏng", "Minh họa luồng thanh toán trước khi tích hợp nhà cung cấp thực tế."],
  ["Chế độ offline", "Lưu check-in cục bộ và đồng bộ lại khi kết nối được khôi phục."],
  ["Đặt chỗ", "Tạo mã đặt chỗ trước khi đến và sử dụng tại quầy cổng vào."],
];
const demoFlow = ["Tạo đặt chỗ", "Check-in tại cổng vào", "Mở vé QR", "Xác thực hóa đơn", "Thanh toán mô phỏng", "Tạo Exit Pass", "Xem dashboard"];

export function HomePage() {
  return <main className="home-layout">
    <section className="home-hero home-hero--product">
      <span className="page-eyebrow">ParkFlow Mall</span>
      <h1>Hệ thống gửi xe thông minh cho trung tâm thương mại</h1>
      <p>Quét biển số, cấp QR ticket, thanh toán mô phỏng, kiểm soát ra/vào và vận hành offline — trong một hành trình rõ ràng cho khách hàng và nhân viên.</p>
      <div className="home-actions"><Link className="primary-button" to="/reservations/new">Tạo đặt chỗ</Link><Link className="secondary-button" to="/staff/gate-entry">Mở cổng vào</Link><Link className="text-link" to="/dashboard">Xem bảng điều khiển →</Link></div>
    </section>

    <section className="home-section"><div className="home-section-heading"><h2>Năng lực vận hành</h2><p>Các luồng demo được xây dựng theo từng bước an toàn, dễ quan sát.</p></div><div className="feature-grid">{features.map(([title, description]) => <article key={title} className="feature-item"><strong>{title}</strong><p>{description}</p></article>)}</div></section>

    <section className="home-section"><div className="home-section-heading"><h2>Truy cập nhanh</h2><p>Chọn đúng điểm bắt đầu cho vai trò đang demo.</p></div><div className="demo-route-grid"><Link to="/reservations/new"><strong>Đặt chỗ</strong><span>Dành cho khách trước khi đến</span></Link><Link to="/staff/gate-entry"><strong>Cổng vào</strong><span>Console chính cho nhân viên cổng vào</span></Link><Link to="/merchant/validate"><strong>Hóa đơn cửa hàng</strong><span>Xác thực ưu đãi tại quầy merchant</span></Link><Link to="/dashboard"><strong>Bảng điều khiển</strong><span>Tổng quan vận hành theo quyền truy cập</span></Link></div></section>

    <section className="home-section demo-flow-section"><div className="home-section-heading"><h2>Kịch bản demo đề xuất</h2><p>Một luồng liền mạch từ khách hàng đến vận hành bãi xe.</p></div><ol className="demo-flow-list">{demoFlow.map((step, index) => <li key={step}><span>{index + 1}</span>{step}</li>)}</ol></section>

    <section className="customer-security-card"><div><span className="page-eyebrow">Vé khách hàng</span><h2>Mở vé bằng QR hoặc liên kết</h2><p>Vé khách hàng có dạng <code>/tickets/&#123;lookupToken&#125;</code>. QR chỉ để xem vé và thanh toán; đây không phải Exit Pass. Nhân viên xác thực check-out bằng Exit Pass riêng.</p></div><Link className="secondary-button" to="/reservations/new">Đặt chỗ ngay</Link></section>
  </main>;
}
