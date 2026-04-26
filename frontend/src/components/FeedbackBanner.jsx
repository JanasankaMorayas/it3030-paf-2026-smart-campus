export default function FeedbackBanner({ error, kind = "error", title }) {
  if (!error) {
    return null;
  }

  const message = typeof error === "string" ? error : error.message;
  const validationErrors = typeof error === "object" ? error.validationErrors || {} : {};
  const entries = Object.entries(validationErrors);

  return (
    <div className={`feedback-banner feedback-banner--${kind}`}>
      <div>
        <strong>{title || (kind === "success" ? "Success" : "Action needed")}</strong>
        <p>{message}</p>
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
