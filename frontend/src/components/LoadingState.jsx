export default function LoadingState({ title = "Loading data", message = "Pulling the latest backend state.", lines = 3, compact = false }) {
  return (
    <div className={`loading-state ${compact ? "loading-state--compact" : ""}`}>
      <div className="loading-state__pulse" />
      <div className="loading-state__copy">
        <strong>{title}</strong>
        <p>{message}</p>
      </div>
      <div className="loading-state__skeleton">
        {Array.from({ length: lines }).map((_, index) => (
          <span key={index} className="loading-state__line" />
        ))}
      </div>
    </div>
  );
}
