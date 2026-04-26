import { AlertCircle, CheckCircle2, Info } from "lucide-react";

const ICONS = {
  error: AlertCircle,
  success: CheckCircle2,
  info: Info,
};

export default function FeedbackBanner({ error, kind = "error", title }) {
  if (!error) {
    return null;
  }

  const message = typeof error === "string" ? error : error.message;
  const validationErrors = typeof error === "object" ? error.validationErrors || {} : {};
  const entries = Object.entries(validationErrors);
  const Icon = ICONS[kind] || AlertCircle;

  return (
    <div className={`feedback-banner feedback-banner--${kind}`}>
      <div className="feedback-banner__summary">
        <div className="feedback-banner__icon">
          <Icon size={18} />
        </div>
        <div>
          <strong>{title || (kind === "success" ? "Success" : "Action needed")}</strong>
          <p>{message}</p>
        </div>
      </div>
      {entries.length > 0 ? (
        <ul className="feedback-list">
          {entries.map(([field, fieldMessage]) => (
            <li key={field}>
              <span>{field}</span>
              <span>{fieldMessage}</span>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
