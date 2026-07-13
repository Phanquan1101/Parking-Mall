# ParkFlow Mall — Demo Script

## 1. Chuẩn bị trước demo

1. Chọn **một** môi trường và khởi động các service cần thiết: API Gateway, Identity, Parking, Payment, Merchant, Reservation, Vision và frontend. Xem [README.md](../README.md) để chạy Hybrid Local Development hoặc Docker full-stack.
2. Mở frontend tại `http://localhost:5173`. Dùng API Gateway `http://localhost:8080` cho hybrid local hoặc `http://localhost:18080` cho Docker full-stack; đặt `VITE_API_BASE_URL` tương ứng nếu cần.
3. Đăng nhập Postman trước để có JWT cho ba vai trò: `admin/admin123`, `staff/staff123`, `merchant/merchant123`.
4. Chuẩn bị một ảnh biển số hoặc biển in rõ. Để demo Gemini thực, cấu hình Vision Service bằng `VISION_OCR_PROVIDER=GEMINI` và `GEMINI_API_KEY`. Nếu không có key, dùng `DEMO_OCR` hoặc nhập biển số thủ công.
5. Chuẩn bị dữ liệu: `59A1-12345`, `INV-UAT-001`/`150000`, `INV-UAT-002`/`150000`, `GATE_IN_01`, `GATE_OUT_01`.
6. Kiểm tra biển số chưa có session active và mã hóa đơn chưa dùng. Vì nhiều service là in-memory, sau restart hãy tạo lại reservation/session/payment cần trình bày.

## 2. Demo story 10–15 phút

| Bước | URL | Actor | Hành động | Kết quả màn hình mong đợi | Talking point |
|---|---|---|---|---|---|
| 1. Tổng quan | `http://localhost:5173/` | Presenter | Mở homepage, chỉ vào route cards và luồng demo. | Luồng Reservation → Gate → Ticket → Merchant → Payment → Exit Pass → Dashboard rõ ràng. | Một hành trình thống nhất cho khách hàng và vận hành bãi xe. |
| 2. Tạo reservation | `/reservations/new` | Customer | Tạo reservation cho `59A1-12345`, thời gian hợp lệ; sao chép `reservationCode`. | Mã opaque và trạng thái `RESERVED` hiển thị. | Reservation Basic không cần deposit/payment trong MVP hiện tại. |
| 3. Mở Gate Entry | `/staff/gate-entry` | Parking staff | Dán staff JWT, nhập `GATE_IN_01`, nhập reservation code. | Console hiển thị camera/OCR/manual fallback và trạng thái thao tác. | Nhân viên là người ra quyết định cuối cùng tại cổng. |
| 4. Scan hoặc fallback | `/staff/gate-entry` | Parking staff | Start camera, scan biển; nếu cần sửa candidate hoặc chuyển manual fallback. | Candidate, confidence/provider và ô xác nhận biển rõ ràng. | OCR chỉ hỗ trợ, không tự tạo session hay mở cổng. |
| 5. Confirm check-in | `/staff/gate-entry` | Parking staff | Xác nhận biển và nhấn check-in một lần. | Session tạo thành công; có session code và customer ticket link/QR handoff. | Parking Service vẫn kiểm tra duplicate active plate và reservation. |
| 6. Show QR ticket | `/tickets/{lookupToken}` | Customer | Mở link trả về từ Gate Entry. | Vé có biển, phí, trạng thái và cảnh báo QR Lookup. | QR Lookup chỉ dùng xem vé/thanh toán, **không** phải quyền ra cổng. |
| 7. Validate invoices | `/merchant/validate` | Merchant staff | Dán merchant JWT; validate `INV-UAT-001` rồi `INV-UAT-002`, mỗi mã `150000`. | Tổng hóa đơn đạt `300000`; thông báo aggregate thành công. | Một invoice chỉ dùng một lần; policy `AGGREGATE_INVOICE` là mặc định. |
| 8. Show discount | `/tickets/{lookupToken}` | Customer | Refresh customer ticket. | Discount `5000` và final fee hiển thị. | Discount không làm final fee âm và được phản ánh trên ticket. |
| 9. Payment simulation | `/tickets/{lookupToken}` | Customer | Tạo payment order, rồi chọn mô phỏng thanh toán thành công. | Payment order/code/amount và trạng thái `PAID` sau refresh. | Đây là Simulation Mode; SePay Live không nằm trong MVP. Idempotency ngăn double-pay. |
| 10. Generate Exit Pass | `/tickets/{lookupToken}` | Customer | Sau PAID, chọn Tạo Exit Pass và sao chép token. | Exit Pass `ACTIVE`, TTL ngắn, thông báo one-time use. | Exit Pass là token khác QR Lookup và chỉ hợp lệ sau thanh toán/zero fee. |
| 11. Validate/check-out | Postman, Gateway API | Parking staff | Với staff JWT, validate pass tại `POST /api/parking/exit-passes/{exitPassToken}/validate`, sau đó check-out tại `POST /api/parking/sessions/{sessionId}/check-out` với plate và `GATE_OUT_01`. | Validate thành công, rồi session `EXITED`; pass bị consume. | Đây là xác minh cổng bảo mật: paid + valid one-time pass + plate match. |
| 12. Show dashboard | `/dashboard` | Admin | Dán admin JWT, chọn Refresh. | Sessions/reservations/metrics và reconciliation section hiển thị. | Admin có reconciliation; staff chỉ nhận notice ADMIN-only cho phần đó. |
| 13. Show offline briefly | `/staff/offline` | Parking staff | Tắt mạng, thêm event offline; bật lại mạng rồi Sync. | Queue `PENDING` chuyển `SYNCED` hoặc hiển thị conflict rõ ràng. | Server là source of truth sau sync; offline không tự động check-out. |

### Postman body tham khảo cho bước check-out

Sau khi thay các biến bằng giá trị vừa tạo:

```json
{
  "exitPassToken": "<exitPassToken>",
  "exitPlate": "59A1-12345",
  "exitGate": "GATE_OUT_01"
}
```

Đặt header `Authorization: Bearer <staffAccessToken>`. Không đưa `lookupToken` vào trường `exitPassToken`: request phải bị từ chối, đây là một điểm bảo mật nên có thể minh họa khi còn thời gian.

## 3. Fallback demo plan

| Sự cố | Cách xử lý khi trình bày |
|---|---|
| Camera không mở hoặc bị từ chối quyền | Chọn manual fallback ở Gate Entry, nhập biển và tiếp tục. Giải thích browser cần quyền camera. |
| Gemini không khả dụng | Kiểm tra Vision Service/`GEMINI_API_KEY`; nếu không thể khôi phục ngay, dùng `DEMO_OCR` hoặc OCR Upload page, sau đó staff xác nhận thủ công. |
| Duplicate plate chặn check-in | Dùng biển số mới trong bộ test, ví dụ `59B2-67890` hoặc `51F1-99999`. Không cố bỏ qua duplicate rule. |
| Mã hóa đơn đã dùng | Dùng mã invoice mới; giữ `INV-UAT-001` để minh họa duplicate rejection nếu cần. |
| In-memory data mất sau restart | Tạo lại reservation, session, invoices và payment theo các bước 2–10; đây là giới hạn của persistence phase hiện tại. |
| Docker full-stack không chạy đủ service | Dùng Hybrid Local Development theo README và đảm bảo frontend trỏ `http://localhost:8080`. |
| Không còn thời gian cho check-out API | Cho thấy Exit Pass trên ticket và giải thích body/Postman request ở bước 11; không khẳng định xe đã exit nếu request chưa chạy. |

## 4. Key explanation points

- **QR Lookup khác Exit Pass:** QR Lookup chỉ mở customer ticket. Nó không được dùng để validate gate hoặc check-out.
- **Exit Pass ngắn hạn, một lần:** Exit Pass chỉ được tạo sau payment/zero fee, có TTL ngắn và phải đúng plate khi staff check-out. Pass đã dùng/hết hạn bị từ chối.
- **OCR cần con người xác nhận:** Gemini/DEMO_OCR chỉ trả candidate/confidence. Staff có thể sửa biển và phải xác nhận trước khi Parking tạo session.
- **Parking Service là source of truth:** Camera cooldown hay Offline queue là hỗ trợ UX. Duplicate plate, payment eligibility và kết quả sync do server quyết định.
- **Giới hạn in-memory:** Service restart có thể xóa demo data. Đây là giới hạn hiện tại, không phải hành vi production.
- **Hướng phát triển:** Persistence Phase sẽ đưa dữ liệu vào PostgreSQL/Supabase theo contract; Docker/Deploy Hardening sẽ ổn định môi trường chạy. Không bật SePay Live trong MVP.
