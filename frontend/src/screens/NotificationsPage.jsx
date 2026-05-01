import { Bell, CheckCheck, Eye, RefreshCcw, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import ConfirmDialog from "../components/ConfirmDialog.jsx";
import DataToolbar from "../components/DataToolbar.jsx";
import EmptyState from "../components/EmptyState.jsx";
import FeedbackBanner from "../components/FeedbackBanner.jsx";
import LoadingState from "../components/LoadingState.jsx";
import PageShell from "../components/PageShell.jsx";
import PaginationBar from "../components/PaginationBar.jsx";
import SectionCard from "../components/SectionCard.jsx";
import StatCard from "../components/StatCard.jsx";
import StatusBadge from "../components/StatusBadge.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { compactCount, formatCompactDateTime, titleizeEnum } from "../lib/format.js";
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
  const [deleteTarget, setDeleteTarget] = useState(null);

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

  async function handleDeleteConfirmed() {
    if (!deleteTarget) {
      return;
    }

    try {
      await api.notifications.remove(deleteTarget.id);
      setDeleteTarget(null);
      setActionMessage({ message: "Notification deleted." });
      await loadNotifications(page, appliedFilters);
    } catch (deleteError) {
      setError(deleteError);
    }
  }

  const notifications = pageData?.content || [];
  const unreadCount = notifications.filter((notification) => !notification.read).length;
  const readCount = notifications.filter((notification) => notification.read).length;

  return (
    <PageShell
      eyebrow="Module D"
      title="Notification inbox"
      description="Stay on top of booking and ticket events with quick unread focus, bulk actions, and inbox hygiene."
      actions={(
        <button type="button" className="button button--primary" onClick={() => void handleMarkAllRead()}>
          <CheckCheck size={16} />
          Mark all as read
        </button>
      )}
      meta={(
        <>
          <StatusBadge value={isAdmin ? "ADMIN" : "USER"} variant="general" />
          <span className="page-shell__meta-text">
            {isAdmin ? "Admins can inspect all inboxes or filter to a specific recipient." : "Your inbox stays scoped to your authenticated user."}
          </span>
        </>
      )}
    >
      <section className="dashboard-stat-grid dashboard-stat-grid--compact">
        <StatCard icon={Bell} label="Visible notifications" value={loading ? "..." : compactCount(pageData?.totalElements ?? 0)} hint="Current inbox result set" tone="teal" />
        <StatCard icon={Bell} label="Unread on page" value={loading ? "..." : compactCount(unreadCount)} hint="Needs attention in this page slice" tone="sand" />
        <StatCard icon={Eye} label="Read on page" value={loading ? "..." : compactCount(readCount)} hint="Already acknowledged entries" tone="cream" />
        <StatCard icon={CheckCheck} label="Mode" value={filters.unreadOnly ? "Unread" : "All"} hint={filters.unreadOnly ? "Unread-only feed enabled" : "Full inbox mode"} tone="teal" />
      </section>

      <DataToolbar
        eyebrow="Inbox filters"
        title="Shape the notification stream"
        description="Switch between unread focus and full inbox review, then refine by type, recipient, sort order, and page size."
      >
        <form
          onSubmit={(event) => {
            event.preventDefault();
            setPage(0);
            setAppliedFilters({ ...filters });
          }}
          style={{ display: "flex", flexWrap: "wrap", gap: "16px", alignItems: "flex-end", paddingTop: "12px" }}
        >
          <label style={{ display: "flex", alignItems: "center", gap: "8px", flex: "1 1 160px", minWidth: "150px", height: "40px", cursor: "pointer" }}>
            <input
              type="checkbox"
              checked={filters.unreadOnly}
              onChange={(event) => setFilters((current) => ({ ...current, unreadOnly: event.target.checked }))}
              style={{ width: "18px", height: "18px", accentColor: "#2563eb", cursor: "pointer" }}
            />
            <span style={{ fontSize: "13px", fontWeight: "600", color: "#1e293b" }}>Unread only</span>
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 160px", minWidth: "150px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Type</span>
            <select
              value={filters.type}
              onChange={(event) => setFilters((current) => ({ ...current, type: event.target.value }))}
              disabled={filters.unreadOnly}
              style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: filters.unreadOnly ? "#f1f5f9" : "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: filters.unreadOnly ? "not-allowed" : "pointer" }}
            >
              <option value="">All types</option>
              {NOTIFICATION_TYPES.map((type) => (
                <option key={type} value={type}>
                  {titleizeEnum(type)}
                </option>
              ))}
            </select>
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 200px", minWidth: "150px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Recipient</span>
            <input
              value={filters.recipient}
              onChange={(event) => setFilters((current) => ({ ...current, recipient: event.target.value }))}
              placeholder={isAdmin ? "Optional recipient inbox filter" : "Already scoped to your inbox"}
              disabled={!isAdmin}
              style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: !isAdmin ? "#f1f5f9" : "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: !isAdmin ? "not-allowed" : "text" }}
            />
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "1 1 160px", minWidth: "150px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Sort</span>
            <select value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
              {NOTIFICATION_SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label style={{ display: "flex", flexDirection: "column", gap: "6px", flex: "0 0 120px" }}>
            <span style={{ fontSize: "12px", fontWeight: "600", color: "#64748b", textTransform: "uppercase", letterSpacing: "0.05em" }}>Page size</span>
            <select value={filters.size} onChange={(event) => setFilters((current) => ({ ...current, size: Number(event.target.value) }))} style={{ padding: "10px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", fontSize: "13px", color: "#1e293b", backgroundColor: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", outline: "none", cursor: "pointer" }}>
              {PAGE_SIZE_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>

          <div style={{ display: "flex", gap: "12px", flex: "1 1 100%", justifyContent: "flex-end", marginTop: "8px", paddingTop: "16px", borderTop: "1px dashed #cbd5e1" }}>
            <button
              type="button"
              className="button button--subtle"
              style={{ padding: "8px 16px", minHeight: "36px", fontSize: "13px" }}
              onClick={() => {
                setFilters(filterTemplate);
                setAppliedFilters(filterTemplate);
                setPage(0);
              }}
            >
              Reset
            </button>
            <button type="submit" className="button button--primary" style={{ padding: "8px 16px", minHeight: "36px", fontSize: "13px" }}>
              Apply filters
            </button>
          </div>
        </form>
      </DataToolbar>

      <FeedbackBanner error={error || actionMessage} kind={error ? "error" : "success"} />

      <SectionCard
        eyebrow="Inbox stream"
        title={loading ? "Loading notifications" : `${pageData?.totalElements ?? 0} notifications found`}
        description="Cards stay readable on desktop and mobile while preserving the backend inbox model."
        actions={
          <button type="button" className="button button--subtle" onClick={() => void loadNotifications(page, appliedFilters)}>
            <RefreshCcw size={16} />
            Refresh
          </button>
        }
        footer={<PaginationBar pageData={pageData} onPageChange={setPage} />}
      >
        {loading ? (
          <LoadingState title="Loading notification inbox" message="Syncing unread alerts and related entity signals." lines={5} />
        ) : notifications.length ? (
          <div className="notification-grid">
            {notifications.map((notification) => (
              <article key={notification.id} className="notification-card">
                <div className="notification-card__main">
                  <div className="notification-card__badges">
                    <StatusBadge value={notification.type} variant="general" />
                    <StatusBadge value={notification.read ? "READ" : "UNREAD"} variant={notification.read ? "active" : "pending"} />
                  </div>
                  <strong>{notification.title}</strong>
                  <p>{notification.message}</p>
                  <div className="notification-card__meta">
                    <span>{notification.recipientEmail}</span>
                    <span>{notification.relatedEntityType || "GENERAL"} #{notification.relatedEntityId || "-"}</span>
                    <span>{formatCompactDateTime(notification.createdAt)}</span>
                  </div>
                </div>

                <div className="table-actions">
                  {!notification.read ? (
                    <button type="button" className="icon-button" onClick={() => void handleMarkRead(notification.id)} aria-label="Mark notification as read">
                      <Eye size={16} />
                    </button>
                  ) : null}
                  <button type="button" className="icon-button icon-button--danger" onClick={() => setDeleteTarget(notification)} aria-label="Delete notification">
                    <Trash2 size={16} />
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Inbox is clear"
            message="No notifications matched the current filters, so the inbox is calm for now."
            action={!filters.unreadOnly ? (
              <button type="button" className="button button--ghost" onClick={() => {
                setFilters(filterTemplate);
                setAppliedFilters(filterTemplate);
                setPage(0);
              }}
              >
                Switch back to unread focus
              </button>
            ) : null}
          />
        )}
      </SectionCard>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete notification"
        message={deleteTarget ? `Remove "${deleteTarget.title}" from the inbox? This only affects the notification record.` : ""}
        confirmLabel="Delete notification"
        onConfirm={handleDeleteConfirmed}
        onClose={() => setDeleteTarget(null)}
      />
    </PageShell>
  );
}
