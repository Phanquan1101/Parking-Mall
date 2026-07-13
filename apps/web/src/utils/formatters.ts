export const dashIfEmpty = (value: string | number | null | undefined) => value === null || value === undefined || value === "" ? "—" : value;

export const formatDateTime = (value?: string) => {
  if (!value) return "—";
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? "—" : parsed.toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" });
};

export const formatPercent = (value?: number) => value === null || value === undefined ? "—" : `${Math.round(value * 100)}%`;

export const formatStatusLabel = (value: string) => ({ MOTORBIKE: "Xe máy", CAR: "Ô tô" }[value] ?? value.replaceAll("_", " "));

export const formatCurrencyVnd = (value?: number) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value ?? 0);
