# ParkFlow Mall — UAT Run 1 Report

**Ngày lập báo cáo:** 2026-07-13  
**Nguồn đã kiểm tra:** `docs/UAT_CHECKLIST.md`, `docs/DEMO_SCRIPT.md`, `README.md`, `docs/API_CONTRACT.md`, `docs/TEST_MATRIX.md` và các tệp kết quả UAT/log/screenshot có trong repository.

> Trạng thái dữ liệu: repository không có phiếu kết quả đã điền, bug note, log hay screenshot từ tester. Đây là báo cáo triage cho **UAT Run 1 chưa được thực thi**, không phải báo cáo xác nhận chất lượng sản phẩm.

## 1. Summary

| Chỉ số | Kết quả |
|---|---:|
| Tổng test case | 99 |
| Passed | 0 |
| Failed | 0 |
| Blocked | 0 |
| Not Run | 99 |
| Evidence từ tester | Không có |
| Đánh giá overall readiness | Chưa thể đánh giá; cần chạy UAT và cung cấp evidence. |

**Đánh giá:** Không có bằng chứng để kết luận các luồng core hoạt động hoặc có lỗi. Project hiện **Blocked for UAT decision** vì UAT Run 1 chưa có kết quả, không phải vì đã xác nhận lỗi source code.

## 2. Bug List

Không có bug sản phẩm nào được ghi nhận: không có UAT case ở trạng thái `Fail` hoặc `Blocked`, cũng không có bug note/evidence từ tester. Không tạo bug giả từ checklist hoặc từ các giới hạn đã biết của môi trường.

| Bug ID | UAT ID | Severity | Module | Title | Expected | Actual | Evidence | Suspected Area | Status |
|---|---|---|---|---|---|---|---|---|---|
| — | — | — | — | Chưa có lỗi được tester báo cáo | — | 99 case vẫn Not Run | Không có result/log/screenshot | Chưa xác định | Needs Repro |

`Needs Repro` trong dòng trên chỉ mô tả khoảng trống triage, **không phải** một lỗi source code và không được đưa vào backlog fix.

## 3. Failure Analysis by Module

| Module | UAT IDs | Kết quả Run 1 | Phân tích |
|---|---|---|---|
| Auth / Role | UAT-A01–A08 | Not Run | Chưa có JWT/login response hoặc evidence quyền hạn. |
| Gate Entry | UAT-C01–C13 | Not Run | Chưa có evidence camera, check-in, cooldown hoặc duplicate plate. |
| OCR / Gemini | UAT-D01–D07 | Not Run | Chưa biết provider đang cấu hình `GEMINI` hay `DEMO_OCR`; không có camera/upload log. |
| Parking / QR | UAT-F01–F07 | Not Run | Chưa có `sessionId`/`lookupToken` hoặc public-ticket response. |
| Reservation | UAT-E01–E10 | Not Run | Chưa có `reservationCode`, consume/expire response hoặc role evidence. |
| Merchant | UAT-G01–G07 | Not Run | Chưa có validation/duplicate invoice response hoặc ticket fee snapshot. |
| Payment | UAT-H01–H07 | Not Run | Chưa có payment order, simulation response hoặc idempotency evidence. |
| Exit Pass / Checkout | UAT-I01–I09 | Not Run | Chưa có pass TTL/use, plate mismatch, checkout hay manual-override evidence. |
| Offline | UAT-J01–J07 | Not Run | Chưa có local queue, sync result hoặc conflict evidence. |
| Dashboard | UAT-L01–L06 | Not Run | Chưa có admin/staff refresh result hoặc partial-error evidence. |
| UI / Responsive | UAT-B01–B05, UAT-M05–M06 | Not Run | Chưa có browser/device screenshots. |
| Docker / Environment | UAT-M02, prerequisites | Not Run | Chưa có service health, compose status, browser network/CORS log. |

## 4. Root Cause Hypothesis

Không có failure evidence để phân loại nguyên nhân vào frontend UI/state, API integration, backend business logic, auth/role config, environment/config, test data hay expected behavior mismatch.

Nguyên nhân chắc chắn duy nhất ở cấp **triage process** là thiếu đầu vào UAT:

| Item | Classification | Evidence | Hành động cần thiết |
|---|---|---|---|
| Không có kết quả Run 1 | Test execution / evidence gap | 99 dòng trong checklist là `Not Run`; không tìm thấy bug note, screenshot hay log | Tester chạy checklist và nộp Actual Result, Status, Notes, request/response đã che token và screenshot/log khi fail. |

Khi có failure, mỗi bug phải được gán đúng một giả thuyết ban đầu: `frontend UI bug`, `frontend state bug`, `API integration bug`, `backend business logic bug`, `auth/role config bug`, `environment/config issue`, `test data issue` hoặc `expected behavior mismatch`. Giả thuyết không thay thế repro và không được coi là root cause đã xác nhận.

## 5. Fix Priority

Chưa có bug để xếp ưu tiên sửa. Thứ tự xử lý sau khi nhận kết quả UAT là:

1. **Demo-blocking bugs:** login, Gateway, Gate Entry/check-in, customer ticket hoặc frontend không mở được.
2. **Security/business-rule bugs:** QR Lookup dùng được để exit, Exit Pass hết hạn/reuse, payment bypass, role bypass, duplicate payment/invoice hoặc offline state không bảo toàn server source of truth.
3. **Integration bugs:** API Gateway routing, service-to-service update, Gemini/OCR configuration, reservation consume, payment/merchant/dashboard integration.
4. **UI bugs:** màn hình không thao tác được, responsive lỗi, error/empty state không thể hoàn thành flow.
5. **Copy/spacing bugs:** diễn đạt, label, căn chỉnh không cản trở thao tác.

Severity được áp dụng khi triage bug thật: S1 cho demo/core data/security bị chặn; S2 cho flow chính hỏng; S3 khi có workaround; S4 cho UI/copy polish.

## 6. Bug Fix Plan

Không đề xuất thay đổi source code khi chưa có bug được tái hiện. Bảng dưới đây là kế hoạch xử lý cho khoảng trống triage, không phải kế hoạch sửa sản phẩm.

| Item | Proposed fix / action | Files likely involved | Risk level | Tests to rerun |
|---|---|---|---|---|
| TRIAGE-001 | Tester thực thi UAT Run 1 và ghi Actual Result/Status/Notes cho từng case; đính kèm evidence của mọi Fail/Blocked. | `docs/UAT_CHECKLIST.md` và artifact tester cung cấp | Low | UAT-A01–M07 (toàn bộ 99 ca) |
| TRIAGE-002 | Với mỗi Fail, tạo Bug ID, chuẩn hóa request/response đã che token, thời điểm, môi trường, data và repro steps; sau đó cập nhật báo cáo này. | `docs/UAT_RUN_1_REPORT.md`; source chỉ được xác định sau repro | Low | UAT ID liên quan và các regression case của module |
| TRIAGE-003 | Chỉ sau khi triage xác nhận mới đề xuất source files, risk và fix; không sửa theo suy đoán. | TBD sau repro | Medium | Tối thiểu UAT case fail + security/integration regression liên quan |

## 7. Retest Plan

Không có source fix nào được chọn trong báo cáo này nên chưa có targeted retest sau fix. Để tạo baseline Run 1, phải chạy toàn bộ checklist theo thứ tự ưu tiên sau:

1. Core demo/security: **UAT-A01–A08, C01–C13, F01–F07, H01–H07, I01–I09**.
2. Supporting core flow: **UAT-E01–E10, G01–G07, D01–D07, J01–J07**.
3. Operations and presentation: **UAT-L01–L06, B01–B05, M01–M07, K01–K06**.

Sau một bug được sửa, rerun tối thiểu UAT ID của bug, toàn bộ case cùng module, và các regression case nối tiếp trong flow. Ví dụ, bug payment cần rerun UAT-H01–H07 và UAT-I01–I09; bug OCR/check-in cần rerun UAT-C01–C13, UAT-D01–D07 và UAT-F01–F07.

## 8. Decision

**Blocked.** Không có kết quả UAT Run 1 để kết luận **Ready for demo** hoặc **Ready after bug fixes**. Bước tiếp theo là chạy manual UAT theo [UAT_CHECKLIST.md](UAT_CHECKLIST.md), sau đó cung cấp checklist đã điền cùng bug notes, screenshots và logs để triage thành bug plan có thể thực hiện.
