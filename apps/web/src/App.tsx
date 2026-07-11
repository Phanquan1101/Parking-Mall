const placeholders = [
  "Staff Console placeholder",
  "Customer Ticket placeholder",
  "Merchant Dashboard placeholder",
  "Admin Dashboard placeholder",
];

export default function App() {
  return (
    <main>
      <h1>ParkFlow Mall</h1>
      <p>Slice 0 frontend foundation. No API calls, login, or QR scanning are implemented.</p>
      <ul>
        {placeholders.map((placeholder) => (
          <li key={placeholder}>{placeholder}</li>
        ))}
      </ul>
    </main>
  );
}
