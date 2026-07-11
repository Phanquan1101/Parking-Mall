# Product Requirement Document — PRD v2

## Project Name

**ParkFlow Mall — Smart Parking & Reservation Management System**

## Version

**PRD v2.0**

## Based On

* BRD v2 — ParkFlow Mall
* SAD v1 — Microservice Architecture
* Implementation Pack for Agent Codex
* Target development approach: Vertical Slice, Microservice-oriented, Agent-controlled implementation

---

# 1. Product Overview

## 1.1 Product Summary

ParkFlow Mall là hệ thống quản lý giữ xe thông minh dành cho hầm giữ xe tại trung tâm thương mại. Hệ thống hỗ trợ tạo phiên gửi xe, QR ticket, nhận diện biển số bằng OCR, thanh toán qua Payment Simulation/SePay Test, giảm phí theo hóa đơn mua hàng, đặt lịch gửi xe trước, vận hành offline khi mất mạng, dashboard quản lý và cảnh báo gian lận.

PRD v2 này chuyển đổi các yêu cầu nghiệp vụ từ BRD v2 và định hướng kiến trúc từ SAD v1 thành các yêu cầu sản phẩm có thể giao cho Agent Codex triển khai theo từng module và vertical slice.

---

## 1.2 Product Goals

| Goal ID | Product Goal                            | Description                                                                   | Priority    |
| ------- | --------------------------------------- | ----------------------------------------------------------------------------- | ----------- |
| PG-001  | Quản lý vòng đời gửi xe                 | Hỗ trợ check-in, QR ticket, thanh toán, check-out và đóng session             | Must Have   |
| PG-002  | Giảm thời gian xử lý tại cổng           | Staff thao tác nhanh, khách không phải làm nhiều bước                         | Must Have   |
| PG-003  | Tăng an toàn QR                         | QR Lookup không được dùng để mở cổng; Exit Pass phải ngắn hạn và dùng một lần | Must Have   |
| PG-004  | Hỗ trợ thanh toán an toàn               | Payment Simulation trước, SePay Test sau, không dùng Live trong MVP           | Must Have   |
| PG-005  | Hỗ trợ vận hành khi mất mạng            | Staff có offline queue, local cache và sync lại khi online                    | Must Have   |
| PG-006  | Hỗ trợ giảm phí hóa đơn thực tế         | Cho phép cộng dồn nhiều hóa đơn cho một parking session                       | Must Have   |
| PG-007  | Hỗ trợ đặt chỗ trước                    | Khách có thể reservation, thanh toán trước/test, check-in theo khung giờ      | Should Have |
| PG-008  | Hỗ trợ quản lý vận hành                 | Dashboard hiển thị xe, payment, discount, offline sync, suspicious case       | Must Have   |
| PG-009  | Tạo nền cho microservice implementation | Yêu cầu rõ để Agent Codex code theo module và slice                           | Must Have   |

---

## 1.3 MVP Scope

### In Scope

| Scope ID | Scope Item                   | Description                                            | Priority    |
| -------- | ---------------------------- | ------------------------------------------------------ | ----------- |
| MVP-001  | Identity & Role              | Admin, Parking Staff, Merchant Staff                   | Must Have   |
| MVP-002  | Parking Session              | Tạo, quản lý, đóng phiên gửi xe                        | Must Have   |
| MVP-003  | QR Lookup Ticket             | QR dùng để tra cứu thông tin vé/session                | Must Have   |
| MVP-004  | Dynamic Exit Pass            | Mã/QR ngắn hạn dùng để check-out sau payment           | Must Have   |
| MVP-005  | Payment Simulation           | Thanh toán giả lập, webhook giả lập, idempotency       | Must Have   |
| MVP-006  | SePay Test Mode              | Chuẩn bị luồng test với SePay, chưa dùng Live          | Should Have |
| MVP-007  | Offline Staff Mode           | Local cache, offline queue, sync, conflict             | Must Have   |
| MVP-008  | Merchant Invoice Aggregation | Nhiều hóa đơn cộng dồn cho một session                 | Must Have   |
| MVP-009  | Reservation Basic            | Đặt chỗ, giữ slot, trạng thái payment, check-in window | Should Have |
| MVP-010  | Dashboard                    | Admin xem vận hành, doanh thu, pending, suspicious     | Must Have   |
| MVP-011  | OCR Assist                   | OCR gợi ý biển số, staff xác nhận/sửa                  | Should Have |
| MVP-012  | Audit & Fraud                | Audit log, manual override, suspicious alert           | Must Have   |

---

### Out of Scope for MVP

| Item                      | Reason                               | Future Consideration |
| ------------------------- | ------------------------------------ | -------------------- |
| Barrier vật lý thật       | Cần phần cứng và tích hợp thiết bị   | Phase sau            |
| Camera cố định production | Cần setup thực tế, tuning và hạ tầng | Phase sau            |
| SePay Live Mode           | Rủi ro tiền thật và đối soát         | Sau khi test mode ổn |
| POS cửa hàng thật         | Cần tích hợp đối tác                 | Future integration   |
| Cảm biến từng ô đỗ        | Quá nặng phần cứng                   | Future smart parking |
| Mobile app native         | Web responsive đủ cho MVP            | Sau validation       |
| Vé tháng/VIP              | Tăng scope                           | Future               |
| Dynamic pricing nâng cao  | Dễ làm phình rule                    | Sau core flow        |
| Refund thật               | Cần policy tài chính                 | Future               |
| Multi-mall production     | Tăng complexity                      | Future SaaS model    |

---

# 2. User Roles and Permissions

## 2.1 Roles

| Role           | Description                                                 |
| -------------- | ----------------------------------------------------------- |
| ADMIN          | Quản lý hệ thống, dashboard, tài khoản, rule, audit, report |
| PARKING_STAFF  | Nhân viên cổng vào/ra, check-in, check-out, xử lý ngoại lệ  |
| MERCHANT_STAFF | Nhân viên cửa hàng, xác nhận hóa đơn giảm phí               |
| CUSTOMER       | Khách gửi xe, không bắt buộc đăng nhập trong MVP            |
| SYSTEM         | Job nội bộ: reconciliation, expiration, sync processing     |

---

## 2.2 Permission Matrix

| Feature / Action           |         Admin |   Parking Staff | Merchant Staff |      Customer | System |
| -------------------------- | ------------: | --------------: | -------------: | ------------: | -----: |
| Login                      |           Yes |             Yes |            Yes |      Optional |     No |
| Create staff account       |           Yes |              No |             No |            No |     No |
| Check-in vehicle           |           Yes |             Yes |             No |            No |     No |
| Check-out vehicle          |           Yes |             Yes |             No |            No |     No |
| View customer ticket by QR |       Limited |             Yes |        Limited | Yes via token |     No |
| Create payment order       |            No |        Optional |             No |           Yes |     No |
| Simulate payment success   | Yes/Test role |              No |             No |            No |    Yes |
| Validate merchant invoice  |           Yes |              No |            Yes |            No |     No |
| Create reservation         |      Optional |        Optional |             No |           Yes |     No |
| View dashboard             |           Yes |         Limited |        Limited |            No |     No |
| View audit log             |           Yes |              No |             No |            No |     No |
| Process reconciliation     |            No |              No |             No |            No |    Yes |
| Manual override            |           Yes | Yes with reason |             No |            No |     No |

---

# 3. Global Product Rules

| Rule ID | Rule                                                                    | Priority  |
| ------- | ----------------------------------------------------------------------- | --------- |
| GPR-001 | QR Lookup Token chỉ dùng để tra cứu session, không được dùng để mở cổng | Must Have |
| GPR-002 | Dynamic Exit Pass phải có TTL ngắn và dùng một lần                      | Must Have |
| GPR-003 | Payment Simulation Mode phải được triển khai trước SePay Test Mode      | Must Have |
| GPR-004 | SePay Live Mode không thuộc MVP                                         | Must Have |
| GPR-005 | Offline Staff Mode phải có local queue và sync lại khi online           | Must Have |
| GPR-006 | Merchant invoice validation mặc định dùng AGGREGATE_INVOICE policy      | Must Have |
| GPR-007 | Duplicate webhook/simulation event phải idempotent                      | Must Have |
| GPR-008 | OCR chỉ là hỗ trợ, staff phải xác nhận/sửa biển số                      | Must Have |
| GPR-009 | Manual override phải có reason và audit log                             | Must Have |
| GPR-010 | Server là source of truth sau offline sync                              | Must Have |
| GPR-011 | Dashboard phải phân biệt confirmed, pending, conflict, suspicious       | Must Have |
| GPR-012 | Không tự thêm tính năng ngoài scope khi chưa có decision log            | Must Have |

Canonical priority clarification:

- Manual plate entry and staff plate confirmation are Must Have.
- OCR Assist is Should Have and cannot block check-in or check-out.
- Reservation Basic is Should Have and follows Payment Reconciliation.
- Payment Simulation is Must Have; SePay Test is Should Have; SePay Live is disabled and out of MVP.

---

# 4. Module 1 — Identity

## 4.1 Module Overview

Identity module quản lý đăng nhập, phân quyền và quyền truy cập cho Admin, Parking Staff và Merchant Staff. Customer trong MVP không bắt buộc đăng nhập; customer truy cập ticket bằng QR Lookup Token.

---

## 4.2 User Stories

| Story ID  | User Story                                                              | Priority    |
| --------- | ----------------------------------------------------------------------- | ----------- |
| US-ID-001 | Là Admin, tôi muốn đăng nhập để truy cập dashboard quản lý              | Must Have   |
| US-ID-002 | Là Parking Staff, tôi muốn đăng nhập để dùng Staff Gate Console         | Must Have   |
| US-ID-003 | Là Merchant Staff, tôi muốn đăng nhập để xác nhận hóa đơn cho khách     | Must Have   |
| US-ID-004 | Là Admin, tôi muốn tạo tài khoản staff và merchant staff                | Should Have |
| US-ID-005 | Là hệ thống, tôi muốn kiểm tra role của user trước khi cho truy cập API | Must Have   |
| US-ID-006 | Là Admin, tôi muốn khóa/mở tài khoản nhân viên                          | Could Have  |

---

## 4.3 Functional Requirements

| FR ID     | Requirement                                                     | Priority    |
| --------- | --------------------------------------------------------------- | ----------- |
| FR-ID-001 | Hệ thống phải hỗ trợ đăng nhập bằng username/email và password  | Must Have   |
| FR-ID-002 | Hệ thống phải phát hành JWT sau khi đăng nhập thành công        | Must Have   |
| FR-ID-003 | Hệ thống phải gắn role vào user token hoặc claims               | Must Have   |
| FR-ID-004 | Backend phải enforce role-based access, không chỉ ẩn UI         | Must Have   |
| FR-ID-005 | Admin có thể tạo user cho Parking Staff và Merchant Staff       | Should Have |
| FR-ID-006 | Password phải được hash, không lưu plain text                   | Must Have   |
| FR-ID-007 | Hệ thống phải có endpoint kiểm tra current user/profile         | Should Have |
| FR-ID-008 | Hệ thống phải ghi audit log cho login thất bại nhiều lần nếu có | Could Have  |

---

## 4.4 Acceptance Criteria

| AC ID     | Acceptance Criteria                                                         |
| --------- | --------------------------------------------------------------------------- |
| AC-ID-001 | User nhập đúng credentials thì nhận được JWT hợp lệ                         |
| AC-ID-002 | User nhập sai credentials thì nhận lỗi rõ ràng, không lộ thông tin nhạy cảm |
| AC-ID-003 | Parking Staff không truy cập được Admin Dashboard API                       |
| AC-ID-004 | Merchant Staff không truy cập được Staff Check-out API                      |
| AC-ID-005 | Customer không cần login để mở ticket page bằng QR Lookup Token             |
| AC-ID-006 | API protected trả 401 nếu thiếu token, 403 nếu sai role                     |
| AC-ID-007 | Admin tạo staff thành công thì staff có thể login                           |

---

## 4.5 Edge Cases

| Edge Case                                          | Expected Handling               |
| -------------------------------------------------- | ------------------------------- |
| Token hết hạn                                      | Frontend yêu cầu login lại      |
| User bị khóa                                       | Không cho login                 |
| Role bị thiếu                                      | Từ chối truy cập protected API  |
| Merchant Staff cố validate invoice của tenant khác | Reject với lỗi quyền truy cập   |
| Parking Staff cố xem audit log toàn hệ thống       | Reject 403                      |
| Customer mở QR token không hợp lệ                  | Hiển thị ticket invalid/expired |

---

## 4.6 Out of Scope

| Item                          | Reason                                      |
| ----------------------------- | ------------------------------------------- |
| Social login                  | Không cần cho MVP                           |
| Customer account đầy đủ       | Customer no-login là chủ đích giảm friction |
| Multi-factor authentication   | Future security enhancement                 |
| Password reset email          | Có thể thêm sau                             |
| Advanced permission matrix UI | MVP chỉ cần role cơ bản                     |

---

# 5. Module 2 — Parking Session

## 5.1 Module Overview

Parking Session là module lõi, quản lý vòng đời gửi xe từ check-in đến check-out. Mỗi xe vào bãi sẽ tạo một session, có biển số, ảnh vào, QR Lookup Token, trạng thái payment, discount, exit state và audit trail.

---

## 5.2 Parking Session Status

| Status               | Description                              |
| -------------------- | ---------------------------------------- |
| ACTIVE               | Xe đang gửi trong bãi                    |
| PENDING_PAYMENT      | Session có phí cần thanh toán            |
| PAID                 | Session đã thanh toán                    |
| EXITED               | Xe đã ra khỏi bãi                        |
| SUSPICIOUS           | Session có dấu hiệu bất thường           |
| LOST_QR              | Khách báo mất QR                         |
| OFFLINE_PENDING_SYNC | Event được tạo offline, chờ sync         |
| CONFLICT             | Offline sync bị xung đột                 |
| CANCELLED            | Session bị hủy bởi admin/staff theo rule |

---

## 5.3 User Stories

| Story ID  | User Story                                                                    | Priority  |
| --------- | ----------------------------------------------------------------------------- | --------- |
| US-PS-001 | Là Parking Staff, tôi muốn tạo phiên gửi xe khi xe vào cổng                   | Must Have |
| US-PS-002 | Là Parking Staff, tôi muốn nhập hoặc xác nhận biển số xe                      | Must Have |
| US-PS-003 | Là Parking Staff, tôi muốn hệ thống sinh QR Lookup Token sau check-in         | Must Have |
| US-PS-004 | Là Customer, tôi muốn quét QR để xem thông tin vé và phí                      | Must Have |
| US-PS-005 | Là Parking Staff, tôi muốn check-out xe khi payment và biển số hợp lệ         | Must Have |
| US-PS-006 | Là Parking Staff, tôi muốn xử lý mất QR bằng cách tìm session theo biển số    | Must Have |
| US-PS-007 | Là Admin, tôi muốn xem danh sách session đang active                          | Must Have |
| US-PS-008 | Là Parking Staff, tôi muốn đánh dấu session suspicious nếu biển số không khớp | Must Have |

---

## 5.4 Functional Requirements

| FR ID     | Requirement                                                                      | Priority  |
| --------- | -------------------------------------------------------------------------------- | --------- |
| FR-PS-001 | Hệ thống phải cho Parking Staff tạo parking session                              | Must Have |
| FR-PS-002 | Mỗi session phải có session_code duy nhất                                        | Must Have |
| FR-PS-003 | Mỗi session phải lưu vehicle_plate hoặc manual_plate flag nếu chưa xác định được | Must Have |
| FR-PS-004 | Hệ thống phải chặn tạo ACTIVE session trùng biển số trong cùng bãi               | Must Have |
| FR-PS-005 | Hệ thống phải tạo QR Lookup Token sau khi session được tạo                       | Must Have |
| FR-PS-006 | Customer có thể mở ticket page bằng QR Lookup Token                              | Must Have |
| FR-PS-007 | Hệ thống phải tính thời gian gửi xe dựa trên entry_time                          | Must Have |
| FR-PS-008 | Hệ thống phải lưu trạng thái payment của session                                 | Must Have |
| FR-PS-009 | Hệ thống phải lưu discount_amount và final_fee                                   | Must Have |
| FR-PS-010 | Hệ thống phải cho Staff check-out khi payment hợp lệ và biển số khớp             | Must Have |
| FR-PS-011 | Hệ thống phải chuyển session sang EXITED sau check-out thành công                | Must Have |
| FR-PS-012 | Hệ thống phải hỗ trợ tìm session theo plate, session_code hoặc QR token          | Must Have |
| FR-PS-013 | Hệ thống phải hỗ trợ Lost QR flow                                                | Must Have |
| FR-PS-014 | Hệ thống phải tạo audit log cho check-in/check-out/manual override               | Must Have |
| FR-PS-015 | Hệ thống phải hỗ trợ update session khi merchant discount thay đổi               | Must Have |

---

## 5.5 Acceptance Criteria

| AC ID     | Acceptance Criteria                                                                        |
| --------- | ------------------------------------------------------------------------------------------ |
| AC-PS-001 | Staff tạo session với biển số hợp lệ thì session status là ACTIVE                          |
| AC-PS-002 | Sau check-in, hệ thống sinh QR Lookup Token hợp lệ                                         |
| AC-PS-003 | Customer mở QR Lookup thì thấy biển số, entry time, duration, fee estimate, payment status |
| AC-PS-004 | QR Lookup Token không thể gọi API check-out                                                |
| AC-PS-005 | Nếu session chưa PAID và final_fee > 0, check-out tự động bị từ chối                       |
| AC-PS-006 | Nếu session PAID, Exit Pass hợp lệ và plate khớp, session chuyển EXITED                    |
| AC-PS-007 | Nếu plate lúc ra khác plate lúc vào, session chuyển SUSPICIOUS hoặc yêu cầu manual review  |
| AC-PS-008 | Nếu khách mất QR, staff tìm được session bằng plate và xử lý theo Lost QR flow             |
| AC-PS-009 | Tạo ACTIVE session trùng biển số bị chặn hoặc yêu cầu xác nhận exception                   |
| AC-PS-010 | Mọi check-in/check-out đều có audit log                                                    |

---

## 5.6 Edge Cases

| Edge Case                      | Expected Handling                                       |
| ------------------------------ | ------------------------------------------------------- |
| Biển số đã có ACTIVE session   | Chặn tạo mới hoặc chuyển staff sang duplicate review    |
| Staff nhập sai biển số         | Cho phép sửa nếu session chưa exit, phải audit          |
| Khách mất QR                   | Staff tìm theo plate, đối chiếu ảnh/session             |
| Không có ảnh vào               | Cho phép nếu staff nhập reason hoặc MVP cho phép bỏ qua |
| Check-out khi chưa thanh toán  | Reject và hướng dẫn thanh toán                          |
| Check-out khi quá grace period | Yêu cầu tính thêm phí hoặc payment bổ sung              |
| QR đã dùng sau EXITED          | Reject                                                  |
| Plate mismatch                 | SUSPICIOUS                                              |
| Staff manual override          | Bắt buộc reason và audit log                            |
| Session active quá lâu         | Dashboard cảnh báo nếu vượt ngưỡng cấu hình             |

---

## 5.7 Out of Scope

| Item                         | Reason                        |
| ---------------------------- | ----------------------------- |
| Barrier vật lý               | Không thuộc MVP               |
| Fee engine phức tạp          | MVP chỉ cần fee rule đơn giản |
| Vé tháng/VIP                 | Future                        |
| Multi-parking-lot production | Future                        |
| Sensor slot thật             | Future                        |

---

# 6. Module 3 — QR Security

## 6.1 Module Overview

QR Security module đảm bảo QR không trở thành lỗ hổng bảo mật. Hệ thống tách rõ QR Lookup Token và Dynamic Exit Pass. QR Lookup chỉ giúp khách/staff tra cứu vé. Dynamic Exit Pass mới được dùng trong check-out và phải ngắn hạn, dùng một lần, liên kết với session đã thanh toán.

---

## 6.2 Token Types

| Token Type          | Purpose                              | Gate Authorization           |
| ------------------- | ------------------------------------ | ---------------------------- |
| QR Lookup Token     | Mở ticket page, tra cứu session      | No                           |
| Dynamic Exit Pass   | Xác thực quyền check-out sau payment | Yes, with validation         |
| Reservation QR      | Tra cứu reservation                  | No direct gate authorization |
| Offline Proof Token | Optional proof khi offline           | Limited / Future             |

---

## 6.3 User Stories

| Story ID  | User Story                                                                         | Priority    |
| --------- | ---------------------------------------------------------------------------------- | ----------- |
| US-QR-001 | Là Customer, tôi muốn quét QR để xem vé xe                                         | Must Have   |
| US-QR-002 | Là Customer, tôi muốn nhận Exit Pass sau khi thanh toán                            | Must Have   |
| US-QR-003 | Là Parking Staff, tôi muốn scan Exit Pass để check-out                             | Must Have   |
| US-QR-004 | Là Admin, tôi muốn QR dùng lại hoặc hết hạn bị từ chối                             | Must Have   |
| US-QR-005 | Là hệ thống, tôi muốn QR Lookup không mở được cổng                                 | Must Have   |
| US-QR-006 | Là Customer, tôi muốn tạo lại Exit Pass nếu mã cũ hết hạn nhưng session còn hợp lệ | Should Have |

---

## 6.4 Functional Requirements

| FR ID     | Requirement                                                                 | Priority    |
| --------- | --------------------------------------------------------------------------- | ----------- |
| FR-QR-001 | Hệ thống phải tạo QR Lookup Token khi session được tạo                      | Must Have   |
| FR-QR-002 | QR Lookup Token phải là opaque random token, không chứa plate/amount        | Must Have   |
| FR-QR-003 | QR Lookup Token chỉ dùng cho ticket lookup                                  | Must Have   |
| FR-QR-004 | Hệ thống phải tạo Dynamic Exit Pass sau khi payment PAID hoặc final_fee = 0 | Must Have   |
| FR-QR-005 | Dynamic Exit Pass phải có TTL cấu hình được                                 | Must Have   |
| FR-QR-006 | Dynamic Exit Pass phải dùng một lần                                         | Must Have   |
| FR-QR-007 | Dynamic Exit Pass phải gắn với session_id                                   | Must Have   |
| FR-QR-008 | Hệ thống phải reject Exit Pass expired/used/invalid                         | Must Have   |
| FR-QR-009 | Hệ thống phải log mọi lần tạo và sử dụng Exit Pass                          | Should Have |
| FR-QR-010 | Hệ thống phải cho tạo lại Exit Pass nếu session vẫn eligible                | Should Have |

---

## 6.5 Acceptance Criteria

| AC ID     | Acceptance Criteria                                                   |
| --------- | --------------------------------------------------------------------- |
| AC-QR-001 | QR Lookup Token mở được ticket page                                   |
| AC-QR-002 | QR Lookup Token không gọi được check-out API                          |
| AC-QR-003 | Sau payment thành công, hệ thống tạo Exit Pass                        |
| AC-QR-004 | Exit Pass hết hạn bị reject                                           |
| AC-QR-005 | Exit Pass đã dùng bị reject khi dùng lại                              |
| AC-QR-006 | Exit Pass hợp lệ nhưng plate mismatch vẫn không được auto exit        |
| AC-QR-007 | Customer có thể refresh Exit Pass nếu session còn PAID và chưa EXITED |
| AC-QR-008 | Session EXITED thì mọi token liên quan không còn dùng để check-out    |

---

## 6.6 Edge Cases

| Edge Case                            | Expected Handling                                   |
| ------------------------------------ | --------------------------------------------------- |
| Người khác chụp QR Lookup            | Chỉ xem được thông tin tối thiểu, không mở cổng     |
| Người khác chụp Exit Pass            | Exit Pass TTL ngắn, cần plate match                 |
| Exit Pass hết hạn khi khách đến cổng | Tạo lại nếu online; offline thì staff manual review |
| QR bị đoán token                     | Token phải đủ entropy, rate limit nếu có            |
| Customer mở QR trên máy khác         | Cho xem ticket; device claim là future/optional     |
| Session đã EXITED                    | Token invalid cho check-out                         |

---

## 6.7 Out of Scope

| Item                                | Reason                           |
| ----------------------------------- | -------------------------------- |
| Device claim bắt buộc               | Tăng UX complexity, để phase sau |
| OTP SMS                             | Cần provider ngoài               |
| Biometric verification              | Không phù hợp MVP                |
| Offline signed Exit Pass hoàn chỉnh | Có thể nghiên cứu sau MVP        |

---

# 7. Module 4 — Payment Simulation / SePay Test

## 7.1 Module Overview

Payment module xử lý thanh toán cho parking session và reservation. MVP bắt buộc có Payment Simulation Mode để test an toàn. SePay Test Mode được triển khai sau khi simulation ổn. SePay Live không thuộc MVP.

---

## 7.2 Payment Modes

| Mode       | Description                        | MVP Status     |
| ---------- | ---------------------------------- | -------------- |
| SIMULATION | Giả lập payment success/webhook    | Required first |
| SEPAY_TEST | Tích hợp thử với SePay/VietQR test | Should Have    |
| SEPAY_LIVE | Tiền thật                          | Out of MVP     |

---

## 7.3 Payment Status

| Status                 | Description                          |
| ---------------------- | ------------------------------------ |
| PENDING                | Đã tạo payment order, chờ thanh toán |
| PAID                   | Đã thanh toán hợp lệ                 |
| FAILED                 | Thanh toán thất bại                  |
| EXPIRED                | Payment order hết hạn                |
| MISMATCHED             | Amount/payment_code không khớp       |
| PENDING_RECONCILIATION | Cần job hoặc admin đối soát          |

---

## 7.4 User Stories

| Story ID   | User Story                                                                 | Priority    |
| ---------- | -------------------------------------------------------------------------- | ----------- |
| US-PAY-001 | Là Customer, tôi muốn tạo payment order cho vé xe                          | Must Have   |
| US-PAY-002 | Là Customer, tôi muốn xem số tiền cần thanh toán                           | Must Have   |
| US-PAY-003 | Là Tester/Admin, tôi muốn giả lập payment success                          | Must Have   |
| US-PAY-004 | Là hệ thống, tôi muốn duplicate payment event không làm trả tiền hai lần   | Must Have   |
| US-PAY-005 | Là Customer, tôi muốn session chuyển PAID sau thanh toán thành công        | Must Have   |
| US-PAY-006 | Là Admin, tôi muốn xem payment pending/mismatched                          | Must Have   |
| US-PAY-007 | Là hệ thống, tôi muốn reconciliation job sửa trạng thái lệch               | Must Have   |
| US-PAY-008 | Là Customer, tôi muốn thanh toán qua SePay Test Mode khi hệ thống sẵn sàng | Should Have |

---

## 7.5 Functional Requirements

| FR ID      | Requirement                                                                       | Priority    |
| ---------- | --------------------------------------------------------------------------------- | ----------- |
| FR-PAY-001 | Hệ thống phải tạo payment order cho parking session                               | Must Have   |
| FR-PAY-002 | Payment order phải có payment_code duy nhất                                       | Must Have   |
| FR-PAY-003 | Payment order phải lưu target_type và target_id                                   | Must Have   |
| FR-PAY-004 | Simulation endpoint phải mô phỏng webhook success                                 | Must Have   |
| FR-PAY-005 | Duplicate simulation/webhook event phải idempotent                                | Must Have   |
| FR-PAY-006 | Payment chỉ hợp lệ khi amount và payment_code khớp                                | Must Have   |
| FR-PAY-007 | Khi payment PAID, Parking Session phải cập nhật payment_status                    | Must Have   |
| FR-PAY-008 | Khi payment PAID, hệ thống phải set grace_period_until                            | Must Have   |
| FR-PAY-009 | Khi payment PAID, hệ thống phải tạo Dynamic Exit Pass hoặc cho phép tạo Exit Pass | Must Have   |
| FR-PAY-010 | Payment Service phải lưu webhook event raw/minimal metadata                       | Should Have |
| FR-PAY-011 | Payment Service phải hỗ trợ SePay Test endpoint/webhook                           | Should Have |
| FR-PAY-012 | Hệ thống phải có reconciliation job cho payment pending/mismatched                | Must Have   |
| FR-PAY-013 | Payment Live phải bị vô hiệu trong MVP                                            | Must Have   |

---

## 7.6 Acceptance Criteria

| AC ID      | Acceptance Criteria                                                       |
| ---------- | ------------------------------------------------------------------------- |
| AC-PAY-001 | Customer tạo payment order cho ACTIVE/PENDING_PAYMENT session thành công  |
| AC-PAY-002 | Simulation success chuyển payment từ PENDING sang PAID                    |
| AC-PAY-003 | Duplicate simulation request không tạo duplicate transaction              |
| AC-PAY-004 | Sai amount hoặc sai payment_code chuyển MISMATCHED/PENDING_RECONCILIATION |
| AC-PAY-005 | Payment PAID cập nhật Parking Session thành PAID                          |
| AC-PAY-006 | Payment PAID tạo hoặc unlock Dynamic Exit Pass                            |
| AC-PAY-007 | Reconciliation job tìm được payment pending quá hạn                       |
| AC-PAY-008 | SePay Live không được gọi trong MVP                                       |
| AC-PAY-009 | Dashboard hiển thị payment pending/mismatched                             |

---

## 7.7 Edge Cases

| Edge Case                                 | Expected Handling                                   |
| ----------------------------------------- | --------------------------------------------------- |
| Webhook duplicate                         | Ignore safely, return success                       |
| Payment amount thấp hơn required          | MISMATCHED                                          |
| Payment code sai                          | PENDING_RECONCILIATION                              |
| Payment success nhưng Parking Service lỗi | Payment PAID, session update pending reconciliation |
| Payment success sau khi session EXITED    | Manual review                                       |
| Payment order hết hạn                     | EXPIRED                                             |
| Customer thanh toán sau grace period      | Tính thêm phí hoặc payment bổ sung theo future rule |
| SePay Test không ổn định                  | Fallback Simulation Mode                            |
| Webhook đến trễ                           | Reconciliation xử lý                                |

---

## 7.8 Out of Scope

| Item                     | Reason                         |
| ------------------------ | ------------------------------ |
| SePay Live               | Không dùng tiền thật trong MVP |
| Refund thật              | Cần policy tài chính           |
| Partial payment phức tạp | Future                         |
| Multi-provider payment   | Future                         |
| Card payment             | Future                         |
| Auto settlement kế toán  | Future                         |

---

# 8. Module 5 — Offline Staff Mode

## 8.1 Module Overview

Offline Staff Mode cho phép Staff Console tiếp tục xử lý một số nghiệp vụ cốt lõi khi mất mạng. Đây là yêu cầu bắt buộc vì hầm TTTM dễ mất sóng hoặc rớt mạng nội bộ.

Offline Mode là degraded mode, không thay thế hoàn toàn online mode. Hệ thống phải có local cache, offline event queue, sync lại khi online và conflict handling.

---

## 8.2 Offline Operation Policy

| Operation            | Offline Support          | Notes                            |
| -------------------- | ------------------------ | -------------------------------- |
| Check-in             | Yes                      | Tạo offline event                |
| Check-out            | Limited                  | Cần local evidence/manual reason |
| Payment confirmation | No official confirmation | Chỉ xem cached/proof             |
| Merchant validation  | Not official             | Có thể draft/pending nếu cần     |
| Reservation creation | No                       | Cần online                       |
| Dashboard            | Limited                  | Last known data                  |
| Sync                 | Khi online lại           | Server quyết định final state    |

---

## 8.3 User Stories

| Story ID   | User Story                                                               | Priority  |
| ---------- | ------------------------------------------------------------------------ | --------- |
| US-OFF-001 | Là Parking Staff, tôi muốn biết hệ thống đang online hay offline         | Must Have |
| US-OFF-002 | Là Parking Staff, tôi muốn check-in xe khi mất mạng                      | Must Have |
| US-OFF-003 | Là Parking Staff, tôi muốn tạo offline check-out event khi đủ bằng chứng | Must Have |
| US-OFF-004 | Là Parking Staff, tôi muốn offline events tự sync khi có mạng            | Must Have |
| US-OFF-005 | Là Admin, tôi muốn xem các event pending sync/conflict                   | Must Have |
| US-OFF-006 | Là hệ thống, tôi muốn sync idempotent để không ghi trùng                 | Must Have |

---

## 8.4 Functional Requirements

| FR ID      | Requirement                                                                 | Priority  |
| ---------- | --------------------------------------------------------------------------- | --------- |
| FR-OFF-001 | Staff Console phải hiển thị trạng thái Online/Offline/Syncing               | Must Have |
| FR-OFF-002 | Staff Console phải có local cache active sessions tối thiểu                 | Must Have |
| FR-OFF-003 | Khi offline, check-in tạo local temporary session/event                     | Must Have |
| FR-OFF-004 | Offline event phải có event_id/idempotency_key/device_id/staff_id/timestamp | Must Have |
| FR-OFF-005 | Offline event phải được lưu trong queue local                               | Must Have |
| FR-OFF-006 | Khi online lại, hệ thống phải sync offline queue lên Parking Service        | Must Have |
| FR-OFF-007 | Server phải xử lý duplicate offline event an toàn                           | Must Have |
| FR-OFF-008 | Server phải trả trạng thái SYNCED/REJECTED/CONFLICT                         | Must Have |
| FR-OFF-009 | Dashboard phải hiển thị pending sync/conflict                               | Must Have |
| FR-OFF-010 | Offline check-out nếu thiếu bằng chứng phải yêu cầu manual reason           | Must Have |
| FR-OFF-011 | Merchant validation offline không được chính thức áp discount               | Must Have |

---

## 8.5 Acceptance Criteria

| AC ID      | Acceptance Criteria                                          |
| ---------- | ------------------------------------------------------------ |
| AC-OFF-001 | Khi mất mạng, Staff Console chuyển trạng thái Offline        |
| AC-OFF-002 | Staff tạo offline check-in event thành công                  |
| AC-OFF-003 | Offline event được lưu trong local queue                     |
| AC-OFF-004 | Khi online lại, queue được sync lên server                   |
| AC-OFF-005 | Sync thành công tạo official session hoặc update session     |
| AC-OFF-006 | Duplicate offline event không tạo dữ liệu trùng              |
| AC-OFF-007 | Conflict được hiển thị trong dashboard hoặc staff console    |
| AC-OFF-008 | Offline check-out thiếu payment proof bắt buộc manual reason |
| AC-OFF-009 | Server là source of truth sau khi sync                       |

---

## 8.6 Edge Cases

| Edge Case                                                | Expected Handling                              |
| -------------------------------------------------------- | ---------------------------------------------- |
| Offline check-in trùng biển số với online active session | Mark CONFLICT                                  |
| Offline event sync hai lần                               | Idempotent, không ghi trùng                    |
| Staff đóng browser trước khi sync                        | Queue vẫn còn nếu local storage còn            |
| Local cache quá cũ                                       | Hiển thị warning                               |
| Offline check-out khi không có session cache             | Manual review required                         |
| Offline check-out session chưa thanh toán                | Không auto exit; manual override reason        |
| Online lại nhưng server reject event                     | Hiển thị REJECTED/CONFLICT                     |
| Device time sai                                          | Server ghi nhận và cảnh báo timestamp mismatch |
| Merchant cố validate invoice offline                     | Không apply chính thức                         |

---

## 8.7 Out of Scope

| Item                                   | Reason                               |
| -------------------------------------- | ------------------------------------ |
| Full offline database replication      | Quá phức tạp                         |
| Offline payment confirmation thật      | Không đáng tin khi không có provider |
| Offline merchant validation chính thức | Rủi ro gian lận                      |
| Offline reservation creation           | Cần slot/payment realtime            |
| Multi-device sync offline nâng cao     | Future                               |

---

# 9. Module 6 — Merchant Invoice Aggregation

## 9.1 Module Overview

Merchant Invoice Aggregation cho phép Merchant Staff xác nhận một hoặc nhiều hóa đơn mua hàng của khách để áp giảm phí gửi xe. Mặc định MVP dùng policy `AGGREGATE_INVOICE`, tức là nhiều hóa đơn hợp lệ có thể cộng dồn cho một parking session.

---

## 9.2 Discount Policy

Default MVP:

```text
AGGREGATE_INVOICE
```

Ví dụ:

| Invoice |      Amount |
| ------- | ----------: |
| Store A | 120,000 VND |
| Store B | 180,000 VND |
| Total   | 300,000 VND |

Nếu ngưỡng là 300,000 VND, session đủ điều kiện giảm phí.

---

## 9.3 User Stories

| Story ID   | User Story                                                                  | Priority    |
| ---------- | --------------------------------------------------------------------------- | ----------- |
| US-MER-001 | Là Merchant Staff, tôi muốn quét QR vé xe của khách                         | Must Have   |
| US-MER-002 | Là Merchant Staff, tôi muốn nhập mã hóa đơn và số tiền hóa đơn              | Must Have   |
| US-MER-003 | Là Merchant Staff, tôi muốn hệ thống cộng dồn nhiều hóa đơn cho một session | Must Have   |
| US-MER-004 | Là Customer, tôi muốn thấy discount được cập nhật trên ticket page          | Must Have   |
| US-MER-005 | Là Admin, tôi muốn ngăn một hóa đơn bị dùng nhiều lần                       | Must Have   |
| US-MER-006 | Là Admin, tôi muốn xem lịch sử validation theo merchant                     | Should Have |

---

## 9.4 Functional Requirements

| FR ID      | Requirement                                               | Priority    |
| ---------- | --------------------------------------------------------- | ----------- |
| FR-MER-001 | Merchant Staff phải đăng nhập trước khi validate invoice  | Must Have   |
| FR-MER-002 | Merchant Staff có thể scan QR Lookup Token để lấy session | Must Have   |
| FR-MER-003 | Merchant Staff có thể nhập invoice_code và invoice_amount | Must Have   |
| FR-MER-004 | Hệ thống phải kiểm tra session chưa EXITED                | Must Have   |
| FR-MER-005 | Hệ thống phải kiểm tra invoice_code chưa được dùng        | Must Have   |
| FR-MER-006 | Hệ thống phải gắn invoice với tenant/merchant             | Must Have   |
| FR-MER-007 | Hệ thống phải cộng dồn invoice_amount hợp lệ theo session | Must Have   |
| FR-MER-008 | Hệ thống phải tính discount theo tổng invoice eligible    | Must Have   |
| FR-MER-009 | Hệ thống không được cho discount vượt quá total_fee       | Must Have   |
| FR-MER-010 | Hệ thống phải audit merchant validation                   | Must Have   |
| FR-MER-011 | Dashboard/Admin phải xem được validation history          | Should Have |
| FR-MER-012 | Merchant validation chính thức chỉ hoạt động online       | Must Have   |

---

## 9.5 Acceptance Criteria

| AC ID      | Acceptance Criteria                                                         |
| ---------- | --------------------------------------------------------------------------- |
| AC-MER-001 | Merchant Staff scan QR hợp lệ thì thấy session summary tối thiểu            |
| AC-MER-002 | Nhập hóa đơn hợp lệ thì invoice được thêm vào session                       |
| AC-MER-003 | Nhiều hóa đơn hợp lệ được cộng dồn đúng                                     |
| AC-MER-004 | Khi tổng hóa đơn đạt ngưỡng, discount được apply                            |
| AC-MER-005 | Ticket Page cập nhật discount/final_fee                                     |
| AC-MER-006 | Invoice trùng bị reject                                                     |
| AC-MER-007 | Merchant Staff không validate được invoice cho tenant khác nếu rule yêu cầu |
| AC-MER-008 | Validation sau khi session EXITED bị reject                                 |
| AC-MER-009 | Mọi validation có audit log                                                 |

---

## 9.6 Edge Cases

| Edge Case                                                  | Expected Handling                                 |
| ---------------------------------------------------------- | ------------------------------------------------- |
| Invoice dưới ngưỡng nhưng cộng với invoice trước đủ ngưỡng | Apply discount sau aggregation                    |
| Invoice code trùng                                         | Reject và tạo alert                               |
| Invoice amount = 0 hoặc âm                                 | Reject                                            |
| Invoice nhập sai                                           | Có thể void/cancel nếu chưa used final, cần audit |
| Session đã EXITED                                          | Reject                                            |
| QR token invalid                                           | Reject                                            |
| Merchant offline                                           | Không apply chính thức                            |
| Tổng discount vượt fee                                     | final_fee = 0                                     |
| Merchant staff nhập invoice của store khác                 | Reject nếu không có quyền                         |
| Nhiều merchant cùng thêm invoice                           | Cho phép nếu policy AGGREGATE                     |

---

## 9.7 Out of Scope

| Item                         | Reason          |
| ---------------------------- | --------------- |
| Tích hợp POS thật            | Future          |
| OCR hóa đơn                  | Future          |
| Merchant settlement kế toán  | Future          |
| Merchant trả tiền thay khách | Future policy   |
| Refund discount sau khi exit | Future          |
| Offline official validation  | Rủi ro gian lận |

---

# 10. Module 7 — Reservation

## 10.1 Module Overview

Reservation module cho phép khách đặt lịch gửi xe trước, giữ slot tạm thời, thanh toán qua simulation/test mode và check-in trong khung giờ hợp lệ. Reservation có rủi ro distributed transaction vì liên quan slot hold, payment và parking session, nên cần trạng thái pending và reconciliation.

---

## 10.2 Reservation Status

| Status                 | Description                                 |
| ---------------------- | ------------------------------------------- |
| DRAFT                  | Khách đang tạo reservation                  |
| PENDING_PAYMENT        | Đã giữ slot, chờ payment                    |
| CONFIRMED              | Payment thành công, reservation hợp lệ      |
| CHECKED_IN             | Reservation đã chuyển thành parking session |
| EXPIRED                | Không thanh toán hoặc quá hạn               |
| NO_SHOW                | Khách không đến trong check-in window       |
| CANCELLED              | Khách/staff hủy                             |
| PENDING_RECONCILIATION | Payment/reservation bị lệch trạng thái      |

---

## 10.3 User Stories

| Story ID   | User Story                                                                                | Priority    |
| ---------- | ----------------------------------------------------------------------------------------- | ----------- |
| US-RES-001 | Là Customer, tôi muốn tạo reservation bằng biển số và giờ đến                             | Should Have |
| US-RES-002 | Là Customer, tôi muốn hệ thống giữ slot tạm thời khi tôi thanh toán                       | Should Have |
| US-RES-003 | Là Customer, tôi muốn nhận reservation QR sau khi confirmed                               | Should Have |
| US-RES-004 | Là Parking Staff, tôi muốn check-in khách có reservation hợp lệ                           | Should Have |
| US-RES-005 | Là hệ thống, tôi muốn release slot nếu customer không thanh toán                          | Should Have |
| US-RES-006 | Là hệ thống, tôi muốn reconciliation khi payment success nhưng reservation chưa confirmed | Should Have |
| US-RES-007 | Là Admin, tôi muốn thấy reservation pending/no-show/expired                               | Could Have  |

---

## 10.4 Functional Requirements

| FR ID      | Requirement                                                                         | Priority    |
| ---------- | ----------------------------------------------------------------------------------- | ----------- |
| FR-RES-001 | Customer có thể tạo reservation với plate, vehicle_type, reserved_time              | Should Have |
| FR-RES-002 | Hệ thống phải kiểm tra slot/capacity trước khi tạo reservation                      | Should Have |
| FR-RES-003 | Hệ thống phải tạo slot hold tạm thời                                                | Should Have |
| FR-RES-004 | Slot hold phải có expiration time                                                   | Should Have |
| FR-RES-005 | Reservation chuyển PENDING_PAYMENT khi tạo payment order                            | Should Have |
| FR-RES-006 | Reservation chỉ CONFIRMED khi payment PAID và slot hold còn hợp lệ                  | Should Have |
| FR-RES-007 | Nếu payment fail/timeout, slot hold phải release                                    | Should Have |
| FR-RES-008 | Reservation có check-in window                                                      | Should Have |
| FR-RES-009 | Staff có thể chuyển reservation CONFIRMED thành parking session khi check-in hợp lệ | Should Have |
| FR-RES-010 | Reservation quá hạn không check-in chuyển NO_SHOW/EXPIRED                           | Could Have  |
| FR-RES-011 | Payment success nhưng confirm fail phải chuyển PENDING_RECONCILIATION               | Should Have |
| FR-RES-012 | Reconciliation job phải xử lý reservation pending                                   | Should Have |

---

## 10.5 Acceptance Criteria

| AC ID      | Acceptance Criteria                                                      |
| ---------- | ------------------------------------------------------------------------ |
| AC-RES-001 | Customer tạo reservation hợp lệ thì status PENDING_PAYMENT               |
| AC-RES-002 | Slot hold được tạo với expiration time                                   |
| AC-RES-003 | Payment simulation success chuyển reservation CONFIRMED nếu hold còn hạn |
| AC-RES-004 | Nếu payment timeout, reservation EXPIRED và slot release                 |
| AC-RES-005 | Staff check-in trong check-in window thì tạo parking session             |
| AC-RES-006 | Reservation CHECKED_IN không thể check-in lần nữa                        |
| AC-RES-007 | Payment success nhưng confirm lỗi thì reservation PENDING_RECONCILIATION |
| AC-RES-008 | Reconciliation job có thể xử lý pending reservation                      |
| AC-RES-009 | Reservation QR không trực tiếp mở cổng nếu chưa tạo parking session      |

---

## 10.6 Edge Cases

| Edge Case                                   | Expected Handling                                       |
| ------------------------------------------- | ------------------------------------------------------- |
| Customer đến sớm                            | Báo chưa đến check-in window hoặc staff manual override |
| Customer đến trễ                            | EXPIRED/NO_SHOW hoặc chuyển gửi xe thường theo policy   |
| Slot hold hết hạn nhưng payment đến trễ     | PENDING_RECONCILIATION/manual review                    |
| Reservation đã CHECKED_IN                   | Không cho check-in lại                                  |
| Plate khác reservation                      | Staff review/manual confirm                             |
| Payment duplicate                           | Idempotent                                              |
| Reservation service lỗi sau payment success | PENDING_RECONCILIATION                                  |
| Customer hủy reservation                    | MVP có thể chỉ mark CANCELLED nếu chưa paid             |
| Mất mạng khi check-in reservation           | Chỉ hỗ trợ nếu reservation cached, hoặc manual flow     |

---

## 10.7 Out of Scope

| Item                            | Reason                  |
| ------------------------------- | ----------------------- |
| Refund thật khi hủy reservation | Future                  |
| Deposit policy phức tạp         | Future                  |
| Chọn slot chính xác từng ô      | Không có sensor         |
| Recurring reservation           | Future                  |
| Reservation marketplace         | Không thuộc MVP         |
| Reservation offline creation    | Cần online slot/payment |

---

# 11. Module 8 — Dashboard

## 11.1 Module Overview

Dashboard giúp Admin/Mall Manager theo dõi tình trạng vận hành: xe đang gửi, doanh thu, payment, validation, reservation, offline sync, suspicious cases và conflict. Dashboard không được chỉ hiển thị số đẹp; phải phân biệt dữ liệu confirmed và pending.

---

## 11.2 User Stories

| Story ID    | User Story                                                     | Priority    |
| ----------- | -------------------------------------------------------------- | ----------- |
| US-DASH-001 | Là Admin, tôi muốn xem số xe đang gửi                          | Must Have   |
| US-DASH-002 | Là Admin, tôi muốn xem doanh thu hôm nay                       | Must Have   |
| US-DASH-003 | Là Admin, tôi muốn xem payment pending/mismatched              | Must Have   |
| US-DASH-004 | Là Admin, tôi muốn xem merchant validations                    | Must Have   |
| US-DASH-005 | Là Admin, tôi muốn xem offline pending sync/conflict           | Must Have   |
| US-DASH-006 | Là Admin, tôi muốn xem suspicious sessions                     | Must Have   |
| US-DASH-007 | Là Admin, tôi muốn xem reservation status                      | Should Have |
| US-DASH-008 | Là Parking Staff, tôi muốn xem danh sách session active cơ bản | Should Have |

---

## 11.3 Functional Requirements

| FR ID       | Requirement                                                            | Priority    |
| ----------- | ---------------------------------------------------------------------- | ----------- |
| FR-DASH-001 | Dashboard phải hiển thị tổng số active sessions                        | Must Have   |
| FR-DASH-002 | Dashboard phải hiển thị số session EXITED trong ngày                   | Must Have   |
| FR-DASH-003 | Dashboard phải hiển thị doanh thu confirmed trong ngày                 | Must Have   |
| FR-DASH-004 | Dashboard phải hiển thị payment pending/mismatched                     | Must Have   |
| FR-DASH-005 | Dashboard phải hiển thị số lượt merchant validation                    | Must Have   |
| FR-DASH-006 | Dashboard phải hiển thị discount total                                 | Should Have |
| FR-DASH-007 | Dashboard phải hiển thị offline events pending/conflict                | Must Have   |
| FR-DASH-008 | Dashboard phải hiển thị suspicious sessions                            | Must Have   |
| FR-DASH-009 | Dashboard phải phân biệt confirmed data và pending data                | Must Have   |
| FR-DASH-010 | Dashboard phải có danh sách session gần đây                            | Should Have |
| FR-DASH-011 | Dashboard phải có filter theo ngày/trạng thái cơ bản                   | Should Have |
| FR-DASH-012 | Dashboard phải hiển thị reservation summary nếu Reservation module bật | Should Have |

---

## 11.4 Acceptance Criteria

| AC ID       | Acceptance Criteria                                          |
| ----------- | ------------------------------------------------------------ |
| AC-DASH-001 | Admin login thấy dashboard                                   |
| AC-DASH-002 | Dashboard hiển thị active sessions đúng theo dữ liệu test    |
| AC-DASH-003 | Payment pending/mismatched xuất hiện rõ                      |
| AC-DASH-004 | Offline pending sync/conflict không bị ẩn                    |
| AC-DASH-005 | Suspicious session được highlight                            |
| AC-DASH-006 | Merchant validation count cập nhật sau khi validate invoice  |
| AC-DASH-007 | Doanh thu không tính payment MISMATCHED                      |
| AC-DASH-008 | Dashboard không cho Merchant Staff xem dữ liệu toàn hệ thống |

---

## 11.5 Edge Cases

| Edge Case                              | Expected Handling                                         |
| -------------------------------------- | --------------------------------------------------------- |
| Offline events chưa sync               | Hiển thị pending, không cộng vào confirmed nếu chưa xử lý |
| Payment PAID nhưng session chưa update | Hiển thị pending reconciliation                           |
| Duplicate webhook                      | Không double revenue                                      |
| Merchant validation bị reject          | Không cộng discount                                       |
| Dashboard service lỗi                  | Frontend hiển thị lỗi rõ                                  |
| Không có dữ liệu                       | Hiển thị empty state                                      |
| User không đủ quyền                    | 403 hoặc redirect                                         |
| Reporting data lệch                    | Ưu tiên source-of-truth status                            |

---

## 11.6 Out of Scope

| Item                        | Reason                  |
| --------------------------- | ----------------------- |
| BI dashboard nâng cao       | Future                  |
| Export Excel/PDF            | Could Have sau          |
| Realtime WebSocket bắt buộc | MVP có thể dùng polling |
| Predictive analytics        | Future                  |
| Multi-branch reporting      | Future                  |

---

# 12. Module 9 — OCR

## 12.1 Module Overview

OCR module nhận ảnh biển số và trả về biển số gợi ý kèm confidence. OCR không phải nguồn quyết định cuối cùng. Parking Staff phải xác nhận hoặc sửa biển số trước khi hệ thống dùng cho nghiệp vụ.

---

## 12.2 User Stories

| Story ID   | User Story                                                      | Priority    |
| ---------- | --------------------------------------------------------------- | ----------- |
| US-OCR-001 | Là Parking Staff, tôi muốn upload/chụp ảnh biển số để OCR gợi ý | Should Have |
| US-OCR-002 | Là Parking Staff, tôi muốn sửa biển số nếu OCR sai              | Must Have   |
| US-OCR-003 | Là Parking Staff, tôi muốn thấy confidence của OCR              | Should Have |
| US-OCR-004 | Là hệ thống, tôi muốn fallback nhập thủ công nếu OCR lỗi        | Must Have   |
| US-OCR-005 | Là Admin, tôi muốn biết tỷ lệ OCR confidence thấp               | Could Have  |

---

## 12.3 Functional Requirements

| FR ID      | Requirement                                                       | Priority    |
| ---------- | ----------------------------------------------------------------- | ----------- |
| FR-OCR-001 | Vision Service phải nhận ảnh từ frontend/backend                  | Should Have |
| FR-OCR-002 | Vision Service phải trả plate candidate                           | Should Have |
| FR-OCR-003 | Vision Service phải trả confidence score                          | Should Have |
| FR-OCR-004 | Vision Service phải trả processing time nếu có                    | Could Have  |
| FR-OCR-005 | Staff Console phải cho staff xác nhận/sửa plate                   | Must Have   |
| FR-OCR-006 | Nếu OCR fail/timeout, staff vẫn nhập thủ công được                | Must Have   |
| FR-OCR-007 | OCR result không tự động tạo session nếu chưa được staff xác nhận | Must Have   |
| FR-OCR-008 | Hệ thống phải lưu plate source: OCR/MANUAL/CORRECTED              | Should Have |
| FR-OCR-009 | Plate correction phải audit nếu session đã tạo                    | Should Have |

---

## 12.4 Acceptance Criteria

| AC ID      | Acceptance Criteria                                        |
| ---------- | ---------------------------------------------------------- |
| AC-OCR-001 | Upload ảnh hợp lệ trả về plate candidate                   |
| AC-OCR-002 | Staff có thể sửa plate trước khi check-in                  |
| AC-OCR-003 | OCR fail không chặn check-in thủ công                      |
| AC-OCR-004 | Low confidence hiển thị cảnh báo cần xác nhận              |
| AC-OCR-005 | Plate cuối cùng dùng trong session là plate staff xác nhận |
| AC-OCR-006 | OCR service lỗi không làm sập Parking Service              |
| AC-OCR-007 | Nếu có correction sau session tạo, audit log được ghi      |

---

## 12.5 Edge Cases

| Edge Case                | Expected Handling                                 |
| ------------------------ | ------------------------------------------------- |
| Ảnh mờ                   | Trả low confidence hoặc fail, staff nhập thủ công |
| Ảnh không có biển số     | Fail rõ ràng                                      |
| Biển số 2 dòng           | OCR cố gắng normalize, staff xác nhận             |
| Ký tự dễ nhầm            | Staff sửa                                         |
| OCR service timeout      | Fallback manual                                   |
| File quá lớn             | Reject hoặc compress                              |
| Unsupported file type    | Reject                                            |
| OCR trả nhiều candidates | Staff chọn một hoặc nhập thủ công                 |

---

## 12.6 Out of Scope

| Item                          | Reason                |
| ----------------------------- | --------------------- |
| OCR accuracy production-grade | Cần dữ liệu và tuning |
| Realtime continuous video OCR | Quá nặng cho MVP      |
| Tự động mở cổng bằng OCR      | Rủi ro cao            |
| Training model mới            | Không thuộc MVP       |
| OCR hóa đơn                   | Future                |

---

# 13. Module 10 — Audit / Fraud

## 13.1 Module Overview

Audit/Fraud module ghi lại thao tác nhạy cảm và phát hiện một số tình huống bất thường. Đây là module bắt buộc vì hệ thống liên quan đến tài sản, tiền, QR, hóa đơn và thao tác nhân viên.

---

## 13.2 Fraud / Suspicious Cases

| Case ID | Case                           | Expected Result        |
| ------- | ------------------------------ | ---------------------- |
| F-001   | QR/Exit Pass dùng lại          | Reject, log            |
| F-002   | Exit Pass hết hạn              | Reject                 |
| F-003   | Plate mismatch                 | SUSPICIOUS             |
| F-004   | Invoice duplicate              | Reject, alert          |
| F-005   | Payment mismatched             | PENDING_RECONCILIATION |
| F-006   | Manual override nhiều lần      | Alert                  |
| F-007   | Offline sync conflict          | Conflict queue         |
| F-008   | Session active quá lâu         | Dashboard warning      |
| F-009   | Merchant validation bất thường | Admin review           |
| F-010   | Payment duplicate webhook      | Ignore duplicate, log  |

---

## 13.3 User Stories

| Story ID   | User Story                                           | Priority  |
| ---------- | ---------------------------------------------------- | --------- |
| US-AUD-001 | Là Admin, tôi muốn xem audit log thao tác quan trọng | Must Have |
| US-AUD-002 | Là hệ thống, tôi muốn log manual override            | Must Have |
| US-AUD-003 | Là hệ thống, tôi muốn cảnh báo plate mismatch        | Must Have |
| US-AUD-004 | Là hệ thống, tôi muốn reject invoice duplicate       | Must Have |
| US-AUD-005 | Là Admin, tôi muốn xem suspicious sessions           | Must Have |
| US-AUD-006 | Là QA, tôi muốn audit/fraud rule có test case rõ     | Must Have |

---

## 13.4 Functional Requirements

| FR ID      | Requirement                                           | Priority    |
| ---------- | ----------------------------------------------------- | ----------- |
| FR-AUD-001 | Hệ thống phải log check-in                            | Must Have   |
| FR-AUD-002 | Hệ thống phải log check-out                           | Must Have   |
| FR-AUD-003 | Hệ thống phải log payment status update               | Must Have   |
| FR-AUD-004 | Hệ thống phải log merchant validation                 | Must Have   |
| FR-AUD-005 | Hệ thống phải log manual override với reason          | Must Have   |
| FR-AUD-006 | Hệ thống phải log plate edit/correction               | Should Have |
| FR-AUD-007 | Hệ thống phải log offline sync result                 | Must Have   |
| FR-AUD-008 | Hệ thống phải tạo suspicious alert cho plate mismatch | Must Have   |
| FR-AUD-009 | Hệ thống phải tạo alert cho invoice duplicate         | Must Have   |
| FR-AUD-010 | Admin Dashboard phải hiển thị suspicious cases        | Must Have   |
| FR-AUD-011 | Audit log không được sửa/xóa tùy tiện qua UI MVP      | Must Have   |

---

## 13.5 Acceptance Criteria

| AC ID      | Acceptance Criteria                                |
| ---------- | -------------------------------------------------- |
| AC-AUD-001 | Check-in tạo audit log                             |
| AC-AUD-002 | Check-out tạo audit log                            |
| AC-AUD-003 | Manual override không có reason thì bị reject      |
| AC-AUD-004 | Manual override có reason thì được log             |
| AC-AUD-005 | Plate mismatch tạo suspicious alert                |
| AC-AUD-006 | Invoice duplicate tạo alert hoặc log fraud attempt |
| AC-AUD-007 | Payment mismatched hiển thị pending reconciliation |
| AC-AUD-008 | Admin xem được danh sách suspicious cases          |
| AC-AUD-009 | User không đủ quyền không xem được audit log       |

---

## 13.6 Edge Cases

| Edge Case                             | Expected Handling                       |
| ------------------------------------- | --------------------------------------- |
| Staff cố manual override không reason | Reject                                  |
| Staff sửa plate sau payment           | Require reason, audit                   |
| Merchant nhập invoice trùng           | Reject, alert                           |
| Duplicate webhook                     | Audit duplicate ignored                 |
| Offline sync conflict                 | Log conflict                            |
| Admin muốn xóa audit log              | Không hỗ trợ trong MVP                  |
| Suspicious case đã review             | Có thể mark reviewed nếu scope cho phép |
| Fraud alert trùng                     | Có thể group hoặc ignore duplicate      |

---

## 13.7 Out of Scope

| Item                      | Reason |
| ------------------------- | ------ |
| AI fraud detection        | Future |
| Advanced risk scoring     | Future |
| SIEM integration          | Future |
| Legal-grade immutable log | Future |
| Video evidence management | Future |

---

# 14. Cross-Module Requirements

## 14.1 API Contract Requirements

| Requirement                                          |
| ---------------------------------------------------- |
| Mọi API mới phải được ghi vào `docs/API_CONTRACT.md` |
| API lỗi phải trả errorCode rõ ràng                   |
| Protected API phải enforce role                      |
| Public ticket API chỉ nhận opaque token              |
| Payment/offline sync API phải hỗ trợ idempotency     |
| API không được expose dữ liệu nhạy cảm quá mức       |

---

## 14.2 Database Requirements

| Requirement                                                                           |
| ------------------------------------------------------------------------------------- |
| Mọi bảng mới phải được ghi vào `docs/DATABASE_SCHEMA.md`                              |
| Mỗi service dùng schema riêng trong MVP                                               |
| Không service nào tự ý modify schema của service khác                                 |
| Payment transaction, offline event, invoice validation phải có unique key/idempotency |
| Audit log phải lưu actor, action, target, timestamp, reason nếu có                    |
| Dữ liệu pending/conflict phải có status rõ ràng                                       |

---

## 14.3 Documentation Requirements

| Requirement                                                        |
| ------------------------------------------------------------------ |
| Khi thêm/sửa API, update `API_CONTRACT.md`                         |
| Khi thêm/sửa schema, update `DATABASE_SCHEMA.md`                   |
| Khi thay đổi rule, update `BUSINESS_RULES.md` và `DECISION_LOG.md` |
| Khi thêm test case, update `TEST_MATRIX.md`                        |
| Nếu Agent gặp ambiguity, phải ghi assumption vào `DECISION_LOG.md` |

---

# 15. Non-Functional Product Requirements

NFR chi tiết nằm trong:

```text
docs/NFR.md
```

PRD yêu cầu các module tuân thủ tối thiểu:

| Category         | Requirement                                                            |
| ---------------- | ---------------------------------------------------------------------- |
| Performance      | Check-in/check-out online target dưới 10 giây thao tác tổng trong demo |
| Security         | QR Lookup không mở cổng, Exit Pass ngắn hạn                            |
| Availability     | Offline Staff Mode phải có queue và sync                               |
| Data Consistency | Payment và offline sync phải idempotent                                |
| Usability        | Customer no-login, merchant flow không quá 3 bước chính                |
| Observability    | Audit log và dashboard pending state bắt buộc                          |
| Resilience       | OCR/payment/dashboard lỗi không làm sập toàn hệ thống                  |

---

# 16. Vertical Slice Mapping

| Slice    | Modules Involved            | Main Outcome                                      |
| -------- | --------------------------- | ------------------------------------------------- |
| Slice 0  | Repository Foundation                    | Folders, docs, env, Docker placeholder; no business logic |
| Slice 1  | API Gateway + Identity Skeleton          | Gateway/identity skeleton, login placeholder, roles, health checks |
| Slice 2  | Parking Session + QR Lookup              | Check-in draft, QR Lookup Token, public lookup; no exit authorization |
| Slice 3  | Customer Ticket Page                     | Ticket summary, fee estimate, and payment status; no payment logic |
| Slice 4  | Payment Simulation                       | Payment order, simulation success, `Idempotency-Key`, payment status update |
| Slice 5  | Dynamic Exit Pass + Check-out            | One-time Exit Pass, plate match, grace period, secure check-out |
| Slice 6  | Offline Staff Mode                      | Local cache/queue, sync contract, conflict state |
| Slice 7  | Merchant Invoice Aggregation             | Aggregate invoices, duplicate rejection, discount recalculation |
| Slice 8  | Payment Reconciliation                   | Pending/mismatched payment and session consistency |
| Slice 9  | Reservation Basic                        | Slot hold, payment state, check-in window, `PENDING_RECONCILIATION` |
| Slice 10 | OCR Assist                               | Vision contract, candidate/confidence, staff confirmation/correction |
| Slice 11 | Dashboard                                | Confirmed revenue, pending/mismatched, sync, suspicious/conflict states |
| Slice 12 | Docker + Deploy Hardening                | Docker completion, Railway/Supabase/hybrid docs, demo readiness |

---

# 17. Product Acceptance Summary

MVP được xem là đạt yêu cầu sản phẩm khi:

| Acceptance Item                                      | Status Needed |
| ---------------------------------------------------- | ------------- |
| Staff có thể check-in xe và tạo QR Lookup            | Required      |
| Customer mở QR và xem ticket                         | Required      |
| Payment Simulation hoạt động và idempotent           | Required      |
| QR Lookup không thể mở cổng                          | Required      |
| Dynamic Exit Pass được tạo sau payment               | Required      |
| Staff check-out bằng Exit Pass + plate match         | Required      |
| Offline Staff Mode tạo event và sync được            | Required      |
| Merchant thêm nhiều hóa đơn và discount đúng         | Required      |
| Dashboard hiển thị active/payment/pending/suspicious | Required      |
| Audit log cho thao tác nhạy cảm                      | Required      |
| OCR fail không chặn nhập thủ công                    | Required      |
| SePay Live không được dùng trong MVP                 | Required      |
| Reservation có basic flow nếu scope Sprint cho phép  | Should Have   |

---

# 18. Open Product Questions

Only non-blocking questions remain; canonical MVP priorities and safety rules are already accepted.

| Question ID | Question | Priority |
|---|---|---|
| Q-PRD-008 | Should OCR start with upload or camera frames? | Medium |
| Q-PRD-009 | Is dashboard export needed in a later hardening phase? | Low |
| Q-PRD-010 | Should customer accounts be added for a future reservation phase? | Medium |

---

# 19. PRD v2 Definition of Done

| Checklist Item                                                        | Status |
| --------------------------------------------------------------------- | ------ |
| Module Identity có story, FR, AC, edge case, out-of-scope             | Done   |
| Module Parking Session có story, FR, AC, edge case, out-of-scope      | Done   |
| Module QR Security có story, FR, AC, edge case, out-of-scope          | Done   |
| Module Payment có story, FR, AC, edge case, out-of-scope              | Done   |
| Module Offline Staff Mode có story, FR, AC, edge case, out-of-scope   | Done   |
| Module Merchant Aggregation có story, FR, AC, edge case, out-of-scope | Done   |
| Module Reservation có story, FR, AC, edge case, out-of-scope          | Done   |
| Module Dashboard có story, FR, AC, edge case, out-of-scope            | Done   |
| Module OCR có story, FR, AC, edge case, out-of-scope                  | Done   |
| Module Audit/Fraud có story, FR, AC, edge case, out-of-scope          | Done   |
| Có mapping sang vertical slices                                       | Done   |
| Có cross-module requirements                                          | Done   |
| Có Open Questions                                                     | Done   |

---

# 20. Instruction for Agent Codex

Agent Codex must treat this PRD as a product source of truth together with:

```text
docs/BRD.md
docs/SAD.md
docs/BUSINESS_RULES.md
docs/API_CONTRACT.md
docs/DATABASE_SCHEMA.md
docs/TEST_MATRIX.md
agent/TASKS.md
agent/VERTICAL_SLICE_ROADMAP.md
```

Agent Codex must not implement features outside this PRD unless a new decision is added to:

```text
docs/DECISION_LOG.md
```

Agent Codex must implement features by vertical slice, not by building the whole system at once.
