import { Navigate, Route, Routes } from "react-router-dom";
import { CustomerTicketPage } from "./pages/CustomerTicketPage";
import { HomePage } from "./pages/HomePage";
import { StaffOfflinePage } from "./pages/StaffOfflinePage";
import { MerchantValidationPage } from "./pages/MerchantValidationPage";
import { ReservationNewPage } from "./pages/ReservationNewPage";
import { ReservationPage } from "./pages/ReservationPage";
import { StaffOcrCheckInPage } from "./pages/StaffOcrCheckInPage";
import { DashboardPage } from "./pages/DashboardPage";
import { StaffGateEntryPage } from "./pages/StaffGateEntryPage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/tickets/:lookupToken?" element={<CustomerTicketPage />} />
      <Route path="/staff/offline" element={<StaffOfflinePage />} />
      <Route path="/merchant/validate" element={<MerchantValidationPage />} />
      <Route path="/reservations/new" element={<ReservationNewPage />} />
      <Route path="/reservations/:reservationCode" element={<ReservationPage />} />
      <Route path="/staff/ocr-checkin" element={<StaffOcrCheckInPage />} />
      <Route path="/staff/gate-entry" element={<StaffGateEntryPage />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
