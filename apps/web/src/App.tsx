import { Navigate, Route, Routes } from "react-router-dom";
import { CustomerTicketPage } from "./pages/CustomerTicketPage";
import { HomePage } from "./pages/HomePage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/tickets/:lookupToken?" element={<CustomerTicketPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
