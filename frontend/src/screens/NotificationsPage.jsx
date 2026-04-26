import { CheckCheck, Eye, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import PaginationBar from "../components/PaginationBar.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { formatCompactDateTime } from "../lib/format.js";
import { NOTIFICATION_SORT_OPTIONS, NOTIFICATION_TYPES, PAGE_SIZE_OPTIONS } from "../lib/options.js";
import { api } from "../lib/api.js";

const filterTemplate = {
  unreadOnly: true,
  type: "",
  recipient: "",
  size: 10,
  sort: "createdAt,desc",
};

export default function NotificationsPage() {
  const { isAdmin } = useAuth();
  const [filters, setFilters] = useState(filterTemplate);
  const [appliedFilters, setAppliedFilters] = useState(filterTemplate);
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);

  async function loadNotifications(activePage = page, activeFilters = appliedFilters) {
    setLoading(true);
    setError(null);

    try {
      const loader = activeFilters.unreadOnly ? api.notifications.unread : api.notifications.list;
      const response = await loader({
        page: activePage,
        size: activeFilters.size,
        sort: activeFilters.sort,
        type: activeFilters.unreadOnly ? undefined : activeFilters.type,
        unreadOnly: activeFilters.unreadOnly ? undefined : activeFilters.unreadOnly,
        recipient: activeFilters.recipient,
      });
      setPageData(response);
    } catch (loadError) {
      setError(loadError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadNotifications(page, appliedFilters);
  }, [page, appliedFilters]);

  async function handleMarkRead(id) {
    try {
      await api.notifications.markRead(id);
      setActionMessage({ message: "Notification marked as read." });
      await loadNotifications(page, appliedFilters);
    } catch (markError) {
      setError(markError);
    }
  }

  async function handleMarkAllRead() {
    try {
      const response = await api.notifications.markAllRead(appliedFilters.recipient || undefined);
      setActionMessage({ message: `${response.updatedCount} notifications updated.` });
      await loadNotifications(0, appliedFilters);
      setPage(0);
    } catch (markError) {
      setError(markError);
    }
  }

  async function handleDelete(id) {
    try {
      await api.notifications.remove(id);
      setActionMessage({ message: "Notification deleted." });
      await loadNotifications(page, appliedFilters);
    } catch (deleteError) {
      setError(deleteError);
    }
  }

  const notifications = pageData?.content || [];

  return (
    <div className="page-stack">
      <section className="page-header">
        <div>
          <p className="eyebrow">Module D</p>
          <h2>Notifications</h2>
          <p>Monitor unread items, inbox ownership, and operational event alerts triggered by bookings and tickets.</p>
        </div>
        <button type="button" className="button button--primary" onClick={() => void handleMarkAllRead()}>
          <CheckCheck size={16} />
          Mark all as read
        </button>
      </section>

      <section className="panel">
        <form
          className="filters-grid"
          onSubmit={(event) => {
            event.preventDefault();
            setPage(0);
            setAppliedFilters({ ...filters });
          }}
        >
          <label className="field field--checkbox">
            <input
              type="checkbox"
              checked={filters.unreadOnly}
              onChange={(event) => setFilters((current) => ({ ...current, unreadOnly: event.target.checked }))}
            />
            <span>Unread only</span>
          </label>

          <label className="field">
            <span>Type</span>
            <select
              value={filters.type}
              onChange={(event) => setFilters((current) => ({ ...current, type: event.target.value }))}
              disabled={filters.unreadOnly}
            >
              <option value="">All types</option>
              {NOTIFICATION_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Recipient</span>
            <input
              value={filters.recipient}
              onChange={(event) => setFilters((current) => ({ ...current, recipient: event.target.value }))}
              placeholder={isAdmin ? "Optional inbox filter" : "Your inbox is already scoped"}
              disabled={!isAdmin}
            />
          </label>

          <label className="field">
            <span>Sort</span>
            <select value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))}>
              {NOTIFICATION_SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Page size</span>
            <select value={filters.size} onChange={(event) => setFilters((current) => ({ ...current, size: Number(event.target.value) }))}>
              {PAGE_SIZE_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>

          <div className="filter-actions">
            <button type="submit" className="button button--ghost">
              Apply filters
            </button>
            <button
              type="button"
              className="button button--subtle"
              onClick={() => {
                setFilters(filterTemplate);
                setAppliedFilters(filterTemplate);
                setPage(0);
              }}
            >
              Reset
            </button>
          </div>
        </form>
      </section>

      <FeedbackBanner error={error || actionMessage} kind={error ? "error" : "success"} />

      <section className="panel">
        <div className="panel-header">
          <div>
            <p className="eyebrow">Inbox</p>
            <h3>{loading ? "Loading notifications..." : `${pageData?.totalElements ?? 0} notifications found`}</h3>
          </div>
        </div>

        {notifications.length ? (
          <div className="list-stack">
            {notifications.map((notification) => (
              <article key={notification.id} className="notification-card">
                <div className="notification-card__main">
                  <div className="stack-inline">
                    <StatusBadge value={notification.type} variant="general" />
                    <StatusBadge value={notification.read ? "READ" : "UNREAD"} variant={notification.read ? "active" : "pending"} />
                  </div>
                  <strong>{notification.title}</strong>
                  <p>{notification.message}</p>
                  <div className="notification-card__meta">
                    <span>{notification.recipientEmail}</span>
                    <span>{notification.relatedEntityType || "GENERAL"} #{notification.relatedEntityId || "—"}</span>
                    <span>{formatCompactDateTime(notification.createdAt)}</span>
                  </div>
                </div>

                <div className="table-actions">
                  {!notification.read ? (
                    <button type="button" className="icon-button" onClick={() => void handleMarkRead(notification.id)} aria-label="Mark notification as read">
                      <Eye size={16} />
                    </button>
                  ) : null}
                  <button type="button" className="icon-button icon-button--danger" onClick={() => void handleDelete(notification.id)} aria-label="Delete notification">
                    <Trash2 size={16} />
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="empty-state">No notifications matched the current inbox filters.</div>
        )}

        <PaginationBar pageData={pageData} onPageChange={setPage} />
      </section>
    </div>
  );
}
