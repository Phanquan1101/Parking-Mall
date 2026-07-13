import type { ReactNode } from "react";

export function PageHeader({ eyebrow, title, children, action }: { eyebrow?: string; title: string; children?: ReactNode; action?: ReactNode }) {
  return <section className="page-header">
    <div>{eyebrow && <span className="page-eyebrow">{eyebrow}</span>}<h1>{title}</h1>{children && <p>{children}</p>}</div>
    {action && <div className="page-header-action">{action}</div>}
  </section>;
}
