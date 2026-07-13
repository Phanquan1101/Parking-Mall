export function StatusBadge({ status }: { status: string }) {
  const normalized = status.toLowerCase().replaceAll("_", "-").replaceAll(" ", "-");
  const labels: Record<string, string> = {
    ACTIVE: "Đang gửi", EXITED: "Đã ra", PAID: "Đã thanh toán", UNPAID: "Chưa thanh toán",
    RESERVED: "Còn hiệu lực", CANCELLED: "Đã hủy", EXPIRED: "Đã hết hạn", CONSUMED: "Đã dùng để check-in",
    PENDING: "Đang chờ", SYNCING: "Đang đồng bộ", SYNCED: "Đã đồng bộ", DUPLICATE: "Trùng lặp", REJECTED: "Bị từ chối", CONFLICT: "Xung đột",
    OPEN: "Đang mở", RESOLVED: "Đã xử lý", PENDING_MANUAL_REVIEW: "Chờ kiểm tra thủ công",
    OCR_ASSISTED: "OCR hỗ trợ", MANUAL: "Nhập thủ công", OFFLINE_CHECK_IN: "Check-in offline",
  };
  return <span className={`status status--${normalized}`}>{labels[status] ?? status.replaceAll("_", " ")}</span>;
}
