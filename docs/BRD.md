# Business Requirement Document — BRD v2

## Project Name

**ParkFlow Mall — Smart Parking & Reservation Management System**

---

## Version History

| Version | Change Summary                                                                                                                                       |
| ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| v1      | BRD ban đầu: QR parking, OCR, SePay, merchant validation, reservation, dashboard, microservice                                                       |
| v2      | Bổ sung offline mode, NFR, QR security, distributed transaction, reconciliation, invoice aggregation, deploy decision và payment simulation strategy |

---

## Executive Summary

ParkFlow Mall là hệ thống quản lý giữ xe thông minh dành cho hầm giữ xe tại trung tâm thương mại, tập trung vào việc giảm ùn tắc tại cổng, tăng tốc độ xử lý xe vào/ra, hỗ trợ thanh toán số, đặt chỗ trước, giảm phí theo hóa đơn mua hàng và nâng cao năng lực vận hành của ban quản lý.

Phiên bản BRD v2 điều chỉnh dự án theo hướng thực tế hơn cho môi trường TTTM, nơi mạng nội bộ, 4G hoặc kết nối cloud có thể không ổn định. Vì vậy, hệ thống không chỉ cần chạy được trong trạng thái online, mà còn phải có cơ chế **Offline Mode**, **local cache**, **offline queue**, **manual fallback**, **sync sau khi có mạng** và **audit log** để đảm bảo bãi xe vẫn vận hành được khi mất mạng.

Dự án triển khai theo mô hình microservice, gồm các domain chính: Identity, Parking, Reservation, Payment, Merchant Validation, Vision/OCR, Reporting và API Gateway. Backend sử dụng Java Spring Boot, frontend sử dụng React, OCR có thể tách thành Python/FastAPI service, database/storage ưu tiên Supabase PostgreSQL/Supabase Storage, đóng gói bằng Docker và deploy chi phí thấp qua Railway hoặc phương án hybrid local/cloud.

Trong MVP, hệ thống cần làm thật các phần: QR ticket, OCR hỗ trợ nhận diện biển số, payment simulation, SePay test integration, merchant invoice validation, reservation, dashboard, audit log, fraud alert và offline fallback cơ bản. Các phần như barrier vật lý, cảm biến từng ô đỗ, POS thật và hoàn tiền thật không nằm trong MVP.

---

# 1. Vision and Objectives

## 1.1 Product Vision

Trở thành nền tảng quản lý giữ xe thông minh cho trung tâm thương mại, giúp khách gửi xe nhanh hơn, thanh toán thuận tiện hơn, cửa hàng xác nhận giảm phí dễ hơn và ban quản lý kiểm soát vận hành tốt hơn thông qua QR, nhận diện biển số, đặt chỗ trước, thanh toán số, dashboard realtime và cơ chế vận hành an toàn khi mất mạng.

---

## 1.2 Business Objectives

| ID      | Objective                                | Business Value                                | Measurement / KPI                                                            | Priority    |
| ------- | ---------------------------------------- | --------------------------------------------- | ---------------------------------------------------------------------------- | ----------- |
| OBJ-001 | Số hóa vòng đời gửi xe                   | Giảm phụ thuộc vé giấy/thẻ thủ công           | Hoàn thành flow check-in → QR → payment → check-out                          | Must Have   |
| OBJ-002 | Giảm thời gian xử lý tại cổng            | Giảm ùn tắc và tăng trải nghiệm khách         | Check-in/check-out hợp lệ dưới 10 giây trong demo                            | Must Have   |
| OBJ-003 | Hỗ trợ vận hành khi mất mạng             | Tránh bãi xe bị tê liệt khi cloud/network lỗi | Staff Console có offline queue và sync lại sau khi có mạng                   | Must Have   |
| OBJ-004 | Hỗ trợ thanh toán số an toàn             | Giảm tiền mặt, tăng khả năng đối soát         | Payment simulation hoạt động; SePay test mode xử lý được webhook             | Must Have   |
| OBJ-005 | Hỗ trợ giảm phí theo hóa đơn thực tế     | Phù hợp nghiệp vụ TTTM                        | Cho phép cấu hình cộng dồn nhiều hóa đơn hoặc yêu cầu một hóa đơn đạt ngưỡng | Must Have   |
| OBJ-006 | Tăng an toàn QR ticket                   | Giảm rủi ro chụp lén/dùng lại QR              | QR tra cứu và Exit Pass được tách quyền sử dụng                              | Must Have   |
| OBJ-007 | Hỗ trợ đặt chỗ trước                     | Tăng khả năng phục vụ khách có kế hoạch       | Reservation có trạng thái, payment, check-in window và reconciliation        | Should Have |
| OBJ-008 | Tạo nền tảng microservice có thể mở rộng | Phù hợp định hướng kỹ thuật và portfolio      | Domain service được tách rõ, deploy được bằng Docker                         | Should Have |

---

## 1.3 User Objectives

| ID     | User Objective                        | Description                                                                      | User Value                       |
| ------ | ------------------------------------- | -------------------------------------------------------------------------------- | -------------------------------- |
| UO-001 | Khách vào bãi nhanh                   | Không phải nhập thông tin phức tạp                                               | Giảm thời gian chờ               |
| UO-002 | Khách xem phí và trạng thái dễ dàng   | Quét QR để xem session, phí, giảm phí, payment                                   | Minh bạch                        |
| UO-003 | Khách thanh toán trước                | Thanh toán trước khi ra cổng                                                     | Ra bãi nhanh hơn                 |
| UO-004 | Khách vẫn được hỗ trợ khi mất mạng    | Nếu không mở được QR page, staff có thể tìm bằng biển số/local cache/manual flow | Không bị kẹt tại hầm             |
| UO-005 | Nhân viên xử lý ngoại lệ rõ ràng      | Có flow mất QR, sai biển số, chưa thanh toán, offline                            | Giảm tranh cãi                   |
| UO-006 | Merchant staff xác nhận hóa đơn nhanh | Quét QR, nhập hóa đơn, áp giảm phí                                               | Không cần giấy xác nhận thủ công |
| UO-007 | Admin kiểm soát vận hành              | Dashboard, audit log, report, fraud alert                                        | Minh bạch doanh thu và rủi ro    |

---

## 1.4 Success Metrics / KPI

| Metric                          | Definition                                                        | Target for MVP                             | Why It Matters                         |
| ------------------------------- | ----------------------------------------------------------------- | ------------------------------------------ | -------------------------------------- |
| Check-in Completion Rate        | % lượt check-in tạo session thành công                            | 95%+ trong test case                       | Đo độ ổn định core flow                |
| Average Check-in Time           | Thời gian từ lúc staff xác nhận plate đến khi tạo QR              | Dưới 10 giây                               | Giảm ùn tại cổng vào                   |
| Check-out Completion Rate       | % lượt check-out hợp lệ chuyển EXITED thành công                  | 95%+                                       | Đo end-to-end flow                     |
| Offline Operation Continuity    | Hệ thống vẫn xử lý được một số thao tác cốt lõi khi mất mạng      | Staff có thể tạo offline event và sync lại | Đảm bảo bãi xe không tê liệt           |
| Payment Recognition Time        | Thời gian hệ thống cập nhật payment sau webhook/test confirmation | Dưới 10 giây trong demo                    | Tránh khách thanh toán rồi vẫn bị chặn |
| Payment Reconciliation Accuracy | % payment pending được đối soát đúng sau job reconciliation       | 95%+ trong test case                       | Xử lý distributed transaction          |
| Merchant Validation Accuracy    | % hóa đơn được áp rule đúng                                       | 95%+                                       | Giảm thất thoát                        |
| QR Misuse Detection             | % case QR dùng lại/sai xe bị phát hiện                            | 80%+ trong test case                       | Tăng an toàn                           |
| Dashboard Freshness             | Thời gian dashboard cập nhật sau thay đổi nghiệp vụ               | Dưới 5 giây trong online mode              | Quản lý realtime                       |
| Sync Recovery Success           | % offline events được đồng bộ đúng sau khi có mạng                | 95%+ trong test case                       | Đảm bảo dữ liệu cuối cùng nhất quán    |

---

## 1.5 Strategic Alignment

ParkFlow Mall phù hợp với bài toán chuyển đổi số vận hành bãi xe tại TTTM, nơi trải nghiệm khách hàng, tốc độ vận hành, an toàn tài sản, thanh toán số và đối soát doanh thu đều quan trọng.

Dự án có tính khác biệt vì không chỉ dừng ở QR ticket, mà kết hợp nhiều nghiệp vụ thực tế: OCR biển số, QR security, offline fallback, merchant invoice aggregation, SePay payment, reservation, audit log, dashboard và reconciliation. Đây là nền tảng tốt để phát triển tiếp sang PRD và SAD.

---

# 2. Problem Statement

## 2.1 Current Situation

Hầm giữ xe TTTM thường là môi trường có tín hiệu mạng không ổn định. Khách có thể mất 4G khi xuống hầm, điện thoại không mở được QR Ticket Page, hoặc staff console không gọi được API cloud. Nếu hệ thống chỉ hoạt động khi online, bãi xe có thể bị tắc ngay tại cổng vào/ra.

Ngoài ra, quy trình giữ xe hiện tại thường có nhiều điểm rời rạc: vé/QR, camera biển số, thanh toán, hóa đơn giảm phí và dashboard vận hành không được kết nối thành một luồng khép kín. Điều này tạo ra nhiều rủi ro: khách đã thanh toán nhưng hệ thống chưa cập nhật, merchant xác nhận hóa đơn sai, QR bị chụp lén, reservation bị trừ tiền nhưng chưa được confirm, hoặc dữ liệu offline không đồng bộ lại đúng.

---

## 2.2 Target User Pain Points

| Pain Point ID | Pain Point                             | Description                                              | Impact                                         |
| ------------- | -------------------------------------- | -------------------------------------------------------- | ---------------------------------------------- |
| PP-001        | Mất mạng trong hầm                     | Khách/staff không truy cập được hệ thống cloud           | Xe bị kẹt, vận hành gián đoạn                  |
| PP-002        | QR tĩnh dễ bị lạm dụng                 | Người khác có thể chụp QR và dùng sai mục đích           | Rủi ro bảo mật                                 |
| PP-003        | Payment state không nhất quán          | SePay thành công nhưng Parking/Reservation chưa cập nhật | Khách đã trả tiền nhưng vẫn bị chặn            |
| PP-004        | Giảm phí hóa đơn chưa thực tế          | Một xe có thể có nhiều người mua ở nhiều cửa hàng        | Rule một hóa đơn duy nhất có thể không phù hợp |
| PP-005        | Staff không có offline fallback        | Khi console rớt mạng, không biết có cho xe ra không      | Tắc cổng hoặc mở cổng thiếu kiểm soát          |
| PP-006        | Không có NFR rõ                        | Không biết hệ thống cần chịu tải/độ trễ/khả dụng ra sao  | Khó thiết kế SAD và test                       |
| PP-007        | Reservation có distributed transaction | Giữ slot và thanh toán nằm ở nhiều service               | Dễ lệch trạng thái                             |
| PP-008        | Merchant validation có rủi ro gian lận | Hóa đơn trùng, cộng dồn sai, staff xác nhận sai          | Thất thoát doanh thu                           |
| PP-009        | Dashboard có thể lệch số liệu          | Offline events chưa sync hoặc webhook pending            | Quản lý không tin dữ liệu                      |
| PP-010        | Khách không muốn thao tác nhiều        | Nếu security quá phức tạp sẽ làm chậm trải nghiệm        | Giảm adoption                                  |

---

## 2.3 Root Cause Analysis

| Symptom                                            | Possible Root Cause                            | Business Implication                       |
| -------------------------------------------------- | ---------------------------------------------- | ------------------------------------------ |
| Xe kẹt tại cổng khi mất mạng                       | Hệ thống phụ thuộc API cloud 100%              | Cần offline mode/local fallback            |
| Khách đã trả tiền nhưng vẫn chưa ra được           | Webhook chưa tới hoặc service xử lý lỗi        | Cần reconciliation và pending-payment flow |
| QR bị chụp lén                                     | QR token tĩnh có quyền quá lớn                 | Cần tách Lookup QR và Exit Pass            |
| Merchant validation không phản ánh thực tế mua sắm | Rule chỉ hỗ trợ một hóa đơn                    | Cần invoice aggregation policy             |
| Reservation bị lệch trạng thái                     | Payment và reservation nằm ở service khác nhau | Cần Saga/outbox/reconciliation             |
| Staff mở cổng thủ công không kiểm soát             | Không bắt buộc lý do/audit                     | Cần manual override rule                   |
| Dashboard sai số                                   | Offline event chưa sync hoặc sync conflict     | Cần sync status và source-of-truth rule    |

---

## 2.4 Existing Alternatives and Gaps

| Current Alternative       | What It Solves            | Gap / Limitation                                         |
| ------------------------- | ------------------------- | -------------------------------------------------------- |
| Vé giấy/thẻ từ            | Cho phép xe vào/ra cơ bản | Không hỗ trợ payment, dashboard, merchant validation tốt |
| QR ticket tĩnh            | Dễ tạo vé số hóa          | Nếu không có security và plate validation thì rủi ro     |
| Camera biển số độc lập    | Ghi nhận phương tiện      | Không xử lý payment/reservation/discount                 |
| Thanh toán tại cổng       | Thu phí trực tiếp         | Gây ùn tắc                                               |
| Merchant đóng dấu hóa đơn | Giảm phí thủ công         | Khó đối soát, dễ gian lận                                |
| Cloud-only dashboard      | Quản lý tập trung         | Mất mạng là vận hành gián đoạn                           |
| Reservation đơn giản      | Cho khách đặt trước       | Dễ lệch trạng thái nếu payment/service lỗi               |

---

## 2.5 Formal Problem Statement

Các TTTM cần một hệ thống giữ xe nhanh, minh bạch và an toàn, nhưng môi trường hầm xe thường có mạng không ổn định, nhiều ngoại lệ vận hành và nhiều bên tham gia như khách, nhân viên giữ xe, cửa hàng, payment provider và ban quản lý. Nếu hệ thống chỉ dựa vào QR tĩnh, cloud API và payment webhook đơn giản, nó có thể gặp rủi ro tắc cổng, lệch trạng thái thanh toán, sai giảm phí hóa đơn, dùng lại QR, hoặc mất dữ liệu khi offline. ParkFlow Mall cần giải quyết bài toán này bằng một nền tảng quản lý giữ xe có online/offline flow rõ ràng, QR security nhiều lớp, payment reconciliation, invoice aggregation, audit log và dashboard vận hành đáng tin cậy.

---

## 2.6 Opportunity Statement

Nếu xử lý tốt các vấn đề offline, payment reconciliation và QR security, ParkFlow Mall có thể vượt khỏi phạm vi một demo QR parking thông thường để trở thành một mô hình smart parking có tính thực tế cao. Dự án có thể chứng minh năng lực BA, system design và backend thông qua các nghiệp vụ khó: offline consistency, distributed transaction, webhook idempotency, invoice aggregation, dynamic exit pass và operational dashboard.

---

# 3. Stakeholder Register

## 3.1 Stakeholder Overview

BRD v2 bổ sung thêm các stakeholder liên quan đến vận hành offline, bảo mật token, payment reconciliation và deploy. Các bên quan trọng không chỉ gồm khách, staff, merchant và admin, mà còn có payment provider, DevOps, security/privacy reviewer và người phụ trách reconciliation.

---

## 3.2 Stakeholder Register Table

| ID      | Stakeholder                  | Type                | Role / Responsibility                             | Needs / Expectations                           | Influence | Interest | Engagement Strategy            |
| ------- | ---------------------------- | ------------------- | ------------------------------------------------- | ---------------------------------------------- | --------- | -------- | ------------------------------ |
| STK-001 | Khách gửi xe                 | External            | Gửi xe, xem vé, thanh toán, ra bãi                | Nhanh, không rườm rà, có fallback khi mất mạng | High      | High     | Usability test trên điện thoại |
| STK-002 | Parking Staff                | Internal User       | Check-in/out, xử lý offline/manual case           | Console nhanh, rõ trạng thái online/offline    | High      | High     | Test với kịch bản mất mạng     |
| STK-003 | Merchant Staff               | External/Internal   | Xác nhận hóa đơn giảm phí                         | Quét QR, nhập hóa đơn, thấy kết quả ngay       | Medium    | High     | Test invoice aggregation       |
| STK-004 | Mall Manager/Admin           | Internal Business   | Theo dõi vận hành, doanh thu, alert               | Dashboard đúng, có audit và reconciliation     | High      | High     | Review KPI/report              |
| STK-005 | Payment/Reconciliation Owner | Internal            | Theo dõi payment pending, webhook lỗi, đối soát   | Không lệch tiền/session/reservation            | High      | High     | Payment issue review           |
| STK-006 | SePay                        | External Dependency | Gửi webhook giao dịch                             | Endpoint public, phản hồi đúng, idempotent     | Medium    | Medium   | Test mode + webhook logs       |
| STK-007 | DevOps Owner                 | Internal            | Docker, Railway deploy, env config                | Deploy ổn, có fallback local                   | Medium    | High     | Deployment review              |
| STK-008 | Security/Privacy Reviewer    | Internal/Advisory   | Kiểm soát QR token, ảnh xe, plate, access         | Token không dễ đoán, ảnh không public          | Medium    | Medium   | Security review                |
| STK-009 | Backend Team                 | Internal            | Xây service, payment, sync, audit                 | Rule rõ, boundary rõ                           | High      | High     | Requirement walkthrough        |
| STK-010 | QA/Testers                   | Internal            | Test business rules, offline, payment, edge cases | Có NFR và expected behavior                    | Medium    | High     | Test scenario mapping          |
| STK-011 | Solution Architect           | Internal            | Thiết kế microservice, saga, sync, deploy         | BRD đủ rõ để viết SAD                          | High      | High     | SAD workshop                   |
| STK-012 | Product Owner                | Internal            | Chốt scope, policy, priority                      | MVP khả thi, không vỡ scope                    | High      | High     | Decision log                   |

---

## 3.3 Stakeholder Needs Analysis

| Stakeholder        | Key Needs                        | Possible Concerns                   | Requirement Implication                             |
| ------------------ | -------------------------------- | ----------------------------------- | --------------------------------------------------- |
| Khách gửi xe       | Không bị kẹt khi mất mạng        | Không mở được QR trong hầm          | Cần screenshot/offline ticket và staff lookup       |
| Parking Staff      | Có quyền xử lý nhanh khi offline | Mở cổng sai, dữ liệu lệch           | Cần offline mode, local cache, audit, sync          |
| Merchant Staff     | Áp giảm phí đơn giản             | Nhiều hóa đơn/cửa hàng gây rối      | Cần invoice aggregation rule rõ                     |
| Admin              | Tin được dashboard               | Offline events chưa sync làm sai số | Dashboard cần hiển thị sync/pending status          |
| Payment Owner      | Không lệch thanh toán            | Webhook duplicate/missing           | Cần idempotency + reconciliation                    |
| Dev Team           | Không bị mơ hồ                   | Rule offline/payment phức tạp       | Cần rule và edge case cụ thể                        |
| QA                 | Có tiêu chí kiểm thử             | Không có NFR thì khó test           | Cần NFR table và acceptance direction               |
| Solution Architect | Đủ căn cứ thiết kế SAD           | BRD không nói saga/offline          | BRD v2 thêm business-level architecture constraints |

---

## 3.4 Decision Rights

| Decision Area              | Primary Decision Owner        | Contributors                 | Notes                                             |
| -------------------------- | ----------------------------- | ---------------------------- | ------------------------------------------------- |
| Offline operation policy   | Product Owner / BA            | Parking Staff, Tech Lead, QA | Quyết định offline được làm gì, không được làm gì |
| QR security policy         | Tech Lead / Security Reviewer | BA, Product Owner            | Chốt Lookup QR, Dynamic Exit Pass, device claim   |
| Payment mode               | Product Owner / Payment Owner | Backend Lead                 | MVP dùng Simulation + SePay Test, chưa dùng Live  |
| Invoice aggregation policy | Business Owner / BA           | Merchant Representative, QA  | Chốt cộng dồn hay một hóa đơn                     |
| Reservation reconciliation | Tech Lead / Backend Lead      | BA, QA                       | Chọn Saga/outbox/reconciliation ở SAD             |
| Deployment                 | DevOps / Tech Lead            | Product Owner                | Docker + Railway + Supabase/hybrid                |
| NFR targets                | Tech Lead / Product Owner     | BA, QA                       | Chốt SLA demo và production-like target           |

---

## 3.5 Communication Plan

| Stakeholder Group      | Communication Method | Frequency             | Purpose                                 |
| ---------------------- | -------------------- | --------------------- | --------------------------------------- |
| Product Owner + BA     | BRD policy review    | 1–2 buổi sau BRD v2   | Chốt offline, QR, invoice, payment      |
| Tech Lead + BA         | SAD workshop         | Sau BRD v2            | Thiết kế microservice, saga, deploy     |
| QA                     | Rule/NFR review      | Trước test plan       | Tạo test case offline/payment/security  |
| Parking Staff giả lập  | Prototype test       | Sau Staff Console MVP | Kiểm tra thao tác thực tế               |
| Merchant Staff giả lập | Merchant flow test   | Sau merchant module   | Kiểm tra cộng dồn hóa đơn               |
| DevOps                 | Deployment review    | Trước demo            | Kiểm tra Railway/Supabase/Docker/hybrid |

---

# 4. Scope and Capabilities

## 4.1 Scope Overview

BRD v2 giữ core scope của ParkFlow Mall, nhưng bổ sung bốn nhóm năng lực bắt buộc: Offline Operation, QR Security, Payment Reconciliation và Invoice Aggregation.

MVP vẫn không làm barrier thật, camera cố định, POS thật hoặc cảm biến từng ô. Tuy nhiên, MVP phải chứng minh hệ thống có thể vận hành an toàn trong các tình huống thực tế: mất mạng, webhook trễ, QR bị chụp, khách có nhiều hóa đơn và distributed transaction bị gián đoạn.

---

## 4.2 In Scope

| Scope ID | In-Scope Area                  | Description                                         | Priority    |
| -------- | ------------------------------ | --------------------------------------------------- | ----------- |
| SC-001   | Authentication & Authorization | Admin, Parking Staff, Merchant Staff                | Must Have   |
| SC-002   | Parking Session Management     | Quản lý vòng đời gửi xe                             | Must Have   |
| SC-003   | QR Lookup Ticket               | QR để mở trang vé và tra cứu session                | Must Have   |
| SC-004   | Dynamic Exit Pass              | Mã/QR ngắn hạn dùng cho check-out sau thanh toán    | Must Have   |
| SC-005   | OCR Assist                     | OCR gợi ý biển số; manual plate entry and staff confirmation remain authoritative | Should Have |
| SC-006   | Staff Gate Console             | Check-in/out, xử lý exception                       | Must Have   |
| SC-007   | Offline Staff Mode             | Cache active sessions, tạo offline events, sync lại | Must Have   |
| SC-008   | Customer Ticket Page           | Xem phí, trạng thái, giảm phí, payment              | Must Have   |
| SC-009   | Payment Simulation Mode        | Giả lập webhook/payment success                     | Must Have   |
| SC-010   | SePay Test Mode                | Tích hợp test webhook/VietQR                        | Should Have |
| SC-011   | Payment Reconciliation         | Quét payment/session/reservation pending            | Must Have   |
| SC-012   | Merchant Validation            | Nhập nhiều hóa đơn, cộng dồn theo policy            | Must Have   |
| SC-013   | Reservation                    | Đặt trước, giữ slot, thanh toán, check-in window    | Should Have |
| SC-014   | Reservation Saga Handling      | Xử lý lệch trạng thái slot/payment/reservation      | Should Have |
| SC-015   | Admin Dashboard                | Xe, doanh thu, validation, pending sync, alert      | Must Have   |
| SC-016   | Audit Log                      | Log toàn bộ thao tác quan trọng                     | Must Have   |
| SC-017   | Fraud Alert Basic              | QR reuse, plate mismatch, invoice duplicate         | Must Have   |
| SC-018   | Dockerized Deployment          | Docker Compose local, Railway cloud/hybrid          | Should Have |

---

## 4.3 Out of Scope

| Out-of-Scope Item                   | Reason                              | Possible Future Consideration                         |
| ----------------------------------- | ----------------------------------- | ----------------------------------------------------- |
| Barrier vật lý thật                 | Cần phần cứng                       | Tích hợp sau MVP                                      |
| Camera cố định real-time production | Cần thiết bị và tuning              | Có thể dùng webcam/IP camera sau                      |
| Cảm biến từng ô đỗ                  | Quá nặng phần cứng                  | Zone-based tracking sau MVP                           |
| POS thật của cửa hàng               | Cần API merchant thật               | Future integration                                    |
| SePay Live API tiền thật trong MVP  | Rủi ro đối soát tiền thật           | Làm Simulation + Test trước                           |
| Refund thật                         | Cần policy và payment flow phức tạp | Future                                                |
| Vé tháng/VIP                        | Tăng scope                          | Phase sau                                             |
| Dynamic pricing nâng cao            | Dễ làm phình rule                   | MVP dùng fee rule đơn giản                            |
| Multi-mall production               | Tăng complexity                     | Future SaaS model                                     |
| Offline merchant validation đầy đủ  | Rủi ro gian lận cao                 | MVP chỉ cho pending hoặc không cho offline validation |

---

## 4.4 MVP Scope

| MVP Capability               | Purpose                                | Success Signal                                            |
| ---------------------------- | -------------------------------------- | --------------------------------------------------------- |
| Check-in + QR Lookup         | Tạo session và QR tra cứu              | Session ACTIVE có QR                                      |
| OCR Assist                   | Hỗ trợ nhận diện biển số               | Staff xác nhận/sửa được plate                             |
| Customer Ticket              | Khách xem phí/trạng thái               | QR mở được web mobile                                     |
| Payment Simulation           | Test payment không phụ thuộc tiền thật | Payment chuyển PAID bằng webhook giả lập                  |
| SePay Test Integration       | Chuẩn bị payment thật                  | Nhận được webhook test và xử lý idempotent                |
| Dynamic Exit Pass            | Giảm rủi ro QR tĩnh                    | Check-out dùng exit pass ngắn hạn hoặc staff verification |
| Offline Staff Queue          | Không tê liệt khi mất mạng             | Tạo offline event và sync lại                             |
| Merchant Invoice Aggregation | Xử lý nhiều hóa đơn                    | Nhiều invoice cộng dồn đúng rule                          |
| Reservation Basic            | Đặt trước có payment state             | Reservation CONFIRMED/CANCELLED/EXPIRED                   |
| Reconciliation Job           | Sửa lệch trạng thái                    | Pending payment được đối soát                             |
| Dashboard                    | Quản lý vận hành                       | Hiển thị xe, doanh thu, alert, pending sync               |
| Audit Log                    | Truy vết                               | Log không mất sau sync                                    |

---

## 4.5 Business Capabilities

### Canonical MVP priority clarification

- Manual plate entry and staff plate confirmation are Must Have.
- OCR Assist is Should Have, assistive only, and must never block parking operations.
- Reservation Basic is Should Have and must not block the core MVP.
- Payment Simulation is Must Have; SePay Test is Should Have; SePay Live is out of MVP.
- Merchant validation defaults to `AGGREGATE_INVOICE`, with one-use invoice codes and online-only official validation.

| Capability ID | Business Capability       | Description                                    | Related Users           | Priority    |
| ------------- | ------------------------- | ---------------------------------------------- | ----------------------- | ----------- |
| CAP-001       | Parking Session Lifecycle | Quản lý check-in đến check-out                 | Staff, Customer         | Must Have   |
| CAP-002       | QR Ticketing              | QR tra cứu session                             | Customer, Staff         | Must Have   |
| CAP-003       | Secure Exit Authorization | Dynamic Exit Pass/verification cho check-out   | Customer, Staff         | Must Have   |
| CAP-004       | Plate Verification        | Đối chiếu biển số/ảnh xe                       | Staff, Security         | Must Have   |
| CAP-005       | Offline Operation         | Vận hành hạn chế khi mất mạng                  | Staff, Admin            | Must Have   |
| CAP-006       | Payment Processing        | Simulation + SePay test/live-ready             | Customer, Payment Owner | Must Have   |
| CAP-007       | Payment Reconciliation    | Xử lý trạng thái payment pending/lệch          | Admin, Payment Owner    | Must Have   |
| CAP-008       | Merchant Validation       | Giảm phí theo hóa đơn                          | Merchant, Customer      | Must Have   |
| CAP-009       | Invoice Aggregation       | Cộng dồn nhiều hóa đơn cho một session         | Merchant, Customer      | Must Have   |
| CAP-010       | Reservation Management    | Đặt trước, giữ slot, check-in window           | Customer, Staff         | Should Have |
| CAP-011       | Reservation Saga Recovery | Xử lý payment/slot/reservation lệch trạng thái | Admin, System           | Should Have |
| CAP-012       | Operational Dashboard     | Theo dõi bãi xe và trạng thái bất thường       | Admin                   | Must Have   |
| CAP-013       | Auditability              | Truy vết thao tác                              | Admin, QA               | Must Have   |
| CAP-014       | Fraud Monitoring          | Cảnh báo QR, plate, invoice bất thường         | Admin, Security         | Must Have   |

---

## 4.6 High-Level User Journey

| Step | User Action            | System / Business Response                               | Value Created               |
| ---- | ---------------------- | -------------------------------------------------------- | --------------------------- |
| 1    | Staff check-in xe      | OCR gợi ý biển số, staff xác nhận, tạo session           | Xe vào nhanh                |
| 2    | Hệ thống tạo QR Lookup | QR mở ticket page, không có quyền tự động cho ra         | Giảm rủi ro QR tĩnh         |
| 3    | Khách mua hàng         | Merchant staff thêm một hoặc nhiều hóa đơn               | Rule giảm phí thực tế hơn   |
| 4    | Khách thanh toán       | Payment Simulation/SePay cập nhật PAID                   | Giảm thanh toán tại cổng    |
| 5    | Hệ thống tạo Exit Pass | Exit Pass ngắn hạn sau payment                           | Tăng bảo mật check-out      |
| 6    | Khách ra cổng          | Staff scan exit pass/QR, đối chiếu plate/payment         | Xe ra nhanh và an toàn      |
| 7    | Nếu mất mạng           | Staff Console dùng local cache/offline queue/manual rule | Không tắc cổng              |
| 8    | Có mạng lại            | Offline events sync lên server, conflict được xử lý      | Dữ liệu cuối cùng nhất quán |
| 9    | Admin xem dashboard    | Dashboard hiển thị online/offline/pending/suspicious     | Quản lý tin được dữ liệu    |
| 10   | Reconciliation chạy    | Payment/reservation pending được sửa                     | Giảm lệch trạng thái        |

---

## 4.7 Capability Prioritization

| Capability                    | Priority           | Rationale                                |
| ----------------------------- | ------------------ | ---------------------------------------- |
| Parking Session               | Must Have          | Core của hệ thống                        |
| QR Lookup + Dynamic Exit Pass | Must Have          | Giải quyết bảo mật QR                    |
| Offline Staff Mode            | Must Have          | Bắt buộc cho hầm TTTM                    |
| Payment Simulation            | Must Have          | Giảm rủi ro tích hợp payment thật        |
| SePay Test Mode               | Should Have        | Chuẩn bị sản phẩm thực tế                |
| Payment Reconciliation        | Must Have          | Bắt buộc khi có webhook/payment          |
| Invoice Aggregation           | Must Have          | Phù hợp thực tế mua hàng nhiều cửa hàng  |
| Reservation                   | Should Have        | Khác biệt nhưng không được phá core flow |
| Reservation Saga Recovery     | Should Have        | Cần nếu làm reservation + payment        |
| Dashboard Pending State       | Must Have          | Admin cần thấy dữ liệu chưa đồng bộ      |
| Offline Merchant Validation   | Won’t Have for MVP | Rủi ro gian lận cao                      |

---

# 5. Business Rules

## 5.1 Business Rules Overview

BRD v2 bổ sung các rule bắt buộc cho offline mode, QR token security, payment reconciliation, reservation saga và invoice aggregation. Các rule này cần được chuyển thẳng thành acceptance criteria và test case.

---

## 5.2 Business Rules Table

| Rule ID | Rule Name                          | Rule Statement                                                                                                                                   | Rationale                          | Priority    | Exception / Notes                                                  |
| ------- | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------- | ----------- | ------------------------------------------------------------------ |
| BR-001  | Unique Active Session Rule         | Một biển số không được có nhiều hơn một session ACTIVE trong cùng bãi                                                                            | Tránh trùng phiên                  | Must Have   | Nếu OCR sai, staff được sửa trước khi tạo                          |
| BR-002  | QR Lookup Token Rule               | QR vé chỉ dùng để tra cứu session/ticket page, không tự động cấp quyền ra cổng                                                                   | Giảm rủi ro QR tĩnh                | Must Have   | Token không chứa plate/amount                                      |
| BR-003  | Dynamic Exit Pass Rule             | Sau khi payment hợp lệ, hệ thống tạo Exit Pass ngắn hạn để check-out                                                                             | Chống screenshot QR tĩnh           | Must Have   | TTL đề xuất 30–60 giây nếu online                                  |
| BR-004  | Offline Exit Pass Rule             | Nếu khách đã thanh toán và có signed offline exit proof còn hạn, staff có thể xác minh bằng local public key/cache                               | Hỗ trợ mất mạng                    | Should Have | Cần SAD thiết kế chữ ký                                            |
| BR-005  | Device Claim Rule                  | Lần đầu khách mở QR ticket, browser có thể được gắn claim token để tạo Exit Pass                                                                 | Giảm nguy cơ QR bị chụp lén        | Should Have | Nếu đổi máy, cần staff recovery                                    |
| BR-006  | Screenshot Policy Rule             | Screenshot QR Lookup chỉ được dùng để tìm session, không đủ quyền check-out tự động                                                              | Tránh QR bị dùng sai               | Must Have   | Staff vẫn phải verify plate/payment                                |
| BR-007  | Plate Match Rule                   | Biển số lúc ra phải khớp với biển số lúc vào hoặc được staff xác minh thủ công                                                                   | Bảo vệ tài sản                     | Must Have   | Nếu không khớp → SUSPICIOUS                                        |
| BR-008  | Payment Required Rule              | Session phải PAID hoặc final_fee = 0 trước khi check-out tự động                                                                                 | Đảm bảo thu phí                    | Must Have   | Offline manual override cần reason                                 |
| BR-009  | Grace Period Rule                  | Sau khi thanh toán, khách có thời gian ra bãi trước khi tính thêm phí                                                                            | Tránh khách bị tính thêm ngay      | Must Have   | Đề xuất 15 phút                                                    |
| BR-010  | Offline Mode Entry Rule            | Khi mất mạng, staff được tạo offline check-in event với local temporary session ID                                                               | Không tắc cổng vào                 | Must Have   | Sync khi có mạng                                                   |
| BR-011  | Offline Mode Exit Rule             | Khi mất mạng, staff chỉ được cho xe ra nếu có đủ bằng chứng local: active session cache, payment proof/final_fee=0 hoặc manual override có lý do | Không tắc cổng nhưng vẫn kiểm soát | Must Have   | Case không đủ dữ liệu → manual review                              |
| BR-012  | Offline Queue Sync Rule            | Mọi offline event phải có idempotency key, staff_id, device_id, timestamp và sync lại khi online                                                 | Tránh mất/trùng dữ liệu            | Must Have   | Server xử lý conflict                                              |
| BR-013  | Server Source of Truth Rule        | Sau khi sync, server là nguồn dữ liệu cuối cùng; offline event có thể bị accepted, rejected hoặc conflict                                        | Đảm bảo nhất quán                  | Must Have   | Dashboard hiển thị conflict                                        |
| BR-014  | Merchant Online Validation Rule    | Merchant validation chỉ được xác nhận chính thức khi online                                                                                      | Giảm gian lận                      | Must Have   | Offline chỉ cho ghi draft/pending nếu cần                          |
| BR-015  | Invoice Aggregation Rule           | Một session có thể cộng dồn nhiều hóa đơn hợp lệ để đạt ngưỡng giảm phí                                                                          | Phù hợp thực tế TTTM               | Must Have   | Policy có thể cấu hình SINGLE hoặc AGGREGATE                       |
| BR-016  | One Invoice One Use Rule           | Một invoice_code chỉ được dùng một lần trong hệ thống                                                                                            | Chống dùng lại hóa đơn             | Must Have   | Trùng invoice → alert                                              |
| BR-017  | Invoice Ownership Rule             | Merchant staff chỉ được nhập hóa đơn thuộc cửa hàng/tài khoản của mình                                                                           | Tránh giả mạo merchant             | Must Have   | Admin có quyền review                                              |
| BR-018  | Discount Recalculation Rule        | Khi thêm hóa đơn mới, hệ thống tính lại tổng hóa đơn hợp lệ và discount tương ứng                                                                | Hỗ trợ cộng dồn                    | Must Have   | Discount không vượt total_fee                                      |
| BR-019  | Payment Simulation First Rule      | MVP phải có Simulation Mode để giả lập payment/webhook trước khi dùng SePay live                                                                 | Giảm rủi ro tiền thật              | Must Have   | Test Mode chạy sau simulation                                      |
| BR-020  | Payment Webhook Idempotency Rule   | Cùng một giao dịch/webhook không được xử lý nhiều lần                                                                                            | Tránh cộng tiền trùng              | Must Have   | Dùng transaction_id/payment_code                                   |
| BR-021  | Payment Matching Rule              | Payment chỉ hợp lệ khi amount và payment_code khớp order                                                                                         | Đối soát đúng session              | Must Have   | Sai lệch → manual review                                           |
| BR-022  | Payment Reconciliation Rule        | Hệ thống phải có job quét payment/session/reservation pending để tự sửa hoặc đưa vào review                                                      | Xử lý distributed failure          | Must Have   | Chạy theo chu kỳ cấu hình                                          |
| BR-023  | Reservation Hold Rule              | Khi khách tạo reservation, slot chỉ được giữ tạm trong thời gian payment window                                                                  | Tránh giữ slot vô hạn              | Should Have | Ví dụ 10 phút                                                      |
| BR-024  | Reservation Confirmation Saga Rule | Reservation chỉ CONFIRMED khi payment success và slot hold còn hợp lệ                                                                            | Tránh lệch payment-slot            | Should Have | Nếu payment success nhưng reservation lỗi → pending reconciliation |
| BR-025  | Reservation Compensation Rule      | Nếu payment timeout/fail, slot hold phải được release                                                                                            | Giải phóng slot                    | Should Have | Nếu payment success trễ → manual/reconciliation                    |
| BR-026  | Manual Override Rule               | Mở cổng thủ công, bypass payment hoặc xử lý offline exit đều phải có reason và audit log                                                         | Kiểm soát trách nhiệm              | Must Have   | Chỉ role được phép                                                 |
| BR-027  | Audit Log Rule                     | Check-in, check-out, payment update, invoice validation, override, plate edit, sync conflict đều phải được log                                   | Truy vết                           | Must Have   | Log không xóa/sửa tùy tiện                                         |
| BR-028  | OCR Assistance Rule                | OCR chỉ là gợi ý; staff xác nhận/sửa plate trước khi chốt                                                                                        | Giảm lỗi OCR                       | Must Have   | Low confidence bắt buộc confirm                                    |
| BR-029  | Dashboard Pending Visibility Rule  | Dashboard phải hiển thị dữ liệu pending sync, pending payment, conflict và suspicious                                                            | Tránh admin hiểu sai               | Must Have   | Không giấu trạng thái chưa chắc chắn                               |
| BR-030  | QR Reuse Rule                      | QR/Exit Pass đã dùng hoặc hết hạn không được dùng lại                                                                                            | Chống replay attack                | Must Have   | Hiển thị INVALID/EXPIRED                                           |

---

## 5.3 Rule Categories

| Category            | Related Rules                                  | Notes                              |
| ------------------- | ---------------------------------------------- | ---------------------------------- |
| QR Security         | BR-002, BR-003, BR-004, BR-005, BR-006, BR-030 | Tách QR tra cứu và quyền ra cổng   |
| Offline Operation   | BR-010, BR-011, BR-012, BR-013, BR-014, BR-029 | Bắt buộc cho TTTM                  |
| Payment             | BR-008, BR-009, BR-019, BR-020, BR-021, BR-022 | Simulation + Test + reconciliation |
| Reservation Saga    | BR-023, BR-024, BR-025                         | Xử lý distributed transaction      |
| Merchant Validation | BR-015, BR-016, BR-017, BR-018                 | Cộng dồn hóa đơn                   |
| Parking Safety      | BR-001, BR-007, BR-026, BR-027, BR-028         | Bảo vệ tài sản và audit            |
| Dashboard/Reporting | BR-029                                         | Hiển thị trạng thái chưa đồng bộ   |

---

## 5.4 Edge Cases and Exceptions

| Edge Case                                               | Expected Business Handling                                                                  |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| Khách ở hầm không có mạng, không mở được QR page        | Staff tìm session bằng biển số/ảnh trong local cache hoặc manual lookup khi có mạng         |
| Khách chụp screenshot QR Lookup                         | Screenshot chỉ giúp tìm session, không đủ quyền mở cổng tự động                             |
| Khách đã thanh toán nhưng không mở được Exit Pass       | Staff kiểm tra payment proof/local cache; nếu không có mạng, tạo manual review/offline exit |
| Staff Console mất mạng khi check-in                     | Tạo offline temporary session, in/hiển thị local QR, sync sau                               |
| Staff Console mất mạng khi check-out                    | Dùng local cache + payment proof + manual reason nếu cần                                    |
| Offline event sync trùng                                | Server dùng idempotency key để bỏ trùng                                                     |
| Offline event conflict với server                       | Đưa vào conflict queue, admin/staff review                                                  |
| SePay trừ tiền thành công nhưng Reservation Service lỗi | Payment chuyển PAYMENT_CONFIRMED_PENDING_RESERVATION, reconciliation job xử lý              |
| Reservation giữ slot nhưng khách không thanh toán       | Release slot sau payment window                                                             |
| Payment webhook đến trễ sau khi reservation expired     | Chuyển manual review hoặc policy refund/future credit                                       |
| Một xe có nhiều hóa đơn từ nhiều cửa hàng               | Cộng dồn nếu policy AGGREGATE bật                                                           |
| Một hóa đơn đã dùng cho xe khác                         | Reject và tạo fraud alert                                                                   |
| Merchant nhập hóa đơn khi offline                       | Không áp discount chính thức; chỉ lưu draft/pending nếu được cấu hình                       |
| QR Exit Pass hết hạn                                    | Khách tạo lại nếu online; nếu offline thì staff manual review                               |
| Biển số ra không khớp                                   | SUSPICIOUS, không tự động cho ra                                                            |
| OCR sai                                                 | Staff sửa và audit log ghi nhận                                                             |

---

## 5.5 Business Rule Validation

1. Tạo test case cho từng rule BR-001 đến BR-030.
2. Test offline bằng cách tắt Wi-Fi/Internet trong lúc check-in/check-out.
3. Test QR security bằng screenshot QR cũ, Exit Pass hết hạn và QR dùng lại.
4. Test payment simulation trước, sau đó test SePay webhook.
5. Test webhook duplicate và webhook đến trễ.
6. Test reservation payment success nhưng service bị lỗi mô phỏng.
7. Test nhiều hóa đơn trên một session, hóa đơn trùng, hóa đơn dưới ngưỡng.
8. Test dashboard hiển thị pending sync/pending payment/conflict/suspicious.

---

# 6. Constraints and Assumptions

## 6.1 Constraints Overview

Dự án bị ràng buộc bởi môi trường TTTM có mạng không ổn định, nguồn lực thiết bị giới hạn, mong muốn triển khai microservice, tích hợp payment, OCR và deploy chi phí thấp. BRD v2 chốt rõ hơn các constraint về deploy, payment mode, offline operation và security.

---

## 6.2 Business Constraints

| Constraint ID | Constraint                                        | Impact                                  | Handling Approach                       |
| ------------- | ------------------------------------------------- | --------------------------------------- | --------------------------------------- |
| CON-001       | Hầm xe có thể mất mạng                            | Không thể phụ thuộc cloud 100%          | Offline staff mode + local cache + sync |
| CON-002       | Khách không bắt buộc đăng nhập                    | QR token phải an toàn hơn               | Tách Lookup QR và Exit Pass             |
| CON-003       | Payment live có rủi ro tiền thật                  | Dễ kẹt đối soát                         | Simulation Mode trước, Test Mode sau    |
| CON-004       | Merchant validation phải nhanh                    | Staff cửa hàng không thể làm nhiều bước | Quét QR + nhập invoice_code + amount    |
| CON-005       | Một session có thể có nhiều hóa đơn               | Rule một hóa đơn không thực tế          | Hỗ trợ invoice aggregation              |
| CON-006       | Demo cần ổn định trên thiết bị cá nhân            | Không chạy quá nhiều service nặng       | Dùng Docker/hybrid và giới hạn service  |
| CON-007       | Dashboard phải phản ánh trạng thái chưa chắc chắn | Offline/pending làm dữ liệu tạm lệch    | Hiển thị pending/conflict rõ ràng       |

---

## 6.3 Technical / Operational Constraints

| Constraint ID | Constraint                                                 | Business Impact                                                    |                                                      |
| ------------- | ---------------------------------------------------------- | ------------------------------------------------------------------ | ---------------------------------------------------- |
| CON-008       | Mô hình microservice                                       | Cần xử lý distributed transaction và service failure               |                                                      |
| CON-009       | Java Spring Boot cho backend                               | Phù hợp team, nhưng OCR nên là Python service                      |                                                      |
| CON-010       | React frontend responsive                                  | Dùng được trên điện thoại khách/staff                              |                                                      |
| CON-011       | Supabase PostgreSQL/Storage được ưu tiên                   | Có Postgres managed và storage cho ảnh; cần connection/security rõ |                                                      |
| CON-012       | Docker bắt buộc cho local development                      | Giúp chạy multi-container nhất quán                                |                                                      |
| CON-013       | Railway là deploy target chính cho cloud demo              | Cần quản lý env, public URL, service cold start                    |                                                      |
| CON-014       | Railway TCP Proxy port không được hardcode như chuẩn chung | Port do Railway generate theo service                              | Ghi `24728` chỉ nếu project thực tế được cấp port đó |
| CON-015       | SePay webhook cần public HTTPS endpoint                    | Local cần tunnel hoặc deploy payment service                       |                                                      |
| CON-016       | OCR repo có thể không ổn định                              | Luôn có manual plate correction                                    |                                                      |

Ghi chú deploy: Docker Compose phù hợp để định nghĩa và chạy nhiều container trong local/dev environment. Supabase cung cấp Postgres database và có tài liệu kết nối Postgres cho backend/service. Railway hỗ trợ public networking và TCP proxy, nhưng proxy domain/port là giá trị được Railway cấp phát, không nên xem một port ví dụ là mặc định cho mọi dự án.

---

## 6.4 Legal, Privacy, Compliance Constraints

| Constraint                                  | Reason                                    | Impact                                |
| ------------------------------------------- | ----------------------------------------- | ------------------------------------- |
| Biển số xe là dữ liệu định danh phương tiện | Có thể liên quan quyền riêng tư           | Cần giới hạn người xem plate/ảnh      |
| Ảnh xe/biển số cần kiểm soát truy cập       | Có thể lộ thông tin tài sản               | Staff/admin role mới xem được         |
| QR token không được chứa dữ liệu nhạy cảm   | Tránh lộ plate/payment                    | Chỉ chứa opaque token                 |
| Offline cache chứa dữ liệu nhạy cảm         | Laptop/staff device mất có thể lộ dữ liệu | Cache cần TTL, mã hóa nếu có thể      |
| Audit log cần bảo toàn                      | Phục vụ tranh chấp                        | Không cho xóa/sửa tùy tiện            |
| Payment data chỉ lưu tối thiểu              | Giảm rủi ro tài chính                     | Không lưu thông tin ngân hàng dư thừa |
| Dữ liệu demo cần retention rõ               | Tránh giữ ảnh/plate quá lâu               | Xóa/ẩn sau thời gian cấu hình         |

---

## 6.5 Assumptions

| Assumption ID | Assumption                                              | Confidence | Validation Method             | Impact If False                               |
| ------------- | ------------------------------------------------------- | ---------- | ----------------------------- | --------------------------------------------- |
| ASM-001       | Hầm TTTM thường có rủi ro mất mạng                      | High       | Test tại hầm/mô phỏng offline | Nếu sai, offline có thể giảm scope            |
| ASM-002       | Staff có thể dùng local cache/offline queue trong demo  | Medium     | Prototype Staff Console       | Nếu sai, cần manual paper fallback            |
| ASM-003       | Khách chấp nhận quét QR nhưng không muốn đăng nhập      | High       | User testing                  | Nếu sai, có thể thêm customer account         |
| ASM-004       | Dynamic Exit Pass giúp giảm rủi ro QR screenshot        | Medium     | Security review               | Nếu sai, cần thêm PIN/OTP                     |
| ASM-005       | Invoice aggregation phù hợp hơn một hóa đơn duy nhất    | Medium     | Validate với nghiệp vụ TTTM   | Nếu sai, chuyển policy SINGLE_INVOICE         |
| ASM-006       | Payment Simulation Mode đủ cho MVP đầu tiên             | High       | Demo planning                 | Nếu sai, phải tích hợp SePay test sớm hơn     |
| ASM-007       | SePay Test Mode có thể tích hợp sau Simulation          | Medium     | Technical spike               | Nếu sai, giữ simulation cho demo              |
| ASM-008       | Supabase đủ cho DB/storage MVP                          | Medium     | Spike connection/storage      | Nếu sai, dùng Railway Postgres/local Postgres |
| ASM-009       | Railway phù hợp deploy demo chi phí thấp                | Medium     | Deploy thử                    | Nếu sai, dùng local/hybrid                    |
| ASM-010       | Laptop cá nhân đủ chạy Docker Compose bản rút gọn       | Medium     | Local performance test        | Nếu sai, gộp service hoặc chạy cloud          |
| ASM-011       | Reservation nên làm sau core parking/payment            | High       | Sprint planning               | Nếu sai, reservation có thể cắt khỏi MVP      |
| ASM-012       | Merchant offline validation không nên cho áp chính thức | High       | Fraud review                  | Nếu sai, cần signed offline merchant policy   |

---

## 6.6 Dependencies

| Dependency                | Description                     | Risk                                          |
| ------------------------- | ------------------------------- | --------------------------------------------- |
| SePay Webhook             | Nhận thông báo payment          | Endpoint lỗi, webhook retry, đối soát pending |
| Payment Simulation Tool   | Giả lập webhook/payment success | Nếu thiếu, test payment chậm                  |
| Supabase PostgreSQL       | Database chính                  | Connection limit, schema ownership            |
| Supabase Storage          | Lưu ảnh xe/plate                | Permission và signed URL                      |
| Railway                   | Deploy API/public webhook       | Cold start, env, public URL                   |
| Docker Compose            | Local multi-service             | Laptop có thể nặng                            |
| OCR Repo                  | Detect/OCR biển số              | Sai do ánh sáng/góc chụp                      |
| Browser Storage/IndexedDB | Local cache/offline queue       | Security/cache corruption                     |
| QR Scanner Library        | Scan QR trên web                | Browser camera permission                     |
| Reconciliation Scheduler  | Job quét pending states         | Nếu lỗi, dữ liệu lệch lâu hơn                 |

---

# 7. Risks and Issues

## 7.1 Risk Overview

BRD v2 xác định rủi ro lớn nhất không còn chỉ là scope creep, mà là **operational failure**: mất mạng, lệch payment, QR misuse, sync conflict và distributed transaction. Đây là các rủi ro phải thiết kế ngay từ BRD để tránh SAD/PRD bị thiếu nền.

---

## 7.2 Risk Register

| Risk ID | Risk                                                       | Probability | Impact | Severity | Mitigation Plan                                  | Owner                 |
| ------- | ---------------------------------------------------------- | ----------- | ------ | -------- | ------------------------------------------------ | --------------------- |
| RSK-001 | Hệ thống cloud mất mạng làm kẹt cổng                       | High        | High   | Critical | Offline mode + local cache + manual override     | Tech Lead/BA          |
| RSK-002 | QR tĩnh bị chụp và dùng lại                                | Medium      | High   | High     | Lookup QR + Dynamic Exit Pass + TTL              | Security/Tech Lead    |
| RSK-003 | Payment thành công nhưng reservation/session chưa cập nhật | Medium      | High   | High     | Reconciliation job + pending state               | Backend Lead          |
| RSK-004 | Webhook duplicate làm ghi nhận trùng                       | Medium      | High   | High     | Idempotency key                                  | Payment Service Owner |
| RSK-005 | Offline sync conflict                                      | Medium      | High   | High     | Event ID, server source of truth, conflict queue | Backend Lead          |
| RSK-006 | Invoice aggregation bị gian lận                            | Medium      | Medium | Medium   | One invoice one use, merchant ownership, audit   | BA/Product            |
| RSK-007 | Merchant offline validation bị lạm dụng                    | Medium      | High   | High     | Không cho validation chính thức khi offline      | Product Owner         |
| RSK-008 | Docker/microservice quá nặng cho laptop                    | Medium      | Medium | Medium   | Rút gọn service, hybrid deploy                   | DevOps                |
| RSK-009 | Railway cold start ảnh hưởng demo                          | Medium      | Medium | Medium   | Warm-up, local fallback                          | DevOps                |
| RSK-010 | OCR sai nhiều                                              | High        | Medium | High     | OCR assist only, manual confirmation             | Tech Lead             |
| RSK-011 | Security flow làm khách khó chịu                           | Medium      | Medium | Medium   | Chỉ áp Dynamic Exit Pass lúc exit/payment        | UX/BA                 |
| RSK-012 | Dashboard hiển thị sai do pending sync                     | Medium      | High   | High     | Hiển thị pending/conflict rõ                     | Backend/Admin         |
| RSK-013 | Reservation làm phình scope                                | Medium      | Medium | Medium   | Làm sau core flow                                | Product Owner         |
| RSK-014 | Live payment gây rủi ro tiền thật                          | Medium      | High   | High     | Simulation + Test Mode trước                     | Payment Owner         |
| RSK-015 | Offline cache lộ dữ liệu                                   | Low/Medium  | High   | High     | TTL, encryption, role, device trust              | Security/Tech Lead    |

---

## 7.3 Issue Log

| Issue ID | Current Issue                                        | Impact                   | Recommended Action                                           | Priority |
| -------- | ---------------------------------------------------- | ------------------------ | ------------------------------------------------------------ | -------- |
| ISS-001  | Chưa chốt discount policy cuối cùng                  | Ảnh hưởng merchant rule  | Chọn AGGREGATE_INVOICE làm default MVP                       | High     |
| ISS-002  | Payment mode chưa chốt                               | Dễ kẹt SePay live        | Chốt Simulation Mode + SePay Test Mode, không Live trong MVP | High     |
| ISS-003  | Offline mode chưa có thiết kế chi tiết               | SAD thiếu phần critical  | Viết offline flow trong PRD/SAD                              | High     |
| ISS-004  | QR security chưa đủ chặt                             | Rủi ro screenshot/replay | Tách Lookup QR và Exit Pass                                  | High     |
| ISS-005  | Reservation distributed transaction chưa có strategy | Dễ lệch tiền/slot        | Bổ sung Saga/reconciliation trong SAD                        | High     |
| ISS-006  | Deploy target chưa quyết định rõ                     | DevOps mơ hồ             | Chốt Docker + Railway + Supabase/hybrid                      | High     |
| ISS-007  | Railway proxy port chưa xác định thực tế             | Không nên hardcode       | Dùng env var, port theo Railway cấp                          | Medium   |
| ISS-008  | NFR chưa có baseline                                 | QA/SAD thiếu mục tiêu    | Thêm Section 8 NFR                                           | High     |
| ISS-009  | OCR repo chưa chọn                                   | Ảnh hưởng Vision Service | Spike repo trước Sprint OCR                                  | Medium   |
| ISS-010  | Offline cache security chưa rõ                       | Rủi ro privacy           | Security review                                              | Medium   |

---

## 7.4 Risk Response Strategy

| Risk Type               | Strategy        | Explanation                                              |
| ----------------------- | --------------- | -------------------------------------------------------- |
| Network failure         | Mitigate        | Offline mode, local cache, sync queue                    |
| QR misuse               | Mitigate        | Dynamic Exit Pass, TTL, device claim, plate verification |
| Payment inconsistency   | Mitigate        | Idempotency, reconciliation, pending states              |
| Distributed transaction | Mitigate        | Saga pattern/outbox/inbox ở SAD                          |
| Invoice fraud           | Mitigate        | Aggregation policy + one invoice one use                 |
| Scope creep             | Avoid           | Reservation/OCR advanced/deploy live tách phase          |
| Deploy instability      | Mitigate        | Railway + local fallback                                 |
| Privacy risk            | Mitigate        | Role access, cache TTL, storage permission               |
| OCR failure             | Accept/Mitigate | Manual correction là bắt buộc                            |

---

## 7.5 Early Warning Indicators

| Indicator                                       | Meaning                    | Suggested Action               |
| ----------------------------------------------- | -------------------------- | ------------------------------ |
| Offline check-out không có expected rule        | Nguy cơ tắc cổng           | Viết offline SOP ngay          |
| Payment pending nhiều                           | Webhook/reconciliation lỗi | Kiểm tra payment matching      |
| QR screenshot vẫn check-out được                | Security fail              | Siết Exit Pass TTL             |
| Staff phải thao tác quá 5 bước để xử lý offline | UX vận hành kém            | Rút gọn Staff Console          |
| Dashboard không phân biệt confirmed/pending     | Admin hiểu sai dữ liệu     | Thêm status label              |
| Nhiều invoice trùng                             | Merchant fraud risk        | Alert và review                |
| Docker chạy chậm trên laptop                    | Demo risk                  | Hybrid deploy hoặc gộp service |
| SePay test chậm tiến độ                         | Payment integration risk   | Dùng simulation cho demo       |
| OCR timeout nhiều                               | Camera/OCR không ổn        | Nhập biển số thủ công fallback |

---

# 8. Non-Functional Requirements

## 8.1 NFR Overview

NFR định nghĩa các yêu cầu phi chức năng tối thiểu để hệ thống đủ khả năng vận hành trong bối cảnh TTTM. Các chỉ số dưới đây là mục tiêu cho MVP/demo, không phải cam kết production enterprise.

---

## 8.2 Performance Requirements

| NFR ID  | Requirement               | Target for MVP                                                                   | Priority    | Notes                                                               |
| ------- | ------------------------- | -------------------------------------------------------------------------------- | ----------- | ------------------------------------------------------------------- |
| NFR-001 | Staff login response time | P95 dưới 2 giây                                                                  | Should Have | Online mode                                                         |
| NFR-002 | Create parking session    | P95 dưới 2 giây, không tính OCR                                                  | Must Have   | Sau khi staff xác nhận plate                                        |
| NFR-003 | OCR processing time       | P95 dưới 5 giây/ảnh                                                              | Should Have | Nếu quá timeout, cho nhập thủ công                                  |
| NFR-004 | Customer Ticket Page load | Dưới 3 giây trên 4G ổn định                                                      | Should Have | Online mode                                                         |
| NFR-005 | Check-out validation      | P95 dưới 2 giây khi online                                                       | Must Have   | Không tính thao tác scan                                            |
| NFR-006 | Dashboard update          | Dưới 5 giây sau event online                                                     | Should Have | Có thể dùng polling                                                 |
| NFR-007 | Payment webhook response  | Hệ thống nên phản hồi dưới 2 giây nội bộ; tuyệt đối không vượt giới hạn provider | Must Have   | SePay yêu cầu response success đúng format trong thời gian quy định |
| NFR-008 | Offline event sync        | 95% events sync trong 5 phút sau khi có mạng                                     | Must Have   | Tùy network                                                         |

---

## 8.3 Capacity & Load Requirements

| NFR ID  | Requirement                      | Target for MVP                          | Priority    | Notes                        |
| ------- | -------------------------------- | --------------------------------------- | ----------- | ---------------------------- |
| NFR-009 | Concurrent staff users           | 3–5 staff đồng thời                     | Must Have   | Đủ demo                      |
| NFR-010 | Concurrent customer ticket views | 20 users đồng thời                      | Should Have | Demo/small pilot             |
| NFR-011 | Daily parking sessions           | 500–1.000 sessions/ngày trong test data | Should Have | Seed data cho dashboard      |
| NFR-012 | Active sessions                  | 300–500 active sessions                 | Should Have | Mô phỏng TTTM nhỏ            |
| NFR-013 | Merchant validations             | 100 validations/ngày test data          | Could Have  | Mô phỏng nhiều cửa hàng      |
| NFR-014 | Webhook events                   | 100–300 events/ngày test data           | Should Have | Bao gồm duplicate/retry mock |

---

## 8.4 Availability & Resilience Requirements

| NFR ID  | Requirement              | Target for MVP                                                | Priority    | Notes                     |
| ------- | ------------------------ | ------------------------------------------------------------- | ----------- | ------------------------- |
| NFR-015 | Core online availability | Demo target 95% trong thời gian test                          | Should Have | Không phải production SLA |
| NFR-016 | Offline staff continuity | Staff có thể tiếp tục check-in/out cơ bản khi mất mạng        | Must Have   | Với local cache/queue     |
| NFR-017 | Graceful degradation     | OCR/payment/dashboard lỗi không được làm sập toàn bộ hệ thống | Must Have   | Có fallback thủ công      |
| NFR-018 | Retry handling           | Payment webhook/offline sync phải xử lý retry an toàn         | Must Have   | Idempotency               |
| NFR-019 | Recovery after restart   | Service restart không làm mất payment/session đã ghi nhận     | Must Have   | DB là source of truth     |
| NFR-020 | Conflict visibility      | Sync conflict phải hiển thị cho admin/staff                   | Must Have   | Không âm thầm bỏ dữ liệu  |

---

## 8.5 Security Requirements

| NFR ID  | Requirement             | Target for MVP                                          | Priority    | Notes                    |
| ------- | ----------------------- | ------------------------------------------------------- | ----------- | ------------------------ |
| NFR-021 | Token entropy           | QR token phải khó đoán                                  | Must Have   | Opaque random token      |
| NFR-022 | Token separation        | Lookup QR và Exit Pass có quyền khác nhau               | Must Have   | Lookup không mở cổng     |
| NFR-023 | Exit Pass TTL           | Exit Pass online ngắn hạn, đề xuất 30–60 giây           | Must Have   | Có thể refresh           |
| NFR-024 | HTTPS                   | Public endpoints phải dùng HTTPS                        | Must Have   | Đặc biệt webhook/payment |
| NFR-025 | Role-based access       | Staff/Admin/Merchant tách quyền rõ                      | Must Have   | Backend enforce          |
| NFR-026 | Audit sensitive actions | Override, plate edit, validation, sync conflict đều log | Must Have   | Forensics                |
| NFR-027 | Local cache protection  | Offline cache có TTL và hạn chế dữ liệu nhạy cảm        | Should Have | Nếu có thể, mã hóa local |
| NFR-028 | Rate limiting           | QR lookup/payment endpoints cần chống spam cơ bản       | Should Have | Gateway level            |
| NFR-029 | Storage access control  | Ảnh xe/plate không public trực tiếp                     | Must Have   | Signed URL/role proxy    |

---

## 8.6 Data Consistency Requirements

| NFR ID  | Requirement              | Target for MVP                                             | Priority  | Notes                       |
| ------- | ------------------------ | ---------------------------------------------------------- | --------- | --------------------------- |
| NFR-030 | Idempotent webhook       | Duplicate webhook không tạo duplicate payment              | Must Have | transaction_id/payment_code |
| NFR-031 | Idempotent offline sync  | Duplicate offline event không ghi trùng                    | Must Have | event_id                    |
| NFR-032 | Event ordering           | Check-out không được đứng trước check-in trong final state | Must Have | Conflict handling           |
| NFR-033 | Pending state visibility | Payment/session/reservation pending phải nhìn thấy         | Must Have | Dashboard                   |
| NFR-034 | Reconciliation schedule  | Job đối soát chạy theo chu kỳ cấu hình                     | Must Have | Đề xuất 1–5 phút trong demo |
| NFR-035 | Server source of truth   | Sau sync, server quyết định final state                    | Must Have | Local chỉ tạm thời          |

---

## 8.7 Usability Requirements

| NFR ID  | Requirement                     | Target for MVP                                    | Priority    | Notes                           |
| ------- | ------------------------------- | ------------------------------------------------- | ----------- | ------------------------------- |
| NFR-036 | Customer no-login               | Khách không cần đăng ký để xem ticket/thanh toán  | Must Have   | Giảm friction                   |
| NFR-037 | Merchant validation steps       | Không quá 3 bước chính                            | Must Have   | Scan QR → nhập invoice → submit |
| NFR-038 | Staff offline status visibility | Staff luôn thấy trạng thái Online/Offline/Syncing | Must Have   | Tránh thao tác nhầm             |
| NFR-039 | Error message clarity           | Lỗi payment/QR/offline phải có hướng xử lý        | Should Have | UX vận hành                     |
| NFR-040 | Mobile responsive               | Customer/Merchant flow dùng tốt trên điện thoại   | Must Have   | Xiaomi 15T test target          |

---

# 9. Deployment & Environment Decision

## 9.1 Deployment Decision

| Area              | Decision                                                  |
| ----------------- | --------------------------------------------------------- |
| Local Development | Docker Compose                                            |
| Backend           | Java Spring Boot microservices                            |
| API Gateway       | Spring Cloud Gateway                                      |
| Frontend          | React + Vite                                              |
| OCR               | Python/FastAPI service                                    |
| Database          | Supabase PostgreSQL ưu tiên                               |
| Storage           | Supabase Storage ưu tiên                                  |
| Cloud Deploy      | Railway cho API Gateway/backend/frontend nếu phù hợp      |
| Payment Webhook   | Public endpoint trên Railway hoặc tunnel trong dev        |
| Payment Mode      | Simulation Mode → SePay Test Mode → Live Mode sau MVP     |
| Fallback Demo     | Local/hybrid nếu Railway cold start hoặc network không ổn |

---

## 9.2 Railway Port Note

Nếu project Railway thực tế cấp public proxy port là `24728`, có thể ghi vào `.env` như một giá trị môi trường:

```text
RAILWAY_DB_PROXY_PORT=24728
```

Tuy nhiên, BRD/SAD không nên ghi `24728` như một chuẩn cố định, vì Railway TCP Proxy tạo domain và port theo từng service/project. Port phải được lấy từ Railway environment thực tế.

---

## 9.3 Recommended Environments

| Environment       | Purpose                  | Payment Mode                              | Database                         | Notes             |
| ----------------- | ------------------------ | ----------------------------------------- | -------------------------------- | ----------------- |
| Local Dev         | Code và test nhanh       | Simulation                                | Local Postgres hoặc Supabase dev | Docker Compose    |
| Integration Test  | Test service integration | Simulation + SePay Test                   | Supabase dev                     | Railway optional  |
| Demo              | Trình bày sản phẩm       | Simulation là bắt buộc, SePay Test nếu ổn | Supabase demo                    | Có local fallback |
| Production Future | Sau MVP                  | SePay Live                                | Managed DB                       | Không thuộc MVP   |

---

# 10. Open Questions

The following questions are intentionally non-blocking and may be resolved during the relevant feature slice:

| Question ID | Question | Priority |
|---|---|---|
| Q-011 | Data retention period for vehicle/plate images | Medium |
| Q-012 | Whether all services or only core services run on Railway for the demo | Medium |
| Q-013 | OCR input starts with upload or camera frames | Medium |
| Q-014 | Exit Pass recovery UX when a customer changes device | Medium |
| Q-015 | Whether dashboard export is included in later hardening | Low |

---

# 11. Recommended Next Steps

1. Validate the canonical business rules during the relevant slice.
2. Keep Simulation first, SePay Test second, and SePay Live disabled.
3. Resolve non-blocking OCR input and reporting UX questions during hardening.
10. Sau offline ổn, thêm merchant aggregation và reservation saga.

---

# 12. BRD Quality Checklist

| Checklist Item                      | Status          | Notes                                                                 |
| ----------------------------------- | --------------- | --------------------------------------------------------------------- |
| Vision rõ ràng                      | Completed       | Đã cập nhật thêm offline/resilience/security                          |
| Problem statement thực tế hơn       | Completed       | Đã bổ sung mất mạng, QR screenshot, payment inconsistency             |
| Stakeholder đầy đủ                  | Completed       | Thêm Payment Owner, DevOps, Security Reviewer                         |
| Scope có In/Out/MVP                 | Completed       | Tách rõ offline, payment simulation, QR security                      |
| Business rules có mã rule           | Completed       | BR-001 đến BR-030                                                     |
| Offline mode có rule                | Completed       | BR-010 đến BR-014                                                     |
| QR security được siết               | Completed       | Lookup QR + Dynamic Exit Pass                                         |
| Distributed transaction được đề cập | Completed       | Reservation saga + reconciliation                                     |
| Invoice aggregation rõ hơn          | Completed       | Cho phép cộng dồn nhiều hóa đơn theo policy                           |
| NFR được bổ sung                    | Completed       | Performance, capacity, availability, security, consistency, usability |
| Deploy decision rõ hơn              | Completed       | Docker + Railway + Supabase/hybrid                                    |
| Payment strategy thực tế            | Completed       | Simulation Mode trước, SePay Test Mode sau                            |
| Risks/issues cập nhật               | Completed       | Thêm offline, payment, QR, sync conflict                              |
| Sẵn sàng chuyển PRD                 | Completed       | Có scope và rule đủ chi tiết                                          |
| Sẵn sàng chuyển SAD                 | Needs Follow-up | Cần viết SAD riêng cho architecture, saga, sync, deployment           |
