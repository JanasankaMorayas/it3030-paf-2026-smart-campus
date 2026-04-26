import { Inbox } from "lucide-react";

export default function EmptyState({ icon: Icon = Inbox, title, message, action, compact = false }) {
  return (
    <div className={`empty-state ${compact ? "empty-state--compact" : ""}`}>
      <div className="empty-state__icon">
        <Icon size={20} />
      </div>
      {title ? <strong>{title}</strong> : null}
      <p>{message}</p>
      {action ? <div className="empty-state__action">{action}</div> : null}
    </div>
  );
}
