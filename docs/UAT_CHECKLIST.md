# ParkFlow Mall — UAT Checklist

## 1. Mục tiêu kiểm thử

Tài liệu này hướng dẫn kiểm thử chấp nhận người dùng (UAT) thủ công sau UI-04. UAT xác nhận hành trình demo frontend, tích hợp API Gateway/backend, phân quyền theo vai trò, camera/Gemini OCR, vòng đời gửi xe, QR Ticket, Payment Simulation, ưu đãi hóa đơn merchant, Exit Pass, check-out, reservation, Offline Staff Mode và dashboard.

Đây là checklist kiểm thử thủ công; không thay thế các kiểm thử tự động trong [TEST_MATRIX.md](TEST_MATRIX.md). Các kiểm tra chỉ có API nên thực hiện bằng Postman collection hiện có hoặc `curl.exe`.

## 2. Môi trường test

| Hạng mục | Giá trị / hướng dẫn |
|---|---|
| Frontend local | `http://localhost:5173` |
| API Gateway — hybrid local | `http://localhost:8080` |
| API Gateway — Docker full-stack | `http://localhost:18080` |
| Chọn một môi trường | Frontend phải trỏ đúng Gateway của môi trường đang chạy; không trộn port `8080` và `18080`. |
| Services cần cho hành trình đầy đủ | API Gateway, Identity, Parking, Payment, Merchant, Reservation, Vision/OCR và frontend. Dashboard dùng các API hiện có. |
| Lệnh Docker tham chiếu | `docker compose --profile full-stack up --build` (chỉ dùng khi full-stack Docker đã build được); xem README để biết Hybrid Local Development. |
| Postman | Import `postman/ParkFlow-Mall.postman_collection.json` và dùng `postman/ParkFlow-Mall.local.postman_environment.json`; cập nhật `baseUrl` sang `18080` khi test Docker. |
| Gemini thực | Đặt `VISION_OCR_PROVIDER=GEMINI` và `GEMINI_API_KEY` ở môi trường Vision Service. Browser không được chứa API key. |
| Fallback OCR | `DEMO_OCR` chỉ phục vụ fallback/demo local; không chứng minh chất lượng OCR Gemini thực. |
| Lưu trữ demo | Nhiều service hiện dùng in-memory; restart service có thể xóa session, payment, reservation, hóa đơn và reconciliation data. |

### Tài khoản demo

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `staff` | `staff123` | `PARKING_STAFF` |
| `merchant` | `merchant123` | `MERCHANT_STAFF` |

Lấy JWT bằng `POST /api/auth/login`. Dán token vào các màn hình staff/dashboard/merchant khi màn hình yêu cầu, hoặc đặt Postman header `Authorization: Bearer <accessToken>`.

## 3. Test data chuẩn

| Loại | Giá trị đề xuất | Lưu ý |
|---|---|---|
| Biển số 1 | `59A1-12345` | Luồng chính reservation → check-in → payment → exit. |
| Biển số 2 | `59B2-67890` | Offline/conflict hoặc OCR. |
| Biển số 3 | `59C1-11111` | Negative/duplicate active plate. |
| Biển số 4 | `51F1-99999` | Fallback test. |
| Hóa đơn 1 | `INV-UAT-001`, `150000` | Hóa đơn hợp lệ thứ nhất. |
| Hóa đơn 2 | `INV-UAT-002`, `150000` | Cộng dồn với hóa đơn 1 để đạt `300000`. |
| Hóa đơn trùng | `INV-UAT-001` | Dùng để xác nhận bị từ chối. |
| Cổng vào | `GATE_IN_01` | Giá trị mặc định. |
| Cổng ra | `GATE_OUT_01` | Dùng cho validate/check-out. |

Trước mỗi vòng chạy, dùng biển số và mã hóa đơn chưa dùng, hoặc restart các service in-memory có liên quan. Ghi token, `sessionId`, `lookupToken`, `paymentOrderId`, `paymentCode`, `reservationCode` và `exitPassToken` tạo được vào cột Notes.

## 4. Quy ước bảng UAT

Mỗi test case dùng một trong bốn trạng thái: **Not Run**, **Pass**, **Fail**, **Blocked**. Tester điền Actual Result, Status và Notes trong lúc chạy.

### A. Authentication and roles

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-A01 | Auth | Admin login | Gateway và Identity đang chạy | Postman: login `admin/admin123` | `200`, nhận JWT và role `ADMIN` | — | Not Run | — |
| UAT-A02 | Auth | Staff login | Như trên | Login `staff/staff123` | `200`, nhận JWT và role `PARKING_STAFF` | — | Not Run | — |
| UAT-A03 | Auth | Merchant login | Như trên | Login `merchant/merchant123` | `200`, nhận JWT và role `MERCHANT_STAFF` | — | Not Run | — |
| UAT-A04 | Auth | Invalid login | Như trên | Login sai password | `401`; không trả JWT hay chi tiết nhạy cảm | — | Not Run | — |
| UAT-A05 | Auth | Current user | Có JWT admin hợp lệ | `GET /api/auth/me` với Bearer token | `200`, user/role đúng với token | — | Not Run | — |
| UAT-A06 | Auth | Missing token | Gateway đang chạy | Gọi API staff-protected không có Authorization | `401` hoặc lỗi unauthorized an toàn | — | Not Run | — |
| UAT-A07 | Auth | Invalid token | Gateway đang chạy | Gọi API protected với JWT giả/hết hạn | Bị từ chối; không lộ stack trace | — | Not Run | — |
| UAT-A08 | Auth | Merchant forbidden staff/admin APIs | Có JWT merchant | Gọi check-in, dashboard parking list và reconciliation run | Các API staff/admin bị `403`; merchant validation vẫn là phạm vi được phép | — | Not Run | — |

### B. Homepage and navigation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-B01 | Homepage | Homepage loads | Frontend đang chạy | Mở `/` | Hero, route cards và cảnh báo QR Lookup hiển thị; không có blank page | — | Not Run | — |
| UAT-B02 | Homepage | Demo route cards work | Frontend đang chạy | Chọn các card Reservation, Gate Entry, Merchant, Dashboard | Điều hướng đúng route, không đổi hành vi API | — | Not Run | — |
| UAT-B03 | Navigation | AppShell staff navigation | Frontend đang chạy | Mở một staff route; dùng menu Dashboard/Gate/OCR/Offline/Merchant | Các link hoạt động và route hiện tại dễ nhận biết | — | Not Run | — |
| UAT-B04 | Navigation | PublicShell pages work | Frontend đang chạy | Mở `/`, `/reservations/new`, `/tickets/<token>` | Public pages hiển thị đúng shell, không yêu cầu JWT để mở ticket/reservation public | — | Not Run | — |
| UAT-B05 | Responsive | Mobile customer ticket layout | Có `lookupToken` hợp lệ | Mở ticket ở viewport khoảng 390px | Thông tin vé, nút payment/Exit Pass và token không tràn ngang; controls dùng được | — | Not Run | — |

### C. Live Gate Entry

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-C01 | Gate Entry | Start camera | Staff JWT; camera có sẵn | Mở `/staff/gate-entry`, dán staff JWT, chọn Start camera | Preview và trạng thái scanning hiển thị; camera chỉ dùng khi người dùng cấp quyền | — | Not Run | — |
| UAT-C02 | Gate Entry | Deny camera permission | Browser có camera | Chặn quyền camera rồi Start camera | Thông báo dễ hiểu và vẫn có manual fallback | — | Not Run | — |
| UAT-C03 | Gate Entry | No camera available | Máy không có camera hoặc chọn thiết bị không tồn tại | Mở Gate Entry, Start camera | Báo không có/không mở được camera an toàn; không crash UI | — | Not Run | — |
| UAT-C04 | Gate Entry | Gemini OCR recognizes plate | `GEMINI`, API key hợp lệ, biển rõ | Scan ảnh/biển `59A1-12345` | Candidate, provider `GEMINI`, confidence/warning hiển thị; chưa check-in tự động | — | Not Run | — |
| UAT-C05 | Gate Entry | Low confidence manual correction | Vision trả confidence thấp/không chắc chắn | Scan biển khó đọc, chỉnh candidate | Nhân viên có thể sửa biển và phải xác nhận trước check-in | — | Not Run | — |
| UAT-C06 | Gate Entry | Enter confirms check-in | Candidate/biển hợp lệ; staff JWT | Điền biển, nhấn Enter hoặc nút xác nhận | Một session `ACTIVE/UNPAID` được tạo, có QR Lookup/customer ticket handoff | — | Not Run | — |
| UAT-C07 | Gate Entry | Double Enter no duplicate | Sau UAT-C06, giữ cùng trạng thái submit | Nhấn Enter nhanh hai lần | Chỉ một request/session được tạo; UI chống double submit | — | Not Run | — |
| UAT-C08 | Gate Entry | QR ticket appears | Check-in thành công | Kiểm tra kết quả Gate Entry và mở ticket link | Có `lookupToken`/ticket link; ticket công khai mở được | — | Not Run | — |
| UAT-C09 | Gate Entry | Next Vehicle resets | Đang hiển thị kết quả check-in | Chọn Next Vehicle | Form/scan state trở về sẵn sàng, không tái sử dụng dữ liệu xe cũ | — | Not Run | — |
| UAT-C10 | Gate Entry | Same-plate cooldown | Vừa scan/check-in một biển | Cố scan lại biển ngay trong cooldown | UI giải thích cooldown; backend vẫn là nguồn quyết định duplicate plate | — | Not Run | — |
| UAT-C11 | Gate Entry | Manual fallback check-in | Camera/OCR không dùng được | Dùng nhập tay biển `51F1-99999`, xác nhận | Check-in thành công với nguồn `MANUAL` nếu biển chưa active | — | Not Run | — |
| UAT-C12 | Gate Entry | Duplicate active plate rejected | Có session active cùng biển | Check-in lại cùng biển qua Gate/manual | Bị từ chối theo business rule; không tạo session thứ hai | — | Not Run | — |
| UAT-C13 | Gate Entry | Merchant cannot OCR/check-in | Có JWT merchant | Dán token merchant và gọi Gate/OCR flow | Vision/check-in bị từ chối theo role; không tạo session | — | Not Run | — |

### D. OCR Upload

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-D01 | OCR Upload | Upload valid image | Staff JWT; JPEG/PNG/WebP hợp lệ | Mở `/staff/ocr-checkin`, upload ảnh biển | Có candidate/metadata hoặc warning an toàn; không tự tạo session | — | Not Run | — |
| UAT-D02 | OCR Upload | Missing image | Staff JWT | Gửi form/call API không có field `image` | `400` hoặc lỗi UI rõ ràng; không gọi check-in | — | Not Run | — |
| UAT-D03 | OCR Upload | Invalid file type | Staff JWT; file `.txt`/không hỗ trợ | Upload file không phải JPEG/PNG/WebP | Bị từ chối rõ ràng; ứng dụng vẫn dùng được | — | Not Run | — |
| UAT-D04 | OCR Upload | GEMINI provider | `VISION_OCR_PROVIDER=GEMINI`, key hợp lệ | Upload ảnh hợp lệ | Response/UI ghi provider `GEMINI`; key không hiển thị ở browser | — | Not Run | — |
| UAT-D05 | OCR Upload | DEMO provider | `VISION_OCR_PROVIDER=DEMO_OCR` | Upload test image demo | Response/UI ghi `DEMO_OCR`; mô tả đây là fallback/demo | — | Not Run | — |
| UAT-D06 | OCR Upload | Merchant forbidden | Có JWT merchant | Gọi `POST /api/vision/ocr/plate` | `403`; không trả candidate hoặc dữ liệu ảnh | — | Not Run | — |
| UAT-D07 | OCR Upload | Staff confirmation required | Có OCR candidate | Chỉnh/giữ candidate rồi kiểm tra trước nút check-in | Không có check-in tự động; staff phải xác nhận biển | — | Not Run | — |

### E. Reservation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-E01 | Reservation | Create reservation | Reservation service đang chạy | `/reservations/new`: tạo `59A1-12345` với khung giờ hợp lệ | Nhận opaque `reservationCode`, trạng thái `RESERVED` | — | Not Run | — |
| UAT-E02 | Reservation | Get reservation | Có code UAT-E01 | Mở `/reservations/{reservationCode}` hoặc public GET | Thấy biển, thời gian, trạng thái; không cần JWT | — | Not Run | — |
| UAT-E03 | Reservation | Cancel reservation | Có reservation `RESERVED` chưa dùng | Cancel ở detail/API | Thành `CANCELLED`; không còn dùng để check-in | — | Not Run | — |
| UAT-E04 | Reservation | Use reservation in check-in | Có reservation `RESERVED` hợp lệ | Gate/manual check-in cùng biển/type, nhập `reservationCode` | Reservation được consume, tạo session `UNPAID` bình thường | — | Not Run | — |
| UAT-E05 | Reservation | Wrong plate rejected | Có reservation hợp lệ cho biển khác | Check-in với code nhưng biển khác | Bị từ chối; reservation không bị consume | — | Not Run | — |
| UAT-E06 | Reservation | Reused reservation rejected | Có reservation đã `CONSUMED` | Dùng code đó cho check-in lần nữa | Bị từ chối; không tạo session mới | — | Not Run | — |
| UAT-E07 | Reservation | Expired reservation behavior | Có reservation quá hạn hoặc gọi expire theo role staff/admin | Mở/dùng reservation đã expire | Trạng thái `EXPIRED`; check-in bị từ chối | — | Not Run | — |
| UAT-E08 | Reservation | Duplicate active reservation per plate | Có reservation `RESERVED` cho cùng biển | Tạo reservation active thứ hai cùng biển | Theo hành vi service: bị từ chối hoặc hiển thị trạng thái an toàn, không âm thầm tạo hold mâu thuẫn | — | Not Run | — |
| UAT-E09 | Reservation | Merchant cannot list reservations | Có JWT merchant | `GET /api/reservations` | `403` | — | Not Run | — |
| UAT-E10 | Reservation | Merchant cannot expire reservations | Có JWT merchant | `POST /api/reservations/expire` | `403`; không đổi dữ liệu | — | Not Run | — |

### F. Parking session and QR ticket

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-F01 | Parking | Manual check-in | Staff JWT; biển chưa active | Postman/Gate: check-in `59C1-11111`, `MANUAL` | Session active, unpaid, `sessionId`, `sessionCode`, `lookupToken` được trả | — | Not Run | — |
| UAT-F02 | Parking | OCR-assisted check-in | Staff JWT; OCR request hợp lệ | Confirm OCR candidate qua Gate/OCR page | Session ghi nguồn `OCR_ASSISTED`; biển confirmed do staff quyết định | — | Not Run | — |
| UAT-F03 | Parking | Reservation check-in | Reservation hợp lệ | Check-in có `reservationCode` | Session mới liên kết reservation/consume thành công | — | Not Run | — |
| UAT-F04 | Parking | Duplicate active plate | Có session active | Gọi check-in cùng normalized plate | Business violation; không có session trùng | — | Not Run | — |
| UAT-F05 | QR Ticket | Public ticket lookup | Có lookup token hợp lệ | `GET /api/public/tickets/{lookupToken}` hoặc mở `/tickets/{lookupToken}` | Chỉ dữ liệu vé an toàn: biển, phí, status; không lộ staff/internal events | — | Not Run | — |
| UAT-F06 | QR Ticket | Invalid ticket token | Token giả | Mở `/tickets/invalid-token` | Safe not-found/404, UI không crash | — | Not Run | — |
| UAT-F07 | QR Security | QR Lookup is not Exit Pass | Có lookup token session chưa/đã paid | Dùng lookup token thay `exitPassToken` khi validate/check-out | Bị từ chối; QR Lookup không mở cổng | — | Not Run | — |

### G. Merchant invoice aggregation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-G01 | Merchant | Validate first invoice | Merchant JWT; active ticket | `/merchant/validate`: token, `INV-UAT-001`, `150000` | Validation thành công, tổng eligible là `150000` | — | Not Run | — |
| UAT-G02 | Merchant | Aggregate two invoices | Sau G01 | Validate `INV-UAT-002`, `150000` cho cùng ticket | Tổng là `300000`; policy aggregate được áp dụng | — | Not Run | — |
| UAT-G03 | Merchant | Discount appears | Sau G02 | Refresh customer ticket | Discount `5000`, `finalFee` được cập nhật và không âm | — | Not Run | — |
| UAT-G04 | Merchant | Duplicate invoice rejected | `INV-UAT-001` đã dùng | Submit lại code đó, cùng hoặc ticket khác | Bị từ chối, không tăng discount/tổng | — | Not Run | — |
| UAT-G05 | Merchant | Invalid lookup token | Merchant JWT; token giả | Submit validation | Lỗi an toàn, không tạo validation | — | Not Run | — |
| UAT-G06 | Merchant | Merchant result page display | Có validation thành công/thất bại | Quan sát result/error page | Kết quả aggregate, discount và lỗi duplicate rõ ràng | — | Not Run | — |
| UAT-G07 | Merchant | Ticket shows discount/finalFee | Có validation thành công | Mở customer ticket tương ứng | `totalEligibleInvoiceAmount`, discount và final fee nhất quán | — | Not Run | — |

### H. Payment simulation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-H01 | Payment | Create payment order | Active unpaid ticket | Ticket page: Tạo lệnh thanh toán, hoặc `POST /api/payments/orders` | Có `paymentOrderId`, `paymentCode`, amount đúng final fee, `PENDING` | — | Not Run | — |
| UAT-H02 | Payment | Simulate success | Có pending order; mode `SIMULATION` | Simulate success với payment code/amount đúng và `Idempotency-Key` | Order/session chuyển `PAID` | — | Not Run | — |
| UAT-H03 | Payment | Wrong paymentCode rejected | Có pending order | Simulate với payment code khác | Không cập nhật Parking sang paid; lỗi/mismatch an toàn | — | Not Run | — |
| UAT-H04 | Payment | Wrong amount rejected | Có pending order | Simulate amount khác order | Không cập nhật Parking sang paid; có mismatch/manual review nếu áp dụng | — | Not Run | — |
| UAT-H05 | Payment | Duplicate success idempotency | Có payment đã xử lý | Gửi lại exact simulate request với cùng `Idempotency-Key` | Nhận kết quả idempotent; không double-pay/duplicate transaction | — | Not Run | — |
| UAT-H06 | Payment | Ticket reflects PAID | H02 thành công | Refresh customer ticket | Badge payment `PAID`; UI cho tạo Exit Pass khi eligible | — | Not Run | — |
| UAT-H07 | Payment | No unnecessary reconciliation | Payment success đã cập nhật Parking | Admin run/list reconciliation | Không tạo OPEN item chỉ vì payment PAID đã `UPDATED` | — | Not Run | — |

### I. Dynamic Exit Pass and checkout

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-I01 | Exit Pass | Generate after paid | Ticket payment `PAID` hoặc zero fee | Ticket page: Tạo Exit Pass | Token opaque, `ACTIVE`, TTL ngắn (mặc định 60 giây) | — | Not Run | — |
| UAT-I02 | Exit Pass | Cannot generate before paid | Session `UNPAID` và final fee > 0 | Gọi generate Exit Pass | Bị từ chối `SESSION_NOT_PAID`/thông báo tương đương | — | Not Run | — |
| UAT-I03 | Exit Pass | Staff validates pass | Staff JWT; pass active; plate đúng | `POST /api/parking/exit-passes/{token}/validate` với `GATE_OUT_01`, plate | Validation thành công nhưng chưa consume pass/session | — | Not Run | — |
| UAT-I04 | Exit Pass | Plate mismatch rejected | Staff JWT; pass active | Validate/check-out bằng biển khác | `PLATE_MISMATCH`; không auto exit | — | Not Run | — |
| UAT-I05 | Checkout | Checkout success | Staff JWT; paid session; valid pass; plate đúng | `POST /api/parking/sessions/{sessionId}/check-out` | Pass consumed, session `EXITED`, exit details được trả | — | Not Run | — |
| UAT-I06 | Exit Pass | Reused pass rejected | I05 thành công | Gọi validate/check-out lại cùng pass | `EXIT_PASS_ALREADY_USED` hoặc lỗi safe tương đương | — | Not Run | — |
| UAT-I07 | QR Security | Lookup token cannot be exit pass | Có lookup token | Gọi validate/check-out dùng lookup token | Từ chối; không đổi session | — | Not Run | — |
| UAT-I08 | Manual override | Override requires reason | Staff/admin JWT; paid/zero-fee session | Gọi manual override thiếu `reason`, sau đó có reason | Thiếu reason bị từ chối; reason hợp lệ tạo audit/override record theo thiết kế | — | Not Run | — |
| UAT-I09 | Manual override | Override does not bypass payment | Session unpaid có fee > 0 | Gọi manual override có reason | Bị từ chối vì chưa paid; không checkout | — | Not Run | — |

### J. Offline staff mode

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-J01 | Offline | Create offline event | Staff JWT đã dán; offline mode browser | `/staff/offline`: tắt mạng, thêm check-in `59B2-67890` | Event `PENDING` được tạo cục bộ; chưa là official session | — | Not Run | — |
| UAT-J02 | Offline | Queue persists localStorage | Có event UAT-J01 | Refresh browser/đóng-mở tab | Event còn trong local queue cùng trạng thái | — | Not Run | — |
| UAT-J03 | Offline | Sync success | Có PENDING event hợp lệ; online; staff JWT | Bật mạng, chọn Sync now | Event `SYNCED`; có official server session/code | — | Not Run | — |
| UAT-J04 | Offline | Duplicate offline event | Có event đã sync hoặc copy same event through API | Gửi lại eventId/idempotency key | `DUPLICATE`; không tạo official session mới | — | Not Run | — |
| UAT-J05 | Offline | Same active plate conflict | Online đã có active session cùng biển; tạo event offline cùng biển | Đồng bộ event | Kết quả `CONFLICT`, vẫn thấy trong queue để review | — | Not Run | — |
| UAT-J06 | Offline | Rejected/conflict visible | Có rejected/conflict result | Mở Offline page | Badge/count/message rõ ràng; không bị xóa im lặng | — | Not Run | — |
| UAT-J07 | Offline | Server source of truth visible | Có bất kỳ sync result | Kiểm tra UI và data sau sync | UI nêu server là nguồn chính; status/server session trả về ghi đè trạng thái local | — | Not Run | — |

### K. Payment reconciliation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-K01 | Reconciliation | Admin can run reconciliation | Admin JWT | `POST /api/payments/reconciliation/run` | Admin được phép chạy, nhận result job/items an toàn | — | Not Run | — |
| UAT-K02 | Reconciliation | Staff/merchant forbidden | Staff rồi merchant JWT | Gọi run reconciliation | Cả hai bị `403` | — | Not Run | — |
| UAT-K03 | Reconciliation | List items | Admin JWT | `GET /api/payments/reconciliation/items` | List trả về an toàn, item không ghi đè nhau | — | Not Run | — |
| UAT-K04 | Reconciliation | Filter items | Admin JWT; có data phù hợp | Lần lượt query `status`, `paymentOrderId`, `targetId` | Filter được forward/áp dụng theo endpoint hỗ trợ | — | Not Run | — |
| UAT-K05 | Reconciliation | Mismatched payment manual review | Có order mismatch nếu tạo được | Chạy/list reconciliation | Mismatch là `PENDING_MANUAL_REVIEW`, không thành payment success | — | Not Run | — |
| UAT-K06 | Reconciliation | No Exit Pass / checkout side effects | Có item reconciliation | Chạy job, kiểm tra session/ticket | Job không tạo Exit Pass, không checkout, không refund/đổi merchant discount | — | Not Run | — |

### L. Dashboard

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-L01 | Dashboard | Admin loads all sections | Admin JWT | `/dashboard`, dán JWT, Refresh | Parking, reservation và reconciliation sections/metrics load | — | Not Run | — |
| UAT-L02 | Dashboard | Staff limited dashboard | Staff JWT | Dán JWT, Refresh | Parking/reservation load; reconciliation hiển thị notice ADMIN-only thay vì crash | — | Not Run | — |
| UAT-L03 | Dashboard | Partial error handling | Chặn một service/API hoặc dùng token phù hợp hạn chế | Refresh dashboard | Phần thất bại có message; các phần API thành công vẫn hiển thị | — | Not Run | — |
| UAT-L04 | Dashboard | Metrics reflect sessions/reservations | Đã tạo session/reservation trong UAT | Refresh | Counts/status và rows phản ánh data hiện tại | — | Not Run | — |
| UAT-L05 | Dashboard | Empty state before data | Environment mới/restart in-memory | Refresh trước khi tạo data | Empty states rõ ràng, không hiển thị số liệu bịa đặt | — | Not Run | — |
| UAT-L06 | Dashboard | Refresh button | Có JWT valid | Nhấn Làm mới dữ liệu nhiều lần hợp lý | Disabled/loading state rõ ràng; data cập nhật không crash | — | Not Run | — |

### M. Security and negative cases

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Notes |
|---|---|---|---|---|---|---|---|---|
| UAT-M01 | Security | Internal endpoints not exposed | Gateway đang chạy | Gọi Gateway `/internal/parking/sessions/{id}/payment-status` và `/internal/parking/sessions/{id}/discount` | Không được route/publicly exposed qua Gateway | — | Not Run | — |
| UAT-M02 | Security | CORS local frontend works | Frontend/Gateway đang chạy | Dùng UI local gọi API bình thường | Browser không bị CORS block trong mode cấu hình được hỗ trợ | — | Not Run | — |
| UAT-M03 | Security | Missing JWT protected APIs | Gateway đang chạy | Gọi check-in, offline sync, OCR, reservation list không Bearer token | Bị `401`/unauthorized nhất quán | — | Not Run | — |
| UAT-M04 | Security | Wrong role rejected | Có token merchant/customer context | Thử endpoint staff/admin tương ứng | Bị `403`; không có thay đổi server state | — | Not Run | — |
| UAT-M05 | UX Security | Long token wraps in UI | Có JWT/token dài | Dán token dài vào staff/dashboard/merchant fields và xem ticket token | Không phá vỡ layout; token có thể copy an toàn | — | Not Run | — |
| UAT-M06 | UX | Invalid inputs show friendly errors | Frontend đang chạy | Gửi thiếu biển, thiếu cổng, invoice amount âm, thời gian reservation sai | Lỗi có hướng dẫn; không crash và không tạo dữ liệu sai | — | Not Run | — |
| UAT-M07 | Security | Error responses safe | Có lỗi 401/403/404/validation | Quan sát UI và API response | Không lộ JWT, API key, stack trace, internal token hay raw exception | — | Not Run | — |

## 5. Tổng kết UAT

- Tổng số test case: **99**.
- Chỉ đánh dấu **Pass** khi kết quả thực tế khớp Expected Result và không thấy lỗi phụ.
- Với **Fail**, ghi request/response đã che token, thời điểm, URL/môi trường, dữ liệu test và ảnh màn hình nếu có.
- Với **Blocked**, ghi service/configuration còn thiếu (ví dụ camera, Gemini key, Docker service) để phân biệt với lỗi sản phẩm.
- Không dùng UAT để bật SePay Live, thay đổi role, hay bỏ qua QR Lookup/Exit Pass/payment/offline business rules.
