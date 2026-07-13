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
import { AppShell } from "./components/ui/AppShell";
import { PublicShell } from "./components/ui/PublicShell";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<PublicShell><HomePage /></PublicShell>} />
      <Route path="/tickets/:lookupToken?" element={<PublicShell><CustomerTicketPage /></PublicShell>} />
      <Route path="/reservations/new" element={<PublicShell><ReservationNewPage /></PublicShell>} />
      <Route path="/reservations/:reservationCode" element={<PublicShell><ReservationPage /></PublicShell>} />
      <Route path="/staff/offline" element={<AppShell><StaffOfflinePage /></AppShell>} />
      <Route path="/merchant/validate" element={<AppShell><MerchantValidationPage /></AppShell>} />
      <Route path="/staff/ocr-checkin" element={<AppShell><StaffOcrCheckInPage /></AppShell>} />
      <Route path="/staff/gate-entry" element={<AppShell><StaffGateEntryPage /></AppShell>} />
      <Route path="/dashboard" element={<AppShell><DashboardPage /></AppShell>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
