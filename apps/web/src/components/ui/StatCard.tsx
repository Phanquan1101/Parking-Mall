import type { ReactNode } from "react";

export function StatCard({ label, value, hint, tone = "neutral" }: { label: string; value: ReactNode; hint?: string; tone?: "neutral" | "success" | "warning" | "danger" | "info" }) {
  return <article className={`stat-card stat-card--${tone}`}>
    <span className="stat-card-label">{label}</span>
    <strong>{value}</strong>
    {hint && <small>{hint}</small>}
  </article>;
}
