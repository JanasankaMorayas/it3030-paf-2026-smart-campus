import { titleizeEnum } from "../lib/format.js";

export default function StatusBadge({ value, variant = "" }) {
  if (!value) {
    return <span className="badge badge--muted">-</span>;
  }

  const normalized = String(variant || value).toLowerCase().replaceAll("_", "-");
  return <span className={`badge badge--${normalized}`}>{titleizeEnum(value)}</span>;
}
