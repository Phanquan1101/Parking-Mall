import { useState } from "react";

export function CopyButton({ value, label = "Sao chép" }: { value: string; label?: string }) {
  const [copied, setCopied] = useState(false);
  async function copy() {
    try { await navigator.clipboard.writeText(value); setCopied(true); window.setTimeout(() => setCopied(false), 1800); }
    catch { setCopied(false); }
  }
  return <button type="button" className="secondary-button copy-button" onClick={() => void copy()}>{copied ? "Đã sao chép" : label}</button>;
}
