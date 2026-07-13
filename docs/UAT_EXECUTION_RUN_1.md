# ParkFlow Mall — UAT Execution Run 1

## Hướng dẫn cho tester

1. Chạy thủ công từng test case theo thứ tự hoặc theo module; không đánh dấu kết quả khi chưa thực hiện.
2. Điền **Actual Result** bằng hành vi/response quan sát được. Không ghi JWT, Gemini API key hay internal service token vào tài liệu.
3. Chọn **Status**: `Pass`, `Fail`, `Blocked` hoặc `Not Run`. Mặc định tất cả case là `Not Run`.
4. Với `Fail` hoặc `Blocked`, đính kèm screenshot, browser console log, service log, Postman request/response đã che token, và ghi rõ evidence ở cột Evidence.
5. Không bỏ qua negative cases, authorization, QR security, idempotency, duplicate và offline conflict cases.
6. Khi có liên quan, ghi account, URL/base URL, biển số, `lookupToken`, `reservationCode`, `invoiceCode`, `sessionId`, `paymentOrderId` hoặc `exitPassToken` vào Notes. Chỉ ghi token đã che bớt nếu cần.

## Môi trường test

| Hạng mục | Giá trị / lưu ý |
|---|---|
| Frontend URL | `http://localhost:5173` |
| API Gateway hybrid local | `http://localhost:8080` |
| API Gateway Docker full-stack | `http://localhost:18080` |
| Demo accounts | `admin/admin123` (`ADMIN`), `staff/staff123` (`PARKING_STAFF`), `merchant/merchant123` (`MERCHANT_STAFF`) |
| Gemini thực | Vision Service cần `VISION_OCR_PROVIDER=GEMINI` và `GEMINI_API_KEY`; API key không được ghi vào sheet hoặc browser. |
| Fallback OCR | `DEMO_OCR` chỉ dùng cho fallback/demo local. |
| Cảnh báo dữ liệu | Nhiều service dùng in-memory; restart có thể xóa session, payment, reservation, invoice và reconciliation data. |

Chọn đúng một Gateway cho mỗi lượt chạy. Với Docker, cập nhật Postman `baseUrl` và `VITE_API_BASE_URL` tương ứng; không trộn `8080` và `18080`.

## Mẫu ghi lỗi

Dùng mẫu này cho mỗi case `Fail` hoặc `Blocked`; có thể dán vào Notes hoặc tạo bug note riêng rồi liên kết tại cột Evidence.

```text
Bug ID:
UAT ID:
Module:
Severity: S1 Critical / S2 High / S3 Medium / S4 Low
Expected:
Actual:
Steps to reproduce:
Screenshot/log/API evidence:
Account used:
Test data:
Notes:
```

## Danh sách ca kiểm thử Run 1

Tổng số: **99**. Tất cả status bên dưới được khởi tạo là **Not Run**; Actual Result, Evidence và Notes để trống cho tester điền.

### A. Authentication and roles

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-A01 | Auth | Admin login | Gateway vÃ  Identity Ä‘ang cháº¡y | Postman: login `admin/admin123` | `200`, nháº­n JWT vÃ  role `ADMIN` |  | Not Run |  |  |
| UAT-A02 | Auth | Staff login | NhÆ° trÃªn | Login `staff/staff123` | `200`, nháº­n JWT vÃ  role `PARKING_STAFF` |  | Not Run |  |  |
| UAT-A03 | Auth | Merchant login | NhÆ° trÃªn | Login `merchant/merchant123` | `200`, nháº­n JWT vÃ  role `MERCHANT_STAFF` |  | Not Run |  |  |
| UAT-A04 | Auth | Invalid login | NhÆ° trÃªn | Login sai password | `401`; khÃ´ng tráº£ JWT hay chi tiáº¿t nháº¡y cáº£m |  | Not Run |  |  |
| UAT-A05 | Auth | Current user | CÃ³ JWT admin há»£p lá»‡ | `GET /api/auth/me` vá»›i Bearer token | `200`, user/role Ä‘Ãºng vá»›i token |  | Not Run |  |  |
| UAT-A06 | Auth | Missing token | Gateway Ä‘ang cháº¡y | Gá»i API staff-protected khÃ´ng cÃ³ Authorization | `401` hoáº·c lá»—i unauthorized an toÃ n |  | Not Run |  |  |
| UAT-A07 | Auth | Invalid token | Gateway Ä‘ang cháº¡y | Gá»i API protected vá»›i JWT giáº£/háº¿t háº¡n | Bá»‹ tá»« chá»‘i; khÃ´ng lá»™ stack trace |  | Not Run |  |  |
| UAT-A08 | Auth | Merchant forbidden staff/admin APIs | CÃ³ JWT merchant | Gá»i check-in, dashboard parking list vÃ  reconciliation run | CÃ¡c API staff/admin bá»‹ `403`; merchant validation váº«n lÃ  pháº¡m vi Ä‘Æ°á»£c phÃ©p |  | Not Run |  |  |

### B. Homepage and navigation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-B01 | Homepage | Homepage loads | Frontend Ä‘ang cháº¡y | Má»Ÿ `/` | Hero, route cards vÃ  cáº£nh bÃ¡o QR Lookup hiá»ƒn thá»‹; khÃ´ng cÃ³ blank page |  | Not Run |  |  |
| UAT-B02 | Homepage | Demo route cards work | Frontend Ä‘ang cháº¡y | Chá»n cÃ¡c card Reservation, Gate Entry, Merchant, Dashboard | Äiá»u hÆ°á»›ng Ä‘Ãºng route, khÃ´ng Ä‘á»•i hÃ nh vi API |  | Not Run |  |  |
| UAT-B03 | Navigation | AppShell staff navigation | Frontend Ä‘ang cháº¡y | Má»Ÿ má»™t staff route; dÃ¹ng menu Dashboard/Gate/OCR/Offline/Merchant | CÃ¡c link hoáº¡t Ä‘á»™ng vÃ  route hiá»‡n táº¡i dá»… nháº­n biáº¿t |  | Not Run |  |  |
| UAT-B04 | Navigation | PublicShell pages work | Frontend Ä‘ang cháº¡y | Má»Ÿ `/`, `/reservations/new`, `/tickets/<token>` | Public pages hiá»ƒn thá»‹ Ä‘Ãºng shell, khÃ´ng yÃªu cáº§u JWT Ä‘á»ƒ má»Ÿ ticket/reservation public |  | Not Run |  |  |
| UAT-B05 | Responsive | Mobile customer ticket layout | CÃ³ `lookupToken` há»£p lá»‡ | Má»Ÿ ticket á»Ÿ viewport khoáº£ng 390px | ThÃ´ng tin vÃ©, nÃºt payment/Exit Pass vÃ  token khÃ´ng trÃ n ngang; controls dÃ¹ng Ä‘Æ°á»£c |  | Not Run |  |  |

### C. Live Gate Entry

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-C01 | Gate Entry | Start camera | Staff JWT; camera cÃ³ sáºµn | Má»Ÿ `/staff/gate-entry`, dÃ¡n staff JWT, chá»n Start camera | Preview vÃ  tráº¡ng thÃ¡i scanning hiá»ƒn thá»‹; camera chá»‰ dÃ¹ng khi ngÆ°á»i dÃ¹ng cáº¥p quyá»n |  | Not Run |  |  |
| UAT-C02 | Gate Entry | Deny camera permission | Browser cÃ³ camera | Cháº·n quyá»n camera rá»“i Start camera | ThÃ´ng bÃ¡o dá»… hiá»ƒu vÃ  váº«n cÃ³ manual fallback |  | Not Run |  |  |
| UAT-C03 | Gate Entry | No camera available | MÃ¡y khÃ´ng cÃ³ camera hoáº·c chá»n thiáº¿t bá»‹ khÃ´ng tá»“n táº¡i | Má»Ÿ Gate Entry, Start camera | BÃ¡o khÃ´ng cÃ³/khÃ´ng má»Ÿ Ä‘Æ°á»£c camera an toÃ n; khÃ´ng crash UI |  | Not Run |  |  |
| UAT-C04 | Gate Entry | Gemini OCR recognizes plate | `GEMINI`, API key há»£p lá»‡, biá»ƒn rÃµ | Scan áº£nh/biá»ƒn `59A1-12345` | Candidate, provider `GEMINI`, confidence/warning hiá»ƒn thá»‹; chÆ°a check-in tá»± Ä‘á»™ng |  | Not Run |  |  |
| UAT-C05 | Gate Entry | Low confidence manual correction | Vision tráº£ confidence tháº¥p/khÃ´ng cháº¯c cháº¯n | Scan biá»ƒn khÃ³ Ä‘á»c, chá»‰nh candidate | NhÃ¢n viÃªn cÃ³ thá»ƒ sá»­a biá»ƒn vÃ  pháº£i xÃ¡c nháº­n trÆ°á»›c check-in |  | Not Run |  |  |
| UAT-C06 | Gate Entry | Enter confirms check-in | Candidate/biá»ƒn há»£p lá»‡; staff JWT | Äiá»n biá»ƒn, nháº¥n Enter hoáº·c nÃºt xÃ¡c nháº­n | Má»™t session `ACTIVE/UNPAID` Ä‘Æ°á»£c táº¡o, cÃ³ QR Lookup/customer ticket handoff |  | Not Run |  |  |
| UAT-C07 | Gate Entry | Double Enter no duplicate | Sau UAT-C06, giá»¯ cÃ¹ng tráº¡ng thÃ¡i submit | Nháº¥n Enter nhanh hai láº§n | Chá»‰ má»™t request/session Ä‘Æ°á»£c táº¡o; UI chá»‘ng double submit |  | Not Run |  |  |
| UAT-C08 | Gate Entry | QR ticket appears | Check-in thÃ nh cÃ´ng | Kiá»ƒm tra káº¿t quáº£ Gate Entry vÃ  má»Ÿ ticket link | CÃ³ `lookupToken`/ticket link; ticket cÃ´ng khai má»Ÿ Ä‘Æ°á»£c |  | Not Run |  |  |
| UAT-C09 | Gate Entry | Next Vehicle resets | Äang hiá»ƒn thá»‹ káº¿t quáº£ check-in | Chá»n Next Vehicle | Form/scan state trá»Ÿ vá» sáºµn sÃ ng, khÃ´ng tÃ¡i sá»­ dá»¥ng dá»¯ liá»‡u xe cÅ© |  | Not Run |  |  |
| UAT-C10 | Gate Entry | Same-plate cooldown | Vá»«a scan/check-in má»™t biá»ƒn | Cá»‘ scan láº¡i biá»ƒn ngay trong cooldown | UI giáº£i thÃ­ch cooldown; backend váº«n lÃ  nguá»“n quyáº¿t Ä‘á»‹nh duplicate plate |  | Not Run |  |  |
| UAT-C11 | Gate Entry | Manual fallback check-in | Camera/OCR khÃ´ng dÃ¹ng Ä‘Æ°á»£c | DÃ¹ng nháº­p tay biá»ƒn `51F1-99999`, xÃ¡c nháº­n | Check-in thÃ nh cÃ´ng vá»›i nguá»“n `MANUAL` náº¿u biá»ƒn chÆ°a active |  | Not Run |  |  |
| UAT-C12 | Gate Entry | Duplicate active plate rejected | CÃ³ session active cÃ¹ng biá»ƒn | Check-in láº¡i cÃ¹ng biá»ƒn qua Gate/manual | Bá»‹ tá»« chá»‘i theo business rule; khÃ´ng táº¡o session thá»© hai |  | Not Run |  |  |
| UAT-C13 | Gate Entry | Merchant cannot OCR/check-in | CÃ³ JWT merchant | DÃ¡n token merchant vÃ  gá»i Gate/OCR flow | Vision/check-in bá»‹ tá»« chá»‘i theo role; khÃ´ng táº¡o session |  | Not Run |  |  |

### D. OCR Upload

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-D01 | OCR Upload | Upload valid image | Staff JWT; JPEG/PNG/WebP há»£p lá»‡ | Má»Ÿ `/staff/ocr-checkin`, upload áº£nh biá»ƒn | CÃ³ candidate/metadata hoáº·c warning an toÃ n; khÃ´ng tá»± táº¡o session |  | Not Run |  |  |
| UAT-D02 | OCR Upload | Missing image | Staff JWT | Gá»­i form/call API khÃ´ng cÃ³ field `image` | `400` hoáº·c lá»—i UI rÃµ rÃ ng; khÃ´ng gá»i check-in |  | Not Run |  |  |
| UAT-D03 | OCR Upload | Invalid file type | Staff JWT; file `.txt`/khÃ´ng há»— trá»£ | Upload file khÃ´ng pháº£i JPEG/PNG/WebP | Bá»‹ tá»« chá»‘i rÃµ rÃ ng; á»©ng dá»¥ng váº«n dÃ¹ng Ä‘Æ°á»£c |  | Not Run |  |  |
| UAT-D04 | OCR Upload | GEMINI provider | `VISION_OCR_PROVIDER=GEMINI`, key há»£p lá»‡ | Upload áº£nh há»£p lá»‡ | Response/UI ghi provider `GEMINI`; key khÃ´ng hiá»ƒn thá»‹ á»Ÿ browser |  | Not Run |  |  |
| UAT-D05 | OCR Upload | DEMO provider | `VISION_OCR_PROVIDER=DEMO_OCR` | Upload test image demo | Response/UI ghi `DEMO_OCR`; mÃ´ táº£ Ä‘Ã¢y lÃ  fallback/demo |  | Not Run |  |  |
| UAT-D06 | OCR Upload | Merchant forbidden | CÃ³ JWT merchant | Gá»i `POST /api/vision/ocr/plate` | `403`; khÃ´ng tráº£ candidate hoáº·c dá»¯ liá»‡u áº£nh |  | Not Run |  |  |
| UAT-D07 | OCR Upload | Staff confirmation required | CÃ³ OCR candidate | Chá»‰nh/giá»¯ candidate rá»“i kiá»ƒm tra trÆ°á»›c nÃºt check-in | KhÃ´ng cÃ³ check-in tá»± Ä‘á»™ng; staff pháº£i xÃ¡c nháº­n biá»ƒn |  | Not Run |  |  |

### E. Reservation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-E01 | Reservation | Create reservation | Reservation service Ä‘ang cháº¡y | `/reservations/new`: táº¡o `59A1-12345` vá»›i khung giá» há»£p lá»‡ | Nháº­n opaque `reservationCode`, tráº¡ng thÃ¡i `RESERVED` |  | Not Run |  |  |
| UAT-E02 | Reservation | Get reservation | CÃ³ code UAT-E01 | Má»Ÿ `/reservations/{reservationCode}` hoáº·c public GET | Tháº¥y biá»ƒn, thá»i gian, tráº¡ng thÃ¡i; khÃ´ng cáº§n JWT |  | Not Run |  |  |
| UAT-E03 | Reservation | Cancel reservation | CÃ³ reservation `RESERVED` chÆ°a dÃ¹ng | Cancel á»Ÿ detail/API | ThÃ nh `CANCELLED`; khÃ´ng cÃ²n dÃ¹ng Ä‘á»ƒ check-in |  | Not Run |  |  |
| UAT-E04 | Reservation | Use reservation in check-in | CÃ³ reservation `RESERVED` há»£p lá»‡ | Gate/manual check-in cÃ¹ng biá»ƒn/type, nháº­p `reservationCode` | Reservation Ä‘Æ°á»£c consume, táº¡o session `UNPAID` bÃ¬nh thÆ°á»ng |  | Not Run |  |  |
| UAT-E05 | Reservation | Wrong plate rejected | CÃ³ reservation há»£p lá»‡ cho biá»ƒn khÃ¡c | Check-in vá»›i code nhÆ°ng biá»ƒn khÃ¡c | Bá»‹ tá»« chá»‘i; reservation khÃ´ng bá»‹ consume |  | Not Run |  |  |
| UAT-E06 | Reservation | Reused reservation rejected | CÃ³ reservation Ä‘Ã£ `CONSUMED` | DÃ¹ng code Ä‘Ã³ cho check-in láº§n ná»¯a | Bá»‹ tá»« chá»‘i; khÃ´ng táº¡o session má»›i |  | Not Run |  |  |
| UAT-E07 | Reservation | Expired reservation behavior | CÃ³ reservation quÃ¡ háº¡n hoáº·c gá»i expire theo role staff/admin | Má»Ÿ/dÃ¹ng reservation Ä‘Ã£ expire | Tráº¡ng thÃ¡i `EXPIRED`; check-in bá»‹ tá»« chá»‘i |  | Not Run |  |  |
| UAT-E08 | Reservation | Duplicate active reservation per plate | CÃ³ reservation `RESERVED` cho cÃ¹ng biá»ƒn | Táº¡o reservation active thá»© hai cÃ¹ng biá»ƒn | Theo hÃ nh vi service: bá»‹ tá»« chá»‘i hoáº·c hiá»ƒn thá»‹ tráº¡ng thÃ¡i an toÃ n, khÃ´ng Ã¢m tháº§m táº¡o hold mÃ¢u thuáº«n |  | Not Run |  |  |
| UAT-E09 | Reservation | Merchant cannot list reservations | CÃ³ JWT merchant | `GET /api/reservations` | `403` |  | Not Run |  |  |
| UAT-E10 | Reservation | Merchant cannot expire reservations | CÃ³ JWT merchant | `POST /api/reservations/expire` | `403`; khÃ´ng Ä‘á»•i dá»¯ liá»‡u |  | Not Run |  |  |

### F. Parking session and QR ticket

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-F01 | Parking | Manual check-in | Staff JWT; biá»ƒn chÆ°a active | Postman/Gate: check-in `59C1-11111`, `MANUAL` | Session active, unpaid, `sessionId`, `sessionCode`, `lookupToken` Ä‘Æ°á»£c tráº£ |  | Not Run |  |  |
| UAT-F02 | Parking | OCR-assisted check-in | Staff JWT; OCR request há»£p lá»‡ | Confirm OCR candidate qua Gate/OCR page | Session ghi nguá»“n `OCR_ASSISTED`; biá»ƒn confirmed do staff quyáº¿t Ä‘á»‹nh |  | Not Run |  |  |
| UAT-F03 | Parking | Reservation check-in | Reservation há»£p lá»‡ | Check-in cÃ³ `reservationCode` | Session má»›i liÃªn káº¿t reservation/consume thÃ nh cÃ´ng |  | Not Run |  |  |
| UAT-F04 | Parking | Duplicate active plate | CÃ³ session active | Gá»i check-in cÃ¹ng normalized plate | Business violation; khÃ´ng cÃ³ session trÃ¹ng |  | Not Run |  |  |
| UAT-F05 | QR Ticket | Public ticket lookup | CÃ³ lookup token há»£p lá»‡ | `GET /api/public/tickets/{lookupToken}` hoáº·c má»Ÿ `/tickets/{lookupToken}` | Chá»‰ dá»¯ liá»‡u vÃ© an toÃ n: biá»ƒn, phÃ­, status; khÃ´ng lá»™ staff/internal events |  | Not Run |  |  |
| UAT-F06 | QR Ticket | Invalid ticket token | Token giáº£ | Má»Ÿ `/tickets/invalid-token` | Safe not-found/404, UI khÃ´ng crash |  | Not Run |  |  |
| UAT-F07 | QR Security | QR Lookup is not Exit Pass | CÃ³ lookup token session chÆ°a/Ä‘Ã£ paid | DÃ¹ng lookup token thay `exitPassToken` khi validate/check-out | Bá»‹ tá»« chá»‘i; QR Lookup khÃ´ng má»Ÿ cá»•ng |  | Not Run |  |  |

### G. Merchant invoice aggregation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-G01 | Merchant | Validate first invoice | Merchant JWT; active ticket | `/merchant/validate`: token, `INV-UAT-001`, `150000` | Validation thÃ nh cÃ´ng, tá»•ng eligible lÃ  `150000` |  | Not Run |  |  |
| UAT-G02 | Merchant | Aggregate two invoices | Sau G01 | Validate `INV-UAT-002`, `150000` cho cÃ¹ng ticket | Tá»•ng lÃ  `300000`; policy aggregate Ä‘Æ°á»£c Ã¡p dá»¥ng |  | Not Run |  |  |
| UAT-G03 | Merchant | Discount appears | Sau G02 | Refresh customer ticket | Discount `5000`, `finalFee` Ä‘Æ°á»£c cáº­p nháº­t vÃ  khÃ´ng Ã¢m |  | Not Run |  |  |
| UAT-G04 | Merchant | Duplicate invoice rejected | `INV-UAT-001` Ä‘Ã£ dÃ¹ng | Submit láº¡i code Ä‘Ã³, cÃ¹ng hoáº·c ticket khÃ¡c | Bá»‹ tá»« chá»‘i, khÃ´ng tÄƒng discount/tá»•ng |  | Not Run |  |  |
| UAT-G05 | Merchant | Invalid lookup token | Merchant JWT; token giáº£ | Submit validation | Lá»—i an toÃ n, khÃ´ng táº¡o validation |  | Not Run |  |  |
| UAT-G06 | Merchant | Merchant result page display | CÃ³ validation thÃ nh cÃ´ng/tháº¥t báº¡i | Quan sÃ¡t result/error page | Káº¿t quáº£ aggregate, discount vÃ  lá»—i duplicate rÃµ rÃ ng |  | Not Run |  |  |
| UAT-G07 | Merchant | Ticket shows discount/finalFee | CÃ³ validation thÃ nh cÃ´ng | Má»Ÿ customer ticket tÆ°Æ¡ng á»©ng | `totalEligibleInvoiceAmount`, discount vÃ  final fee nháº¥t quÃ¡n |  | Not Run |  |  |

### H. Payment simulation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-H01 | Payment | Create payment order | Active unpaid ticket | Ticket page: Táº¡o lá»‡nh thanh toÃ¡n, hoáº·c `POST /api/payments/orders` | CÃ³ `paymentOrderId`, `paymentCode`, amount Ä‘Ãºng final fee, `PENDING` |  | Not Run |  |  |
| UAT-H02 | Payment | Simulate success | CÃ³ pending order; mode `SIMULATION` | Simulate success vá»›i payment code/amount Ä‘Ãºng vÃ  `Idempotency-Key` | Order/session chuyá»ƒn `PAID` |  | Not Run |  |  |
| UAT-H03 | Payment | Wrong paymentCode rejected | CÃ³ pending order | Simulate vá»›i payment code khÃ¡c | KhÃ´ng cáº­p nháº­t Parking sang paid; lá»—i/mismatch an toÃ n |  | Not Run |  |  |
| UAT-H04 | Payment | Wrong amount rejected | CÃ³ pending order | Simulate amount khÃ¡c order | KhÃ´ng cáº­p nháº­t Parking sang paid; cÃ³ mismatch/manual review náº¿u Ã¡p dá»¥ng |  | Not Run |  |  |
| UAT-H05 | Payment | Duplicate success idempotency | CÃ³ payment Ä‘Ã£ xá»­ lÃ½ | Gá»­i láº¡i exact simulate request vá»›i cÃ¹ng `Idempotency-Key` | Nháº­n káº¿t quáº£ idempotent; khÃ´ng double-pay/duplicate transaction |  | Not Run |  |  |
| UAT-H06 | Payment | Ticket reflects PAID | H02 thÃ nh cÃ´ng | Refresh customer ticket | Badge payment `PAID`; UI cho táº¡o Exit Pass khi eligible |  | Not Run |  |  |
| UAT-H07 | Payment | No unnecessary reconciliation | Payment success Ä‘Ã£ cáº­p nháº­t Parking | Admin run/list reconciliation | KhÃ´ng táº¡o OPEN item chá»‰ vÃ¬ payment PAID Ä‘Ã£ `UPDATED` |  | Not Run |  |  |

### I. Dynamic Exit Pass and checkout

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-I01 | Exit Pass | Generate after paid | Ticket payment `PAID` hoáº·c zero fee | Ticket page: Táº¡o Exit Pass | Token opaque, `ACTIVE`, TTL ngáº¯n (máº·c Ä‘á»‹nh 60 giÃ¢y) |  | Not Run |  |  |
| UAT-I02 | Exit Pass | Cannot generate before paid | Session `UNPAID` vÃ  final fee > 0 | Gá»i generate Exit Pass | Bá»‹ tá»« chá»‘i `SESSION_NOT_PAID`/thÃ´ng bÃ¡o tÆ°Æ¡ng Ä‘Æ°Æ¡ng |  | Not Run |  |  |
| UAT-I03 | Exit Pass | Staff validates pass | Staff JWT; pass active; plate Ä‘Ãºng | `POST /api/parking/exit-passes/{token}/validate` vá»›i `GATE_OUT_01`, plate | Validation thÃ nh cÃ´ng nhÆ°ng chÆ°a consume pass/session |  | Not Run |  |  |
| UAT-I04 | Exit Pass | Plate mismatch rejected | Staff JWT; pass active | Validate/check-out báº±ng biá»ƒn khÃ¡c | `PLATE_MISMATCH`; khÃ´ng auto exit |  | Not Run |  |  |
| UAT-I05 | Checkout | Checkout success | Staff JWT; paid session; valid pass; plate Ä‘Ãºng | `POST /api/parking/sessions/{sessionId}/check-out` | Pass consumed, session `EXITED`, exit details Ä‘Æ°á»£c tráº£ |  | Not Run |  |  |
| UAT-I06 | Exit Pass | Reused pass rejected | I05 thÃ nh cÃ´ng | Gá»i validate/check-out láº¡i cÃ¹ng pass | `EXIT_PASS_ALREADY_USED` hoáº·c lá»—i safe tÆ°Æ¡ng Ä‘Æ°Æ¡ng |  | Not Run |  |  |
| UAT-I07 | QR Security | Lookup token cannot be exit pass | CÃ³ lookup token | Gá»i validate/check-out dÃ¹ng lookup token | Tá»« chá»‘i; khÃ´ng Ä‘á»•i session |  | Not Run |  |  |
| UAT-I08 | Manual override | Override requires reason | Staff/admin JWT; paid/zero-fee session | Gá»i manual override thiáº¿u `reason`, sau Ä‘Ã³ cÃ³ reason | Thiáº¿u reason bá»‹ tá»« chá»‘i; reason há»£p lá»‡ táº¡o audit/override record theo thiáº¿t káº¿ |  | Not Run |  |  |
| UAT-I09 | Manual override | Override does not bypass payment | Session unpaid cÃ³ fee > 0 | Gá»i manual override cÃ³ reason | Bá»‹ tá»« chá»‘i vÃ¬ chÆ°a paid; khÃ´ng checkout |  | Not Run |  |  |

### J. Offline staff mode

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-J01 | Offline | Create offline event | Staff JWT Ä‘Ã£ dÃ¡n; offline mode browser | `/staff/offline`: táº¯t máº¡ng, thÃªm check-in `59B2-67890` | Event `PENDING` Ä‘Æ°á»£c táº¡o cá»¥c bá»™; chÆ°a lÃ  official session |  | Not Run |  |  |
| UAT-J02 | Offline | Queue persists localStorage | CÃ³ event UAT-J01 | Refresh browser/Ä‘Ã³ng-má»Ÿ tab | Event cÃ²n trong local queue cÃ¹ng tráº¡ng thÃ¡i |  | Not Run |  |  |
| UAT-J03 | Offline | Sync success | CÃ³ PENDING event há»£p lá»‡; online; staff JWT | Báº­t máº¡ng, chá»n Sync now | Event `SYNCED`; cÃ³ official server session/code |  | Not Run |  |  |
| UAT-J04 | Offline | Duplicate offline event | CÃ³ event Ä‘Ã£ sync hoáº·c copy same event through API | Gá»­i láº¡i eventId/idempotency key | `DUPLICATE`; khÃ´ng táº¡o official session má»›i |  | Not Run |  |  |
| UAT-J05 | Offline | Same active plate conflict | Online Ä‘Ã£ cÃ³ active session cÃ¹ng biá»ƒn; táº¡o event offline cÃ¹ng biá»ƒn | Äá»“ng bá»™ event | Káº¿t quáº£ `CONFLICT`, váº«n tháº¥y trong queue Ä‘á»ƒ review |  | Not Run |  |  |
| UAT-J06 | Offline | Rejected/conflict visible | CÃ³ rejected/conflict result | Má»Ÿ Offline page | Badge/count/message rÃµ rÃ ng; khÃ´ng bá»‹ xÃ³a im láº·ng |  | Not Run |  |  |
| UAT-J07 | Offline | Server source of truth visible | CÃ³ báº¥t ká»³ sync result | Kiá»ƒm tra UI vÃ  data sau sync | UI nÃªu server lÃ  nguá»“n chÃ­nh; status/server session tráº£ vá» ghi Ä‘Ã¨ tráº¡ng thÃ¡i local |  | Not Run |  |  |

### K. Payment reconciliation

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-K01 | Reconciliation | Admin can run reconciliation | Admin JWT | `POST /api/payments/reconciliation/run` | Admin Ä‘Æ°á»£c phÃ©p cháº¡y, nháº­n result job/items an toÃ n |  | Not Run |  |  |
| UAT-K02 | Reconciliation | Staff/merchant forbidden | Staff rá»“i merchant JWT | Gá»i run reconciliation | Cáº£ hai bá»‹ `403` |  | Not Run |  |  |
| UAT-K03 | Reconciliation | List items | Admin JWT | `GET /api/payments/reconciliation/items` | List tráº£ vá» an toÃ n, item khÃ´ng ghi Ä‘Ã¨ nhau |  | Not Run |  |  |
| UAT-K04 | Reconciliation | Filter items | Admin JWT; cÃ³ data phÃ¹ há»£p | Láº§n lÆ°á»£t query `status`, `paymentOrderId`, `targetId` | Filter Ä‘Æ°á»£c forward/Ã¡p dá»¥ng theo endpoint há»— trá»£ |  | Not Run |  |  |
| UAT-K05 | Reconciliation | Mismatched payment manual review | CÃ³ order mismatch náº¿u táº¡o Ä‘Æ°á»£c | Cháº¡y/list reconciliation | Mismatch lÃ  `PENDING_MANUAL_REVIEW`, khÃ´ng thÃ nh payment success |  | Not Run |  |  |
| UAT-K06 | Reconciliation | No Exit Pass / checkout side effects | CÃ³ item reconciliation | Cháº¡y job, kiá»ƒm tra session/ticket | Job khÃ´ng táº¡o Exit Pass, khÃ´ng checkout, khÃ´ng refund/Ä‘á»•i merchant discount |  | Not Run |  |  |

### L. Dashboard

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-L01 | Dashboard | Admin loads all sections | Admin JWT | `/dashboard`, dÃ¡n JWT, Refresh | Parking, reservation vÃ  reconciliation sections/metrics load |  | Not Run |  |  |
| UAT-L02 | Dashboard | Staff limited dashboard | Staff JWT | DÃ¡n JWT, Refresh | Parking/reservation load; reconciliation hiá»ƒn thá»‹ notice ADMIN-only thay vÃ¬ crash |  | Not Run |  |  |
| UAT-L03 | Dashboard | Partial error handling | Cháº·n má»™t service/API hoáº·c dÃ¹ng token phÃ¹ há»£p háº¡n cháº¿ | Refresh dashboard | Pháº§n tháº¥t báº¡i cÃ³ message; cÃ¡c pháº§n API thÃ nh cÃ´ng váº«n hiá»ƒn thá»‹ |  | Not Run |  |  |
| UAT-L04 | Dashboard | Metrics reflect sessions/reservations | ÄÃ£ táº¡o session/reservation trong UAT | Refresh | Counts/status vÃ  rows pháº£n Ã¡nh data hiá»‡n táº¡i |  | Not Run |  |  |
| UAT-L05 | Dashboard | Empty state before data | Environment má»›i/restart in-memory | Refresh trÆ°á»›c khi táº¡o data | Empty states rÃµ rÃ ng, khÃ´ng hiá»ƒn thá»‹ sá»‘ liá»‡u bá»‹a Ä‘áº·t |  | Not Run |  |  |
| UAT-L06 | Dashboard | Refresh button | CÃ³ JWT valid | Nháº¥n LÃ m má»›i dá»¯ liá»‡u nhiá»u láº§n há»£p lÃ½ | Disabled/loading state rÃµ rÃ ng; data cáº­p nháº­t khÃ´ng crash |  | Not Run |  |  |

### M. Security and negative cases

| ID | Module | Scenario | Preconditions | Steps | Expected Result | Actual Result | Status | Evidence | Notes |
|---|---|---|---|---|---|---|---|---|---|
| UAT-M01 | Security | Internal endpoints not exposed | Gateway Ä‘ang cháº¡y | Gá»i Gateway `/internal/parking/sessions/{id}/payment-status` vÃ  `/internal/parking/sessions/{id}/discount` | KhÃ´ng Ä‘Æ°á»£c route/publicly exposed qua Gateway |  | Not Run |  |  |
| UAT-M02 | Security | CORS local frontend works | Frontend/Gateway Ä‘ang cháº¡y | DÃ¹ng UI local gá»i API bÃ¬nh thÆ°á»ng | Browser khÃ´ng bá»‹ CORS block trong mode cáº¥u hÃ¬nh Ä‘Æ°á»£c há»— trá»£ |  | Not Run |  |  |
| UAT-M03 | Security | Missing JWT protected APIs | Gateway Ä‘ang cháº¡y | Gá»i check-in, offline sync, OCR, reservation list khÃ´ng Bearer token | Bá»‹ `401`/unauthorized nháº¥t quÃ¡n |  | Not Run |  |  |
| UAT-M04 | Security | Wrong role rejected | CÃ³ token merchant/customer context | Thá»­ endpoint staff/admin tÆ°Æ¡ng á»©ng | Bá»‹ `403`; khÃ´ng cÃ³ thay Ä‘á»•i server state |  | Not Run |  |  |
| UAT-M05 | UX Security | Long token wraps in UI | CÃ³ JWT/token dÃ i | DÃ¡n token dÃ i vÃ o staff/dashboard/merchant fields vÃ  xem ticket token | KhÃ´ng phÃ¡ vá»¡ layout; token cÃ³ thá»ƒ copy an toÃ n |  | Not Run |  |  |
| UAT-M06 | UX | Invalid inputs show friendly errors | Frontend Ä‘ang cháº¡y | Gá»­i thiáº¿u biá»ƒn, thiáº¿u cá»•ng, invoice amount Ã¢m, thá»i gian reservation sai | Lá»—i cÃ³ hÆ°á»›ng dáº«n; khÃ´ng crash vÃ  khÃ´ng táº¡o dá»¯ liá»‡u sai |  | Not Run |  |  |
| UAT-M07 | Security | Error responses safe | CÃ³ lá»—i 401/403/404/validation | Quan sÃ¡t UI vÃ  API response | KhÃ´ng lá»™ JWT, API key, stack trace, internal token hay raw exception |  | Not Run |  |  |

